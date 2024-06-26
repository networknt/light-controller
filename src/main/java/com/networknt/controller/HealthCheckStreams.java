package com.networknt.controller;

import com.networknt.config.Config;
import com.networknt.config.JsonMapper;
import com.networknt.kafka.common.AvroDeserializer;
import com.networknt.kafka.common.AvroSerializer;
import com.networknt.kafka.common.EventId;
import com.networknt.kafka.common.KafkaStreamsConfig;
import com.networknt.kafka.streams.LightStreams;
import com.networknt.scheduler.DefinitionAction;
import com.networknt.scheduler.TaskDefinition;
import com.networknt.scheduler.TaskDefinitionKey;
import com.networknt.scheduler.TaskFrequency;
import com.networknt.utility.TimeUtil;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import net.lightapi.portal.controller.ControllerDeregisteredEvent;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.StreamsMetadata;
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler;
import org.apache.kafka.streams.processor.AbstractProcessor;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.processor.To;
import org.apache.kafka.streams.state.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class HealthCheckStreams implements LightStreams {
    static private final Logger logger = LoggerFactory.getLogger(HealthCheckStreams.class);
    static final KafkaStreamsConfig streamsConfig = (KafkaStreamsConfig) Config.getInstance().getJsonObjectConfig(KafkaStreamsConfig.CONFIG_NAME, KafkaStreamsConfig.class);
    static final ControllerConfig controllerConfig = (ControllerConfig) Config.getInstance().getJsonObjectConfig(ControllerConfig.CONFIG_NAME, ControllerConfig.class);
    static private final String health = "health-store";  // local health store between key and health check status
    final Serde keySpecificAvroSerde = new SpecificAvroSerde<>();
    KafkaStreams healthStreams;


    public HealthCheckStreams() {
        logger.info("HealthCheckStreams is created");
    }

    public ReadOnlyKeyValueStore<String, String> getHealthStore() {
        QueryableStoreType<ReadOnlyKeyValueStore<String, String>> queryableStoreType = QueryableStoreTypes.keyValueStore();
        StoreQueryParameters<ReadOnlyKeyValueStore<String, String>> sqp = StoreQueryParameters.fromNameAndType(health, queryableStoreType);
        return healthStreams.store(sqp);
    }

    public KeyQueryMetadata getHealthStreamsMetadata(TaskDefinitionKey taskDefinitionKey) {
        return healthStreams.queryMetadataForKey(health, taskDefinitionKey, keySpecificAvroSerde.serializer());
    }

    public Collection<StreamsMetadata> getAllHealthStreamsMetadata() {
        return healthStreams.streamsMetadataForStore(health);
    }

    private void startHealthStreams(String ip, int port) {

        StoreBuilder<KeyValueStore<String, String>> keyValueHealthStoreBuilder =
                Stores.keyValueStoreBuilder(Stores.persistentKeyValueStore(health),
                        Serdes.String(),
                        Serdes.String());


        final Topology topology = new Topology();
        topology.addSource("SourceTopicProcessor", controllerConfig.getHealthCheckTopic());
        topology.addProcessor("HealthCheckProcessor", HealthCheckStreams.HealthCheckProcessor::new, "SourceTopicProcessor");
        topology.addStateStore(keyValueHealthStoreBuilder, "HealthCheckProcessor");
        topology.addSink("ServiceEventProcessor", ControllerStartupHook.config.getTopic(), "HealthCheckProcessor");
        topology.addSink("SchedulerProcessor", ControllerStartupHook.config.getSchedulerTopic(), "HealthCheckProcessor");

        Properties streamsProps = new Properties();
        streamsProps.putAll(streamsConfig.getProperties());
        streamsProps.put(StreamsConfig.APPLICATION_ID_CONFIG, controllerConfig.getHealthApplicationId());
        streamsProps.put(StreamsConfig.APPLICATION_SERVER_CONFIG, ip + ":" + port);
        streamsProps.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.ByteArray().getClass());
        streamsProps.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.ByteArray().getClass());

        healthStreams = new KafkaStreams(topology, streamsProps);
        healthStreams.setUncaughtExceptionHandler(ex -> {
            logger.error("Kafka-Streams uncaught exception occurred. Stream will be replaced with new thread", ex);
            return StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.REPLACE_THREAD;
        });
        keySpecificAvroSerde.configure(streamsConfig.getProperties(), true);

        if (streamsConfig.isCleanUp()) {
            healthStreams.cleanUp();
        }
        healthStreams.start();
    }

    public static class HealthCheckProcessor extends AbstractProcessor<byte[], byte[]> {

        private ProcessorContext pc;
        private KeyValueStore<String, String> healthStore;

        public HealthCheckProcessor() {
        }

        @Override
        public void init(ProcessorContext pc) {

            this.pc = pc;
            this.healthStore = (KeyValueStore<String, String>) pc.getStateStore(health);

            if (logger.isInfoEnabled()) logger.info("Processor initialized");
        }

        @Override
        public void process(byte[] key, byte[] value) {

            if (logger.isDebugEnabled())
                logger.debug("HealthCheckStreams.process is called!");

            AvroDeserializer deserializer = new AvroDeserializer(true);
            Object object;

            // we need to ignore any message that cannot be deserialized. For example Unknown magic byte!
            try {
                object = deserializer.deserialize(value);
            } catch (Exception e) {
                logger.error("Exception:", e);
                return;
            }
            try {
                if (object instanceof TaskDefinition) {
                    TaskDefinition taskDefinition = (TaskDefinition) object;

                    // this is the task definition from the controller-health-check topic for the scheduled health check task.
                    if (logger.isTraceEnabled())
                        logger.trace("Task Definition = " + taskDefinition);

                    Map<String, String> data = taskDefinition.getData();
                    switch (taskDefinition.getAction()) {
                        case INSERT:

                            // need to make sure the message is not too old based on the time unit. Get the frequency * 2 in milliseconds.
                            long gracePeriod = TimeUtil.oneTimeUnitMillisecond(TimeUnit.valueOf(taskDefinition.getFrequency().getTimeUnit().name())) * taskDefinition.getFrequency().getTime() * 2;

                            if (logger.isTraceEnabled())
                                logger.trace("current = " + System.currentTimeMillis() + " task start = " + taskDefinition.getStart() + " gracePeriod = " + gracePeriod);

                            if (System.currentTimeMillis() - taskDefinition.getStart() < gracePeriod) {

                                // not too old and do the health check here.
                                // first get the health check object from the store.
                                String healthString = healthStore.get(data.get("id"));
                                Map<String, String> healthMap = null;

                                if (healthString != null) {

                                    // not the first time, let's convert this into a map and update it.
                                    Map<String, Object> objects = JsonMapper.string2Map(healthString);
                                    healthMap = objects.entrySet().stream()
                                            .collect(Collectors.toMap(Map.Entry::getKey, e -> (String) e.getValue()));
                                } else {

                                    // first time to run the health check for this id. Create a map and populate it with the data.
                                    healthMap = new HashMap<>(data);
                                }
                                String healthPath = data.get("healthPath");
                                if (healthPath != null) {

                                    // HTTP health check is used.
                                    boolean res = ControllerClient.checkHealth(data.get("protocol"), data.get("address"), Integer.valueOf(data.get("port")), healthPath, data.get("serviceId"));
                                    healthMap.put("lastExecuteTimestamp", String.valueOf(System.currentTimeMillis()));

                                    if (res) {

                                        // once the health check is successful, reset the last failedTimestamp to 0 so that it is not continue counting.
                                        // also set the executeInterval to normal interval from the task definition interval.
                                        healthMap.put("lastFailedTimestamp", "0");
                                        healthMap.put("executeInterval", data.get("interval"));
                                    } else {

                                        // only set the failure flag if it is not set yet.
                                        if ("0".equals(healthMap.get("lastFailedTimestamp")))
                                            healthMap.put("lastFailedTimestamp", String.valueOf(System.currentTimeMillis()));

                                        // double the health check executeInterval to avoid hitting the failed server too fast.
                                        healthMap.put("executeInterval", String.valueOf(Integer.valueOf(healthMap.get("executeInterval")) * 2));
                                    }
                                    if (!"0".equals(healthMap.get("lastFailedTimestamp"))) {

                                        // calculate if we need to remove the node.
                                        removeNode(healthMap);
                                    }
                                } else {

                                    // TTL health check is used.
                                    if (!"0".equals(healthMap.get("lastFailedTimestamp"))) {

                                        // calculate if we need to remove the node.
                                        removeNode(healthMap);
                                    } else {

                                        // last failed time is 0, set it to current
                                        healthMap.put("lastFailedTimestamp", String.valueOf(System.currentTimeMillis()));
                                    }
                                }

                                // save the health check object to the key value store for query.
                                healthStore.put(data.get("id"), JsonMapper.toJson(healthMap));
                            }
                            break;
                        case UPDATE:

                            // this can only be TTL health check put handler is called and the update message is pushed to the health check topic directly.
                            String healthString = healthStore.get(data.get("id"));
                            if (healthString != null) {

                                // not the first time, let's convert this into a map and update it.
                                Map<String, Object> objects = JsonMapper.string2Map(healthString);
                                Map<String, String> healthMap = objects.entrySet().stream()
                                        .collect(Collectors.toMap(Map.Entry::getKey, e -> (String) e.getValue()));
                                if ("true".equals(data.get("pass"))) {

                                    // if the lastFailedTimestamp is not 0, then reset it to 0 as it is passed. If it doesn't exist, it
                                    // means the node is removed already due to pass the de-register after period.
                                    if (!"0".equals(healthMap.get("lastFailedTimestamp"))) {
                                        healthMap.put("lastFailedTimestamp", "0");
                                    }
                                    healthMap.put("lastExecuteTimestamp", String.valueOf(System.currentTimeMillis()));
                                } else {

                                    // update the lastFailedTimestamp in the check object in health map. If it keeps failing, then don't
                                    // update it. This will allow the job to remove the node if it fails for a long time greater than the
                                    // de-register after setting in the check.
                                    String timestamp = String.valueOf(System.currentTimeMillis());
                                    if ("0".equals(healthMap.get("lastFailedTimestamp"))) {
                                        healthMap.put("lastFailedTimestamp", timestamp);
                                    }
                                    healthMap.put("lastExecuteTimestamp", timestamp);
                                }
                                healthStore.put(data.get("id"), JsonMapper.toJson(healthMap));
                            }
                            break;
                        case DELETE:

                            // ignore any delete message if there is any.
                            break;
                    }
                }
            } catch (Exception e) {
                logger.error("Exception:", e);
                return;
            }
        }

        @Override
        public void close() {
            if (logger.isInfoEnabled()) logger.info("Closing processor...");
        }

        private void removeNode(Map<String, String> healthMap) {
            if (System.currentTimeMillis() - Long.valueOf(healthMap.get("deregisterCriticalServiceAfter")) > Long.valueOf(healthMap.get("lastFailedTimestamp"))) {

                // remove the node as it passed the de-register after.
                String key = healthMap.get("tag") == null ? healthMap.get("serviceId") : healthMap.get("serviceId") + "|" + healthMap.get("tag");
                EventId eventId = EventId.newBuilder()
                        .setId(key)
                        .setNonce(ControllerConstants.NONCE)
                        .build();
                ControllerDeregisteredEvent event = ControllerDeregisteredEvent.newBuilder()
                        .setEventId(eventId)
                        .setHost(ControllerConstants.HOST)
                        .setKey(key)
                        .setServiceId(healthMap.get("serviceId"))
                        .setProtocol(healthMap.get("protocol"))
                        .setTag(healthMap.get("tag") == null ? null : healthMap.get("tag"))
                        .setAddress(healthMap.get("address"))
                        .setPort(Integer.valueOf(healthMap.get("port")))
                        .setTimestamp(System.currentTimeMillis())
                        .build();

                AvroSerializer serializer = new AvroSerializer();
                byte[] bytes = serializer.serialize(event);
                pc.forward(key.getBytes(StandardCharsets.UTF_8), bytes, To.child("ServiceEventProcessor"));

                // stop the light-scheduler to for health check task creation.
                String checkId = key + ":" + healthMap.get("protocol") + ":" + healthMap.get("address") + ":" + healthMap.get("port");
                TaskDefinitionKey taskDefinitionKey = TaskDefinitionKey.newBuilder()
                        .setName(checkId)
                        .setHost(ControllerConstants.HOST)
                        .build();

                // Task frequency definition triggers the task every 10 sec once
                TaskFrequency taskFrequency = TaskFrequency.newBuilder()
                        .setTimeUnit(com.networknt.scheduler.TimeUnit.SECONDS)
                        .setTime(healthMap.get("checkInterval") == null ? ControllerConstants.CHECK_FREQUENCY : Integer.valueOf(healthMap.get("checkInterval")) / 1000)
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
                pc.forward(keyBytes, valueBytes, To.child("SchedulerProcessor"));
            }
        }

    }

    @Override
    public void start(String ip, int port) {
        if (logger.isDebugEnabled()) logger.debug("HealthCheckStreams is starting...");
        startHealthStreams(ip, port);
    }

    @Override
    public void close() {
        if (logger.isDebugEnabled()) logger.debug("HealthCheckStreams is closing...");
        healthStreams.close();
    }

}
