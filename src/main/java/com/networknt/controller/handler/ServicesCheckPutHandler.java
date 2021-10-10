package com.networknt.controller.handler;

import com.networknt.body.BodyHandler;
import com.networknt.controller.ControllerConstants;
import com.networknt.controller.ControllerStartupHook;
import com.networknt.controller.model.Check;
import com.networknt.handler.LightHttpHandler;
import com.networknt.kafka.common.AvroSerializer;
import com.networknt.scheduler.*;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

/**
 * If TTL check is selected during the service register, the service will send TTL request periodically to indicate
 * if it is healthy or not. This endpoint is telling the controller a particular service instance status with check
 * id and a pass status.
 *
 * For demo mode, we are going to update the check object in the map from the startup hook. For cluster mode, we are
 * going to get the health store object with the key to update it with the latest check info and timestamp.
 *
 * In order to differentiate the normal health check scheduled tasks, we will use the action as UPDATE to reuse the
 * TaskDefinition object in the health check topic.
 *
 * @author Steve Hu
 */
public class ServicesCheckPutHandler implements LightHttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(ServicesCheckPutHandler.class);
    private static final String SUC10200 = "SUC10200";

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        Map<String, Object> body = (Map<String, Object>)exchange.getAttachment(BodyHandler.REQUEST_BODY);
        String id = (String)body.get("id");
        boolean pass = (Boolean)body.get("pass");
        Integer checkInterval = (Integer)body.get("checkInterval");
        if(logger.isTraceEnabled()) logger.trace("id = " + id + " pass = " + pass);

        if (ControllerStartupHook.config.isClusterMode()) {
            // as we cannot update the read only stores directly, we have to send a message to the controller-health-check
            // to update the health check status for a particular check id.
            TaskDefinitionKey taskDefinitionKey = TaskDefinitionKey.newBuilder()
                    .setName(id)
                    .setHost(ControllerConstants.HOST)
                    .build();
            // Task frequency definition triggers the task every 10 sec once
            TaskFrequency taskFrequency = TaskFrequency.newBuilder()
                    .setTimeUnit(TimeUnit.SECONDS)
                    .setTime(checkInterval == null ? ControllerConstants.CHECK_FREQUENCY : Integer.valueOf(checkInterval) / 1000)
                    .build();

            // create a data map with pass status for the check.
            Map<String, String> data = new HashMap<>();
            data.put("id", id);
            data.put("pass", String.valueOf(pass));

            TaskDefinition taskDefinition = TaskDefinition.newBuilder()
                    .setName(id)
                    .setHost(ControllerConstants.HOST)
                    .setAction(DefinitionAction.UPDATE)   // use UPDATE action to differentiate with normal scheduled task.
                    .setTopic(ControllerConstants.CHECK_TOPIC)
                    .setFrequency(taskFrequency)
                    .setStart(System.currentTimeMillis())
                    .setData(data)
                    .build();

            AvroSerializer serializer = new AvroSerializer();
            byte[] keyBytes = serializer.serialize(taskDefinitionKey);
            byte[] valueBytes = serializer.serialize(taskDefinition);
            // here we send to the health check topic directly. Not the scheduler topic.
            ProducerRecord<byte[], byte[]> tdRecord = new ProducerRecord<>(ControllerStartupHook.config.getHealthCheckTopic(), keyBytes, valueBytes);
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
            Check check = ControllerStartupHook.checks.get(id);
            if(check != null) {
                if (pass) {
                    // if the lastFailedTimestamp is not 0L, then reset it to 0L as it is passed. If it doesn't exist, it
                    // means the node is removed already due to pass the de-register after period.
                    if(check.getLastFailedTimestamp() != 0L) {
                        check.setLastFailedTimestamp(0L);
                    }
                    check.setLastExecuteTimestamp(System.currentTimeMillis());
                } else {
                    // update the lastFailedTimestamp in the check object in checks map. If it keeps failing, then don't
                    // update it. This will allow the job to remove the node if it fails for a long time greater than the
                    // de-register after setting in the check.
                    long timestamp = System.currentTimeMillis();
                    if(check.getLastFailedTimestamp() == 0L) {
                        check.setLastFailedTimestamp(timestamp);
                    }
                    check.setLastExecuteTimestamp(timestamp);
                }
            }
        }
        setExchangeStatus(exchange, SUC10200);
    }
}
