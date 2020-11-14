package com.networknt.controller.handler;

import com.networknt.body.BodyHandler;
import com.networknt.config.JsonMapper;
import com.networknt.controller.ControllerStartupHook;
import com.networknt.handler.LightHttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Remove a service from the registry with this put request. If the service doesn't exist, then no action
 * is taken. It is normally called during the service shutdown phase.
 *
 * @author Steve Hu
 */
public class ServicesDeleteHandler implements LightHttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(ServicesDeleteHandler.class);
    private static final String SUC10200 = "SUC10200";

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        String serviceId = exchange.getQueryParameters().get("serviceId").getFirst();
        String tag = null;
        Deque<String> tagDeque = exchange.getQueryParameters().get("tag");
        if(tagDeque != null && !tagDeque.isEmpty()) tag = tagDeque.getFirst();
        String key = tag == null ?  serviceId : serviceId + "|" + tag;
        String address = exchange.getQueryParameters().get("address").getFirst();
        int port = Integer.valueOf(exchange.getQueryParameters().get("port").getFirst());
        if(logger.isDebugEnabled()) logger.debug("serviceId = " + serviceId + " tag = " + tag + " address = " + address + " port = " + port);

        List nodes = (List)ControllerStartupHook.services.get(key);
        nodes = delService(nodes, address, port);
        if(nodes != null && nodes.size() > 0) {
            ControllerStartupHook.services.put(key, nodes);
        } else {
            ControllerStartupHook.services.remove(key);
        }
        setExchangeStatus(exchange, SUC10200);
    }

    private List delService(List nodes, String address, int port) {
        if(nodes != null) {
            for(Iterator<Map<String, Object>> iter = nodes.iterator(); iter.hasNext(); ) {
                Map<String, Object> map = iter.next();
                String a = (String)map.get("address");
                int p = (Integer)map.get("port");
                if (address.equals(a) && port == p)
                    iter.remove();
            }
        }
        return nodes;
    }
}
