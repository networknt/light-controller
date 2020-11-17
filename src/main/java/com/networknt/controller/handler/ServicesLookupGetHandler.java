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

/**
 * Get all healthy nodes for a registered service from the controller. This endpoint is used for
 * service discovery from the portal-registry. A serviceId query parameter is required and a tag
 * parameter is optional.
 *
 * @author Steve Hu
 */
public class ServicesLookupGetHandler implements LightHttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(ServicesLookupGetHandler.class);

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        String serviceId = exchange.getQueryParameters().get("serviceId").getFirst();
        boolean passing = false;
        Deque<String> passingDeque = exchange.getQueryParameters().get("passing");
        if(passingDeque != null && !passingDeque.isEmpty()) {
            passing = true;
        }
        String tag = null;
        Deque<String> tagDeque = exchange.getQueryParameters().get("tag");
        if(tagDeque != null && !tagDeque.isEmpty()) tag = tagDeque.getFirst();
        if(logger.isDebugEnabled()) logger.debug("passing = " + passing + " serviceId = " + serviceId + " tag = " + tag);

        exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, "application/json");
        exchange.setStatusCode(200);
        if(tag != null) {
            List nodes = (List) ControllerStartupHook.services.get(serviceId + "|" + tag);
            exchange.getResponseSender().send(JsonMapper.toJson(nodes));
        } else {
            List nodes = (List)ControllerStartupHook.services.get(serviceId);
            exchange.getResponseSender().send(JsonMapper.toJson(nodes));
        }
    }
}
