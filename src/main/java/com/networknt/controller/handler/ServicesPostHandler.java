package com.networknt.controller.handler;

import com.networknt.controller.ControllerConstants;
import com.networknt.controller.ControllerStartupHook;
import com.networknt.controller.model.Check;
import com.networknt.body.BodyHandler;
import com.networknt.config.JsonMapper;
import com.networknt.handler.LightHttpHandler;
import com.networknt.kafka.common.AvroSerializer;
import com.networknt.kafka.common.EventId;
import com.networknt.scheduler.*;
import io.undertow.server.HttpServerExchange;
import net.lightapi.portal.controller.ControllerRegisteredEvent;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import static com.networknt.controller.ControllerConstants.*;

/**
 * Register a service to the controller to indicate it is alive. It is normally called
 * during the service startup phase.
 *
 * @author Steve Hu
 */
public class ServicesPostHandler implements LightHttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(ServicesPostHandler.class);
    private static final String SUC10200 = "SUC10200";
    AvroSerializer serializer = new AvroSerializer();

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        Map<String, Object> body = (Map<String, Object>)exchange.getAttachment(BodyHandler.REQUEST_BODY);
        String serviceId = (String)body.get(SERVICE_ID);
        String tag = (String)body.get(TAG);
        String key = tag == null ? serviceId : serviceId + "|" + tag;
        String protocol = (String)body.get(PROTOCOL);
        String address = (String)body.get(ADDRESS);
        int port = (Integer)body.get(PORT);

        Check check = JsonMapper.objectMapper.convertValue(body.get(ControllerConstants.CHECK), Check.class);
        if(logger.isDebugEnabled()) logger.debug("serviceId = " + serviceId + " tag = " + tag + " protocol = " + protocol + " address = " + address + " port = " + port + " check = " + body.get(ControllerConstants.CHECK));
        if(ControllerStartupHook.config.isClusterMode()) {
            EventId eventId = EventId.newBuilder()
                    .setId(key)
                    .setNonce(ControllerConstants.NONCE)
                    .build();
            ControllerRegisteredEvent event = ControllerRegisteredEvent.newBuilder()
                    .setEventId(eventId)
                    .setHost(ControllerConstants.HOST)
                    .setKey(key)
                    .setServiceId(serviceId)
                    .setProtocol(protocol)
                    .setTag(tag)
                    .setAddress(address)
                    .setPort(port)
                    .setCheck(JsonMapper.toJson(body.get(ControllerConstants.CHECK)))
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
            // schedule health check task with light-scheduler service. There are two ways to do that:
            // 1. call the light-scheduler REST API to create a task definition.
            // 2. produce a task definition to the light-scheduler topic directly.
            // We are using the option 2 here as we light-controller is a producer already.
            TaskDefinitionKey taskDefinitionKey = TaskDefinitionKey.newBuilder()
                    .setName(check.getId())
                    .setHost(ControllerConstants.HOST)
                    .build();
            // Task frequency definition triggers the task every 10 sec once
            TaskFrequency taskFrequency = TaskFrequency.newBuilder()
                    .setTimeUnit(TimeUnit.SECONDS)
                    .setTime(check.getInterval() == null ? ControllerConstants.CHECK_FREQUENCY : check.getInterval() / 1000) // use the interval and fall back to default 20 seconds.
                    .build();

            Map<String, String> dataMap = new HashMap<>();
            dataMap.put("id", check.getId());
            dataMap.put("deregisterCriticalServiceAfter", check.getDeregisterCriticalServiceAfter().toString());
            dataMap.put("healthPath", check.getHealthPath());
            dataMap.put("tlsSkipVerify", check.getTlsSkipVerify().toString());
            dataMap.put("interval", check.getInterval().toString());
            dataMap.put("executeInterval", String.valueOf(check.getExecuteInterval()));
            dataMap.put("lastExecuteTimestamp", String.valueOf(check.getLastExecuteTimestamp()));
            dataMap.put("lastFailedTimestamp", String.valueOf(check.getLastFailedTimestamp()));

            dataMap.put(SERVICE_ID, check.getServiceId());
            dataMap.put(TAG, check.getTag());
            dataMap.put(PROTOCOL, check.getProtocol());
            dataMap.put(ADDRESS, check.getAddress());
            dataMap.put(PORT, String.valueOf(check.getPort()));

            TaskDefinition taskDefinition = TaskDefinition.newBuilder()
                    .setName(check.getId())
                    .setHost(ControllerConstants.HOST)
                    .setAction(DefinitionAction.INSERT)
                    .setTopic(ControllerStartupHook.config.getHealthCheckTopic())
                    .setFrequency(taskFrequency)
                    .setStart(System.currentTimeMillis())
                    .setData(dataMap)
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


        } else {
            Map<String, Object> nodeMap = new ConcurrentHashMap<>();
            nodeMap.put(PROTOCOL, protocol);
            nodeMap.put(ADDRESS, address);
            nodeMap.put(PORT, port);

            List nodes = (List) ControllerStartupHook.services.get(key);
            nodes = addService(nodes, nodeMap);
            ControllerStartupHook.services.put(key, nodes);

            ControllerStartupHook.checks.put(check.getId(), check);

            // update all subscribed clients with the nodes
            WebSocketHandler.sendUpdatedNodes(key, nodes);
        }
        setExchangeStatus(exchange, SUC10200);
    }

    private List addService(List nodes, Map<String, Object> nodeMap) {
        if(nodes == null) {
            nodes = new ArrayList<>();
            nodes.add(nodeMap);
        } else {
            // delete the nodeMap if it is already there before adding it.
            String address = (String)nodeMap.get(ADDRESS);
            int port = (Integer)nodeMap.get(PORT);
            for(Iterator<Map<String, Object>> iter = nodes.iterator(); iter.hasNext(); ) {
                Map<String, Object> map = iter.next();
                String a = (String)map.get(ADDRESS);
                int p = (Integer)map.get(PORT);
                if (address.equals(a) && port == p)
                    iter.remove();
            }
            nodes.add(nodeMap);
        }
        return nodes;
    }
}
