package com.networknt.controller.handler;

import com.networknt.controller.ControllerClient;
import com.networknt.handler.LightHttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.networknt.controller.ControllerConstants.*;

/**
 * This endpoint is used to get a list of loggers with their levels for a target server instance. This is
 * the first step to update logging levels for that instance.
 *
 * @author Steve Hu
 */
public class ServicesLoggerGetHandler implements LightHttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(ServicesLoggerGetHandler.class);

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        String protocol = exchange.getQueryParameters().get(PROTOCOL).getFirst();
        String address = exchange.getQueryParameters().get(ADDRESS).getFirst();
        String port = exchange.getQueryParameters().get(PORT).getFirst();

        if(logger.isTraceEnabled()) logger.trace("protocol = " + protocol + " address = " + address + " port = " + port);
        // use the above info to call the service to get the loggers.
        String result = ControllerClient.getLoggerConfig(protocol, address, port);
        exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
        exchange.setStatusCode(200);
        exchange.getResponseSender().send(result);
    }
}
