package com.networknt.controller.handler;

import com.networknt.controller.ControllerChaosMonkey;
import com.networknt.handler.LightHttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServicesChaosMonkeyGetHandler implements LightHttpHandler {

    private static final Logger logger = LoggerFactory.getLogger(ServicesChaosMonkeyGetHandler.class);

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {

        // get all params
        String protocol = exchange.getQueryParameters().get("protocol").getFirst();
        String address = exchange.getQueryParameters().get("address").getFirst();
        int port = Integer.parseInt(exchange.getQueryParameters().get("port").getFirst());



        String res = ControllerChaosMonkey.getChaosMonkeyInfo(protocol, address, port);
        exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, "application/json");
        exchange.setStatusCode(200);
        exchange.getResponseSender().send(res);
    }

}
