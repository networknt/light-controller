package com.networknt.controller.handler;

import com.networknt.body.BodyHandler;
import com.networknt.config.Config;
import com.networknt.config.JsonMapper;
import com.networknt.controller.ControllerClient;
import com.networknt.controller.ControllerConfig;
import com.networknt.controller.ControllerConstants;
import com.networknt.controller.ControllerStartupHook;
import com.networknt.controller.model.Check;
import com.networknt.handler.LightHttpHandler;
import com.networknt.kafka.common.AvroSerializer;
import com.networknt.kafka.common.EventId;
import com.networknt.kafka.producer.QueuedLightProducer;
import com.networknt.service.SingletonServiceFactory;
import io.undertow.server.HttpServerExchange;
import net.lightapi.portal.controller.ControllerRegisteredEvent;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Register a service to the controller to indicate it is alive. It is normally called
 * during the service startup phase.
 *
 * @author Steve Hu
 */
public class ServicesPostHandler implements LightHttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(ServicesPostHandler.class);
    private static final String SUC10200 = "SUC10200";
    private static final String SEND_MESSAGE_EXCEPTION = "ERR11605";
    public static ControllerConfig config = (ControllerConfig) Config.getInstance().getJsonObjectConfig(ControllerConfig.CONFIG_NAME, ControllerConfig.class);

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        Map<String, Object> body = (Map<String, Object>)exchange.getAttachment(BodyHandler.REQUEST_BODY);
        String serviceId = (String)body.get("serviceId");
        String tag = (String)body.get("tag");
        String key = tag == null ? serviceId : serviceId + "|" + tag;
        String protocol = (String)body.get("protocol");
        String address = (String)body.get("address");
        int port = (Integer)body.get("port");
        if(logger.isDebugEnabled()) logger.debug("serviceId = " + serviceId + " tag = " + tag + " protocol = " + protocol + " address = " + address + " port = " + port + " check = " + body.get(ControllerConstants.CHECK));
        if(config.isClusterMode()) {
            EventId eventId = EventId.newBuilder()
                    .setId(ControllerConstants.USER_ID)
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

            AvroSerializer serializer = new AvroSerializer();
            byte[] bytes = serializer.serialize(event);

            ProducerRecord<byte[], byte[]> record = new ProducerRecord<>(config.getTopic(), ControllerConstants.USER_ID.getBytes(StandardCharsets.UTF_8), bytes);
            QueuedLightProducer producer = SingletonServiceFactory.getBean(QueuedLightProducer.class);
            BlockingQueue<ProducerRecord<byte[], byte[]>> txQueue = producer.getTxQueue();
            try {
                if(logger.isDebugEnabled()) logger.debug("Pushing one record to kafka through the txQueue");
                txQueue.put(record);
            } catch (InterruptedException e) {
                logger.error("Exception:", e);
                setExchangeStatus(exchange, SEND_MESSAGE_EXCEPTION, e.getMessage(), ControllerConstants.USER_ID);
                return;
            }
        } else {
            Map<String, Object> nodeMap = new ConcurrentHashMap<>();
            nodeMap.put("protocol", protocol);
            nodeMap.put("address", address);
            nodeMap.put("port", port);

            List nodes = (List) ControllerStartupHook.services.get(key);
            nodes = addService(nodes, nodeMap);
            ControllerStartupHook.services.put(key, nodes);

            // save the check Object in another map for background process to perform check periodically.
            Check check = JsonMapper.objectMapper.convertValue(body.get("check"), Check.class);
            ControllerStartupHook.checks.put(check.getId(), check);
            // now try to get server info from by accessing the endpoint with a URL constructed with address and port
            // we assume that the server is running with https and it can verify the bootstrap token from the controller.
            String info = ControllerClient.getServerInfo(protocol, address, port);
            if (info != null) {
                ControllerStartupHook.infos.put(address + ":" + port, info);
            }

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
            String address = (String)nodeMap.get("address");
            int port = (Integer)nodeMap.get("port");
            for(Iterator<Map<String, Object>> iter = nodes.iterator(); iter.hasNext(); ) {
                Map<String, Object> map = iter.next();
                String a = (String)map.get("address");
                int p = (Integer)map.get("port");
                if (address.equals(a) && port == p)
                    iter.remove();
            }
            nodes.add(nodeMap);
        }
        return nodes;
    }
}
