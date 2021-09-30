package com.networknt.controller;

import com.networknt.config.Config;
import com.networknt.kafka.common.AvroDeserializer;
import com.networknt.kafka.common.KafkaStreamsConfig;
import com.networknt.kafka.streams.LightStreams;
import com.networknt.scheduler.TaskDefinition;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.processor.AbstractProcessor;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class HealthCheckStreams implements LightStreams {
    static private final Logger logger = LoggerFactory.getLogger(HealthCheckStreams.class);
    static final KafkaStreamsConfig streamsConfig = (KafkaStreamsConfig) Config.getInstance().getJsonObjectConfig(KafkaStreamsConfig.CONFIG_NAME, KafkaStreamsConfig.class);
    static final ControllerConfig controllerConfig = (ControllerConfig) Config.getInstance().getJsonObjectConfig(ControllerConfig.CONFIG_NAME, ControllerConfig.class);
    static private final String health = "health-store";  // local health store between key and health check status

    KafkaStreams healthStreams;


    public HealthCheckStreams() {
        logger.info("HealthCheckStreams is created");
    }

    public ReadOnlyKeyValueStore<String, String> getHealthStore() {
        QueryableStoreType<ReadOnlyKeyValueStore<String, String>> queryableStoreType = QueryableStoreTypes.keyValueStore();
        StoreQueryParameters<ReadOnlyKeyValueStore<String, String>> sqp = StoreQueryParameters.fromNameAndType(health, queryableStoreType);
        return healthStreams.store(sqp);
    }

    public KeyQueryMetadata getHealthStreamsMetadata(String key) {
        return healthStreams.queryMetadataForKey(health, key, Serdes.String().serializer());
    }

    public Collection<StreamsMetadata> getAllHealthStreamsMetadata() {
        return healthStreams.allMetadataForStore(health);
    }

    private void startHealthStreams(String ip, int port) {

        StoreBuilder<KeyValueStore<String, String>> keyValueHealthStoreBuilder =
                Stores.keyValueStoreBuilder(Stores.persistentKeyValueStore(health),
                        Serdes.String(),
                        Serdes.String());


        final Topology topology = new Topology();
        topology.addSource("SourceTopicProcessor", controllerConfig.getSchedulerTopic());
        topology.addProcessor("HealthCheckProcessor", HealthCheckStreams.HealthCheckProcessor::new, "SourceTopicProcessor");
        topology.addStateStore(keyValueHealthStoreBuilder, "HealthCheckProcessor");
        Properties streamsProps = new Properties();
        streamsProps.putAll(streamsConfig.getProperties());
        streamsProps.put(StreamsConfig.APPLICATION_ID_CONFIG, controllerConfig.getHealthApplicationId());
        streamsProps.put(StreamsConfig.APPLICATION_SERVER_CONFIG, ip + ":" + port);
        healthStreams = new KafkaStreams(topology, streamsProps);
        if(streamsConfig.isCleanUp()) {
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

            if(logger.isInfoEnabled()) logger.info("Processor initialized");
        }

        @Override
        public void process(byte[] key, byte[] value) {
            if(logger.isDebugEnabled()) logger.debug("HealthCheckStreams.process is called!");
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
                if(object instanceof TaskDefinition) {
                    TaskDefinition taskDefinition = (TaskDefinition) object;
                    // this is the task definition from the controller-health-check topic for the scheduled health check task.
                    if (logger.isTraceEnabled()) logger.trace("Task Definition = " + taskDefinition);

                    // query the key/value store to get the current health check object.
                    // need to execute the health check based on the task definition
                    // create or update the health check object based on the health check result.
                    // save the health check object to the key value store for query.
                    // healthStore.put(controllerRegisteredEvent.getKey(), nodesString);

                }
            } catch (Exception e) {
                logger.error("Exception:", e);
                return;
            }
        }

        @Override
        public void close() {
            if(logger.isInfoEnabled()) logger.info("Closing processor...");
        }
    }
    @Override
    public void start(String ip, int port) {
        if(logger.isDebugEnabled()) logger.debug("HealthCheckStreams is starting...");
        startHealthStreams(ip, port);
    }

    @Override
    public void close() {
        if(logger.isDebugEnabled()) logger.debug("HealthCheckStreams is closing...");
        healthStreams.close();
    }

}
