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
 * Get all the registered services from the controller. A query parameter passing can filter out
 * only the healthy services. If serviceId and tag is passed in, then only a particular service
 * is returned.
 *
 * @author Steve Hu
 */
public class ServicesGetHandler implements LightHttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(ServicesGetHandler.class);

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        boolean passing = false;
        Deque<String> passingDeque = exchange.getQueryParameters().get("passing");
        if(passingDeque != null && !passingDeque.isEmpty()) {
            passing = true;
        }
        String serviceId = null;
        Deque<String> serviceIdDeque = exchange.getQueryParameters().get("serviceId");
        if(serviceIdDeque != null && !serviceIdDeque.isEmpty()) serviceId = serviceIdDeque.getFirst();
        String tag = null;
        Deque<String> tagDeque = exchange.getQueryParameters().get("tag");
        if(tagDeque != null && !tagDeque.isEmpty()) tag = tagDeque.getFirst();
        if(logger.isDebugEnabled()) logger.debug("passing = " + passing + " serviceId = " + serviceId + " tag = " + tag);

        exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, "application/json");
        exchange.setStatusCode(200);

        if(serviceId != null) {
            if(tag != null) {
                List nodes = (List)ControllerStartupHook.services.get(serviceId + "|" + tag);
                exchange.getResponseSender().send(JsonMapper.toJson(nodes));
            } else {
                List nodes = (List)ControllerStartupHook.services.get(serviceId);
                exchange.getResponseSender().send(JsonMapper.toJson(nodes));
            }
        } else {
            exchange.getResponseSender().send(JsonMapper.toJson(ControllerStartupHook.services));
        }
    }
}
