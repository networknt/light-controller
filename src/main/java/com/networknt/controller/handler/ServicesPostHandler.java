package com.networknt.controller.handler;

import com.networknt.body.BodyHandler;
import com.networknt.config.Config;
import com.networknt.config.JsonMapper;
import com.networknt.controller.ControllerConfig;
import com.networknt.controller.ControllerStartupHook;
import com.networknt.handler.LightHttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Register a service to the controller to indicate it is alive. It is normally called
 * during the service startup phase.
 *
 * @author Steve Hu
 */
public class ServicesPostHandler implements LightHttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(ServicesPostHandler.class);
    private static final String SUC10200 = "SUC10200";

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        Map<String, Object> body = (Map<String, Object>)exchange.getAttachment(BodyHandler.REQUEST_BODY);
        String id = (String)body.get("id");
        String tag = (String)body.get("tag");
        String address = (String)body.get("address");
        int port = (Integer)body.get("port");
        if(logger.isDebugEnabled()) logger.debug("id = " + id + " tag = " + tag + " address = " + address + " port = " + port);
        Map<String, Object> nodeMap = new ConcurrentHashMap<>();
        nodeMap.put("address", address);
        nodeMap.put("port", port);

        if(tag != null) {
            List nodes = (List)ControllerStartupHook.services.get(id + "|" + tag);
            ControllerStartupHook.services.put(id + "|" + tag, addService(nodes, nodeMap));
        } else {
            List nodes = (List)ControllerStartupHook.services.get(id);
            ControllerStartupHook.services.put(id, addService(nodes, nodeMap));
        }

        setExchangeStatus(exchange, SUC10200);
    }

    private List addService(List nodes, Map<String, Object> nodeMap) {
        if(nodes == null) {
            nodes = new ArrayList<>();
            nodes.add(nodeMap);
        } else {
            nodes.add(nodeMap);
        }
        return nodes;
    }
}
