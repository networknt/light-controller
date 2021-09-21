package com.networknt.controller;

import com.networknt.controller.model.Check;
import com.networknt.server.StartupHookProvider;
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

    // this map contains all the server info entries per address and port combination as keys.
    // the first time a service is registered, it will call the /server/info endpoint to get
    // the info with a bootstrap token.
    // public static final Map<String, Object> infos = new ConcurrentHashMap<>();

    public static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    @Override
    public void onStartup() {
        logger.info("ControllerStartupHook onStartup is called.");
        long delay  = 1000L;
        long period = 1000L;
        CheckTask checkTask = new CheckTask();
        executor.scheduleAtFixedRate(checkTask, delay, period, TimeUnit.MILLISECONDS);
    }
}
