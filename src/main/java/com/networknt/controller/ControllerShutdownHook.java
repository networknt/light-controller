package com.networknt.controller;

import com.networknt.server.ShutdownHookProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ControllerShutdownHook implements ShutdownHookProvider {
    private static final Logger logger = LoggerFactory.getLogger(ControllerShutdownHook.class);
    @Override
    public void onShutdown() {
        logger.info("ControllerShutdownHook onStartup is called.");
        ControllerStartupHook.executor.shutdown();
    }
}
