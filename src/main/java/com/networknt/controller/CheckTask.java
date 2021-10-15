package com.networknt.controller;

import com.networknt.controller.model.Check;
import com.networknt.kafka.common.AvroSerializer;
import com.networknt.kafka.common.EventId;
import com.networknt.kafka.producer.QueuedLightProducer;
import com.networknt.service.SingletonServiceFactory;
import net.lightapi.portal.controller.ControllerDeregisteredEvent;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;

public class CheckTask extends TimerTask {
    private static final Logger logger = LoggerFactory.getLogger(CheckTask.class);

    @Override
    public void run() {
        // iterate the checks map and run the check based on the parameters in check object
        try {
            if(ControllerStartupHook.checks != null) {
                List<String> keys = ControllerStartupHook.checks.keySet().stream().collect(Collectors.toList());
                for(String key: keys) {
                    Check check = ControllerStartupHook.checks.get(key);
                    if (check!=null) {
                        if (check.getExecuteInterval()==0) check.setExecuteInterval(check.getInterval());
                        if(logger.isTraceEnabled()) logger.trace("id = " + check.getId() + " lastFailed = " + check.getLastFailedTimestamp() + " lastExecute = " + check.getLastExecuteTimestamp() + " interval = " + check.getInterval() + " deregAfter = " + check.getDeregisterCriticalServiceAfter());
                        if(check.getLastExecuteTimestamp() == 0) {
                            if(logger.isTraceEnabled()) logger.trace("check " + check.getId() + " first time");
                            execute(check);
                        } else if(System.currentTimeMillis() - check.getExecuteInterval() > check.getLastExecuteTimestamp()){
                            if(logger.isTraceEnabled()) logger.trace("check " + check.getId() + " interval reached");
                            execute(check);
                        } else {
                            if(logger.isTraceEnabled()) logger.trace("check " + check.getId() + " not reach interval yet");
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void execute(Check check) {
        if(check.getHealthPath() != null) {
            boolean res = ControllerClient.checkHealth(check.getProtocol(), check.getAddress(), check.getPort(), check.getHealthPath(), check.getServiceId());
            check.setLastExecuteTimestamp(System.currentTimeMillis());
            if(res) {
                // once the health check is successful, reset the last failedTimestamp to 0 so that it is not continue counting.
                // also set the executeInterval to normal interval from the task definition interval.
                check.setLastFailedTimestamp(0L);
                check.setExecuteInterval(check.getInterval());
            } else {
                // only set the failure flag if it is not set yet.
                if(check.getLastFailedTimestamp() == 0L) check.setLastFailedTimestamp(System.currentTimeMillis());
                // double the health check executeInterval to avoid hitting the failed server too fast.
                check.setExecuteInterval(check.getExecuteInterval()*2);
            }
            if(check.getLastFailedTimestamp() != 0L) {
                removeNode(check);
            }
        } else {
            // ttl check so we need to check the last failed time to decide if the node should be removed.
            if(check.getLastFailedTimestamp() != 0L) {
                removeNode(check);
            } else {
                // last failed time is 0, set it to current
                check.setLastFailedTimestamp(System.currentTimeMillis());
            }
        }
    }

    private void removeNode(Check check) {
        if(System.currentTimeMillis() - check.getDeregisterCriticalServiceAfter() > check.getLastFailedTimestamp()) {
            // remove the node as it passed the de-register after.
            String key = check.getTag() == null ?  check.getServiceId() : check.getServiceId() + "|" + check.getTag();
            if(ControllerStartupHook.config.isClusterMode()) {
                EventId eventId = EventId.newBuilder()
                        .setId(ControllerConstants.USER_ID)
                        .setNonce(ControllerConstants.NONCE)
                        .build();
                ControllerDeregisteredEvent event = ControllerDeregisteredEvent.newBuilder()
                        .setEventId(eventId)
                        .setHost(ControllerConstants.HOST)
                        .setKey(key)
                        .setServiceId(check.getServiceId())
                        .setProtocol(check.getProtocol())
                        .setTag(check.getTag())
                        .setAddress(check.getAddress())
                        .setPort(check.getPort())
                        .setTimestamp(System.currentTimeMillis())
                        .build();

                AvroSerializer serializer = new AvroSerializer();
                byte[] bytes = serializer.serialize(event);

                ProducerRecord<byte[], byte[]> record = new ProducerRecord<>(ControllerStartupHook.config.getTopic(), ControllerConstants.USER_ID.getBytes(StandardCharsets.UTF_8), bytes);
                QueuedLightProducer producer = SingletonServiceFactory.getBean(QueuedLightProducer.class);
                BlockingQueue<ProducerRecord<byte[], byte[]>> txQueue = producer.getTxQueue();
                try {
                    txQueue.put(record);
                } catch (InterruptedException e) {
                    logger.error("Exception:", e);
                }
            } else {
                if(logger.isDebugEnabled()) logger.debug("remove service key: " + key);
                List nodes = (List) ControllerStartupHook.services.get(key);
                nodes = ControllerUtil.delService(nodes, check.getAddress(), check.getPort());
                if (nodes != null && nodes.size() > 0) {
                    ControllerStartupHook.services.put(key, nodes);
                } else {
                    ControllerStartupHook.services.remove(key);
                }
                Check unusedCheck = ControllerStartupHook.checks.remove(check.getId());
                if(logger.isDebugEnabled()) logger.debug("remove check for id: " + unusedCheck.getId());
            }
        }
    }
}
