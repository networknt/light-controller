package com.networknt.controller;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.networknt.server.StartupHookProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class ControllerStartupHook implements StartupHookProvider {
    private static final Logger logger = LoggerFactory.getLogger(ControllerStartupHook.class);
    public static final Cache<String, Object> cache = Caffeine.newBuilder().expireAfterWrite(1, TimeUnit.HOURS).build();
    public static final Map<String, Object> services = new ConcurrentHashMap<>();
    public static final Map<String, Object> checks = new ConcurrentHashMap<>();
    @Override
    public void onStartup() {
        logger.info("ControllerStartupHook onStartup is called.");
    }
}
