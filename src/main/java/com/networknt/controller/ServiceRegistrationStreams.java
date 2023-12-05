package com.networknt.controller;

import com.networknt.config.Config;
import com.networknt.config.JsonMapper;
import com.networknt.kafka.common.AvroDeserializer;
import com.networknt.kafka.common.KafkaStreamsConfig;
import com.networknt.kafka.streams.LightStreams;
import net.lightapi.portal.controller.ControllerDeregisteredEvent;
import net.lightapi.portal.controller.ControllerRegisteredEvent;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.StreamsMetadata;
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler;
import org.apache.kafka.streams.processor.AbstractProcessor;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ServiceRegistrationStreams implements LightStreams {
    static private final Logger logger = LoggerFactory.getLogger(ServiceRegistrationStreams.class);
    static final KafkaStreamsConfig streamsConfig = (KafkaStreamsConfig) Config.getInstance().getJsonObjectConfig(KafkaStreamsConfig.CONFIG_NAME, KafkaStreamsConfig.class);
    static final ControllerConfig controllerConfig = (ControllerConfig) Config.getInstance().getJsonObjectConfig(ControllerConfig.CONFIG_NAME, ControllerConfig.class);
    static private final String service = "service-store";  // local service store between key and service entity

    KafkaStreams serviceStreams;


    public ServiceRegistrationStreams() {
        logger.info("ServiceRegistrationStreams is created");
    }

    public ReadOnlyKeyValueStore<String, String> getServiceStore() {
        QueryableStoreType<ReadOnlyKeyValueStore<String, String>> queryableStoreType = QueryableStoreTypes.keyValueStore();
        StoreQueryParameters<ReadOnlyKeyValueStore<String, String>> sqp = StoreQueryParameters.fromNameAndType(service, queryableStoreType);
        return serviceStreams.store(sqp);
    }

    public KeyQueryMetadata getServiceStreamsMetadata(String key) {
        return serviceStreams.queryMetadataForKey(service, key, Serdes.String().serializer());
    }

    public Collection<StreamsMetadata> getAllServiceStreamsMetadata() {
        return serviceStreams.streamsMetadataForStore(service);
    }

    private void startServiceStreams(String ip, int port) {

        StoreBuilder<KeyValueStore<String, String>> keyValueServiceStoreBuilder =
                Stores.keyValueStoreBuilder(Stores.persistentKeyValueStore(service),
                        Serdes.String(),
                        Serdes.String());


        final Topology topology = new Topology();
        topology.addSource("SourceTopicProcessor", controllerConfig.getTopic());
        topology.addProcessor("ServiceEventProcessor", ServiceEventProcessor::new, "SourceTopicProcessor");
        topology.addStateStore(keyValueServiceStoreBuilder, "ServiceEventProcessor");

        // topology.addSink("NonceProcessor", "portal-nonce", "ServiceEventProcessor");
        // topology.addSink("NotificationProcessor", "portal-notification", "ServiceEventProcessor");
        Properties streamsProps = new Properties();
        streamsProps.putAll(streamsConfig.getProperties());
        streamsProps.put(StreamsConfig.APPLICATION_ID_CONFIG, controllerConfig.getRegistryApplicationId());
        streamsProps.put(StreamsConfig.APPLICATION_SERVER_CONFIG, ip + ":" + port);
        streamsProps.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        streamsProps.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        serviceStreams = new KafkaStreams(topology, streamsProps);
        serviceStreams.setUncaughtExceptionHandler(ex -> {
            logger.error("Kafka-Streams uncaught exception occurred. Stream will be replaced with new thread", ex);
            return StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.REPLACE_THREAD;
        });

        if (streamsConfig.isCleanUp()) {
            serviceStreams.cleanUp();
        }
        serviceStreams.start();
    }

    public static class ServiceEventProcessor extends AbstractProcessor<byte[], byte[]> {

        private ProcessorContext pc;
        private KeyValueStore<String, String> serviceStore;

        public ServiceEventProcessor() {
        }

        @Override
        public void init(ProcessorContext pc) {

            this.pc = pc;
            this.serviceStore = (KeyValueStore<String, String>) pc.getStateStore(service);

            if (logger.isInfoEnabled())
                logger.info("Processor initialized");

        }

        @Override
        public void process(byte[] key, byte[] value) {
            if (logger.isDebugEnabled()) logger.debug("ServiceRegistrationStreams.process is called!");
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
                if (object instanceof ControllerRegisteredEvent) {
                    ControllerRegisteredEvent controllerRegisteredEvent = (ControllerRegisteredEvent) object;

                    if (logger.isTraceEnabled())
                        logger.trace("Event = " + controllerRegisteredEvent);

                    Map<String, Object> nodeMap = new ConcurrentHashMap<>();
                    nodeMap.put("protocol", controllerRegisteredEvent.getProtocol());
                    nodeMap.put("address", controllerRegisteredEvent.getAddress());
                    nodeMap.put("port", controllerRegisteredEvent.getPort());

                    String nodesString = serviceStore.get(controllerRegisteredEvent.getKey());
                    List<Map<String, Object>> nodes;
                    if (nodesString == null) {
                        nodes = new ArrayList<>();
                    } else {
                        nodes = JsonMapper.string2List(nodesString);
                    }
                    // before we add a new entry to the list, we need to make sure that the same entry doesn't exist.
                    nodes.removeIf(e -> (e.get("protocol").equals(controllerRegisteredEvent.getProtocol()) && e.get("address").equals(controllerRegisteredEvent.getAddress()) && controllerRegisteredEvent.getPort() == (Integer) e.get("port")));
                    nodes.add(nodeMap);
                    nodesString = JsonMapper.toJson(nodes);
                    serviceStore.put(controllerRegisteredEvent.getKey(), nodesString);
                } else if (object instanceof ControllerDeregisteredEvent) {
                    ControllerDeregisteredEvent controllerDeregisteredEvent = (ControllerDeregisteredEvent) object;
                    if (logger.isTraceEnabled()) logger.trace("Event = " + controllerDeregisteredEvent);

                    String nodesString = serviceStore.get(controllerDeregisteredEvent.getKey());
                    if (nodesString != null) {
                        List<Map<String, Object>> nodes = JsonMapper.string2List(nodesString);
                        String protocol = controllerDeregisteredEvent.getProtocol();
                        String address = controllerDeregisteredEvent.getAddress();
                        int port = controllerDeregisteredEvent.getPort();
                        nodes.removeIf(e -> (e.get("protocol").equals(protocol) && e.get("address").equals(address) && port == (Integer) e.get("port")));
                        if (nodes.size() == 0) {
                            // set it null in order to remove the service from the store.
                            nodesString = null;
                        } else {
                            nodesString = JsonMapper.toJson(nodes);
                        }
                    }
                    if (nodesString == null) {
                        // when the last node is removed for the service, remove it from the store.
                        serviceStore.delete(controllerDeregisteredEvent.getKey());
                    } else {
                        serviceStore.put(controllerDeregisteredEvent.getKey(), nodesString);
                    }
                }
            } catch (Exception e) {
                logger.error("Exception:", e);
                return;
            }
        }

        @Override
        public void close() {

            if (logger.isInfoEnabled())
                logger.info("Closing processor...");
        }
    }

    @Override
    public void start(String ip, int port) {

        if (logger.isDebugEnabled())
            logger.debug("ServiceStreams is starting...");

        startServiceStreams(ip, port);
    }

    @Override
    public void close() {

        if (logger.isDebugEnabled())
            logger.debug("ServiceStreams is closing...");

        serviceStreams.close();
    }

}
