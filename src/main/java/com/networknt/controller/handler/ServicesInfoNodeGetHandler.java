package com.networknt.controller.handler;

import com.networknt.controller.ControllerClient;
import com.networknt.handler.LightHttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Query server info based on the serviceId and tag. There might be multiple instances that separated
 * by IP and Port. List all of them in an array of service info objects returned from the /server/info
 * endpoint.
 *
 * @author Steve Hu
 */
public class ServicesInfoNodeGetHandler implements LightHttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(ServicesDeleteHandler.class);

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        /* get server info */
        String protocol = exchange.getQueryParameters().get("protocol").getFirst();
        String address = exchange.getQueryParameters().get("address").getFirst();
        int port = Integer.parseInt(exchange.getQueryParameters().get("port").getFirst());
        String info = ControllerClient.getServerInfo(protocol, address, port);

        exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
        exchange.setStatusCode(200);
        exchange.getResponseSender().send(info);
    }
}
