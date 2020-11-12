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
    // this is the service map. The key is serviceId or serviceId + | + tag and the value is a
    // list of map with address and port as keys. The list can only be updated with service post
    // and service delete handlers. Also, node can be removed after the check is failed after
    // configured period in the check during the server registry.
    public static final Map<String, Object> services = new ConcurrentHashMap<>();


    public static final Map<String, Object> checks = new ConcurrentHashMap<>();

    // this map contains all the server info entries per address and port combination as keys.
    // the first time a service is registered, it will call the /server/info endpoint to get
    // the info with a bootstrap token.
    public static final Map<String, Object> infos = new ConcurrentHashMap<>();

    @Override
    public void onStartup() {
        logger.info("ControllerStartupHook onStartup is called.");
    }
}
