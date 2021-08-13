package com.networknt.controller;

import com.networknt.config.Config;
import com.networknt.kafka.producer.QueuedLightProducer;
import com.networknt.server.ShutdownHookProvider;
import com.networknt.service.SingletonServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ControllerShutdownHook implements ShutdownHookProvider {
    private static final Logger logger = LoggerFactory.getLogger(ControllerShutdownHook.class);
    // controller configuration.
    public static ControllerConfig config = (ControllerConfig) Config.getInstance().getJsonObjectConfig(ControllerConfig.CONFIG_NAME, ControllerConfig.class);

    @Override
    public void onShutdown() {
        logger.info("ControllerShutdownHook onStartup is called.");
        ControllerStartupHook.executor.shutdown();
        if(config.isClusterMode()) {
        // close the Kafka transactional producer before the server is shutdown
            QueuedLightProducer producer = SingletonServiceFactory.getBean(QueuedLightProducer.class);
            try { if(producer != null) producer.close(); } catch(Exception e) {e.printStackTrace();}
        }
    }
}
