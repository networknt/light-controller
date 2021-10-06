package com.networknt.controller.handler;

import com.networknt.controller.ControllerChaosMonkey;
import com.networknt.body.BodyHandler;
import com.networknt.handler.LightHttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class ServicesChaosMonkeyAssaultPostHandler implements LightHttpHandler {

    private static final Logger logger = LoggerFactory.getLogger(ServicesChaosMonkeyAssaultPostHandler.class);

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {

        // get data from body
        Map<String, Object> body = (Map<String, Object>)exchange.getAttachment(BodyHandler.REQUEST_BODY);
        String protocol = body.getOrDefault("protocol", "null").toString();
        String address = body.getOrDefault("address", "null").toString();
        String assaultType = body.getOrDefault("assaultType", "null").toString();
        int port = Integer.parseInt(body.getOrDefault("port", "0").toString());
        int reqCount = Integer.parseInt(body.getOrDefault("requests", "0").toString());
        String endpointTest = body.getOrDefault("endpoint", "null").toString();

        String res = ControllerChaosMonkey.initChaosMonkeyAssault(assaultType, address, port, protocol, endpointTest, reqCount);
        exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
        exchange.setStatusCode(200);
        exchange.getResponseSender().send(res);

    }

}
