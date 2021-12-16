package com.networknt.controller.handler;

import com.networknt.controller.ControllerConstants;
import com.networknt.controller.ControllerStartupHook;
import com.networknt.controller.ControllerUtil;
import com.networknt.controller.model.Check;
import com.networknt.handler.LightHttpHandler;
import com.networknt.kafka.common.AvroSerializer;
import com.networknt.kafka.common.EventId;
import com.networknt.scheduler.*;
import io.undertow.server.HttpServerExchange;
import net.lightapi.portal.controller.ControllerDeregisteredEvent;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CountDownLatch;

/**
 * Remove a service from the registry with this put request. If the service doesn't exist, then no action
 * is taken. It is normally called during the service shutdown phase.
 *
 * @author Steve Hu
 */
public class ServicesDeleteHandler implements LightHttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(ServicesDeleteHandler.class);
    private static final String SUC10200 = "SUC10200";
    private AvroSerializer serializer = new AvroSerializer();

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        String serviceId = exchange.getQueryParameters().get("serviceId").getFirst();
        String protocol = exchange.getQueryParameters().get("protocol").getFirst();
        String checkInterval = exchange.getQueryParameters().get("checkInterval").getFirst();
        String tag = null;
        Deque<String> tagDeque = exchange.getQueryParameters().get("tag");
        if(tagDeque != null && !tagDeque.isEmpty()) tag = tagDeque.getFirst();
        String key = tag == null ?  serviceId : serviceId + "|" + tag;

        String address = exchange.getQueryParameters().get("address").getFirst();
        int port = Integer.valueOf(exchange.getQueryParameters().get("port").getFirst());
        if(logger.isDebugEnabled()) logger.debug("serviceId = " + serviceId + " protocol = " + protocol + " tag = " + tag + " address = " + address + " port = " + port);
        if(ControllerStartupHook.config.isClusterMode()) {
            // push the de-registered event to portal-event.
            pushDeregisterEvent(serializer, key, serviceId, protocol, tag, address, port);
            // send task definition with delete action to stop the task scheduling. Send to the light-scheduler topic directly.
            pushDeleteTaskDefinition(serializer, key, protocol, address, port, checkInterval);
        } else {
            List nodes = (List) ControllerStartupHook.services.get(key);
            nodes = ControllerUtil.delService(nodes, address, port);
            if (nodes != null && nodes.size() > 0) {
                ControllerStartupHook.services.put(key, nodes);
            } else {
                ControllerStartupHook.services.remove(key);
            }
            // delete from the checks, cancel the timer task before deleting.
            String checkId = key + ":" + protocol + ":" + address + ":" + port;
            Check check = ControllerStartupHook.checks.remove(checkId);

            // update all subscribed clients with the nodes
            WebSocketHandler.sendUpdatedNodes(key, nodes);
        }
        setExchangeStatus(exchange, SUC10200);
    }

    public static void pushDeregisterEvent(AvroSerializer serializer, String key, String serviceId, String protocol, String tag, String address, int port) throws Exception {
        EventId eventId = EventId.newBuilder()
                .setId(key)
                .setNonce(ControllerConstants.NONCE)
                .build();
        ControllerDeregisteredEvent event = ControllerDeregisteredEvent.newBuilder()
                .setEventId(eventId)
                .setHost(ControllerConstants.HOST)
                .setKey(key)
                .setServiceId(serviceId)
                .setProtocol(protocol)
                .setTag(tag)
                .setAddress(address)
                .setPort(port)
                .setTimestamp(System.currentTimeMillis())
                .build();

        byte[] bytes = serializer.serialize(event);

        ProducerRecord<byte[], byte[]> record = new ProducerRecord<>(ControllerStartupHook.config.getTopic(), key.getBytes(StandardCharsets.UTF_8), bytes);
        final CountDownLatch latch = new CountDownLatch(1);
        ControllerStartupHook.producer.send(record, (recordMetadata, e) -> {
            if (Objects.nonNull(e)) {
                logger.error("Exception occurred while pushing the event", e);
            } else {
                logger.info("Event record pushed successfully. Received Record Metadata is {}",
                        recordMetadata);
            }
            latch.countDown();
        });
        latch.await();
    }

    public static void pushDeleteTaskDefinition(AvroSerializer serializer, String key, String protocol, String address, int port, String checkInterval) throws Exception {
        String checkId = key + ":" + protocol + ":" + address + ":" + port;
        TaskDefinitionKey taskDefinitionKey = TaskDefinitionKey.newBuilder()
                .setName(checkId)
                .setHost(ControllerConstants.HOST)
                .build();
        // Task frequency definition triggers the task every 10 sec once
        TaskFrequency taskFrequency = TaskFrequency.newBuilder()
                .setTimeUnit(TimeUnit.SECONDS)
                .setTime(checkInterval == null ? ControllerConstants.CHECK_FREQUENCY : Integer.valueOf(checkInterval) / 1000)
                .build();

        TaskDefinition taskDefinition = TaskDefinition.newBuilder()
                .setName(checkId)
                .setHost(ControllerConstants.HOST)
                .setAction(DefinitionAction.DELETE)
                .setTopic(ControllerStartupHook.config.getHealthCheckTopic())
                .setFrequency(taskFrequency)
                .setStart(System.currentTimeMillis())
                .build();

        byte[] keyBytes = serializer.serialize(taskDefinitionKey);
        byte[] valueBytes = serializer.serialize(taskDefinition);
        ProducerRecord<byte[], byte[]> tdRecord = new ProducerRecord<>(ControllerStartupHook.config.getSchedulerTopic(), keyBytes, valueBytes);
        final CountDownLatch schedulerLatch = new CountDownLatch(1);
        ControllerStartupHook.producer.send(tdRecord, (recordMetadata, e) -> {
            if (Objects.nonNull(e)) {
                logger.error("Exception occurred while pushing the task definition", e);
            } else {
                logger.info("Task definition record pushed successfully. Received Record Metadata is {}",
                        recordMetadata);
            }
            schedulerLatch.countDown();
        });
        schedulerLatch.await();
    }
}
