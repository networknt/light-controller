package com.networknt.controller.handler;

import com.networknt.controller.ControllerClient;
import com.networknt.body.BodyHandler;
import com.networknt.handler.LightHttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is the endpoint to update logging levels for a list of loggers on a target service instance.
 *
 * @author Steve Hu
 */
public class ServicesLoggerPostHandler implements LightHttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(ServicesLoggerPostHandler.class);

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        Map<String, Object> body = (Map<String, Object>)exchange.getAttachment(BodyHandler.REQUEST_BODY);
        String protocol = (String)body.get("protocol");
        String address = (String)body.get("address");
        Integer port = (Integer)body.get("port");
        List loggers = (List)body.get("loggers");
        if(logger.isTraceEnabled()) logger.trace("protocol = " + protocol + " address = " + address + " port = " + port);
        String result = ControllerClient.updateLoggerConfig(protocol, address, port, loggers);
        exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
        exchange.setStatusCode(200);
        exchange.getResponseSender().send(result);
    }
}
