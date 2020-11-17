package com.networknt.controller.handler;

import com.networknt.config.JsonMapper;
import com.networknt.controller.ControllerStartupHook;
import com.networknt.handler.LightHttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Get all the registered services from the controller for the view to display. This is a separate
 * endpoint as the view might use basic authentication instead of OAuth 2.0 jwt token for service
 * lookup from discovery. In normal cases, all endpoints should have the same authentication handler;
 * however, we need to allow basic authentication as an option for some users with small scale usage.
 *
 * @author Steve Hu
 */
public class ServicesGetHandler implements LightHttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(ServicesGetHandler.class);

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, "application/json");
        exchange.setStatusCode(200);
        exchange.getResponseSender().send(JsonMapper.toJson(ControllerStartupHook.services));
    }
}
