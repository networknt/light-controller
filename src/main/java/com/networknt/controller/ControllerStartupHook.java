package com.networknt.controller;

import com.networknt.config.Config;
import com.networknt.controller.model.Check;
import com.networknt.kafka.producer.NativeLightProducer;
import com.networknt.server.Server;
import com.networknt.server.StartupHookProvider;
import com.networknt.service.SingletonServiceFactory;
import com.networknt.utility.NetUtils;
import org.apache.kafka.clients.producer.Producer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ControllerStartupHook implements StartupHookProvider {

    private static final Logger logger = LoggerFactory.getLogger(ControllerStartupHook.class);
    // this is the service map. The key is serviceId or serviceId + | + tag and the value is a
    // list of map with address and port as keys. The list can only be updated with service post
    // and service delete handlers. Also, node can be removed after the check is failed after
    // configured period in the check during the server registry.
    public static final Map<String, Object> services = new ConcurrentHashMap<>();

    // this is a check map with a check id to the Check object. For every valid check, a background
    // coroutine will check based on the interval (http check) and ttl (ttl check). After a period
    // of deregisterAfter if a node is in critical state, the node will be removed from the services
    public static final Map<String, Check> checks = new ConcurrentHashMap<>();

    // scheduled executor service for multiple threading.
    public static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    // controller configuration.
    public static ControllerConfig config = (ControllerConfig) Config.getInstance().getJsonObjectConfig(ControllerConfig.CONFIG_NAME, ControllerConfig.class);

    public static ServiceRegistrationStreams streams = null;
    public static Producer producer = null;

    @Override
    public void onStartup() {
        logger.info("ControllerStartupHook onStartup begins.");
        long delay  = 1000L;
        long period = 1000L;
        CheckTask checkTask = new CheckTask();
        executor.scheduleAtFixedRate(checkTask, delay, period, TimeUnit.MILLISECONDS);
        if(config.isClusterMode()) {
            // create Kafka transactional producer for publishing events to portal-event topic
            NativeLightProducer lightProducer = SingletonServiceFactory.getBean(NativeLightProducer.class);
            lightProducer.open();
            producer = lightProducer.getProducer();

            // start the service registration streams
            int port = Server.getServerConfig().getHttpsPort();
            String ip = NetUtils.getLocalAddressByDatagram();
            logger.info("ip = " + ip + " port = " + port);
            streams = new ServiceRegistrationStreams();
            // start the kafka stream process
            streams.start(ip, port);
        }
        logger.info("ControllerStartupHook onStartup ends.");
    }
}
