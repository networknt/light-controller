package com.networknt.controller.handler;

import com.networknt.controller.ControllerClient;
import com.networknt.body.BodyHandler;
import com.networknt.handler.LightHttpHandler;
import com.networknt.http.MediaType;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Map;

import static com.networknt.controller.ControllerConstants.*;

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
        String protocol = (String)body.get(PROTOCOL);
        String address = (String)body.get(ADDRESS);
        Integer port = (Integer)body.get(PORT);

        List loggers = (List)body.get("loggers");
        if(logger.isTraceEnabled()) logger.trace("protocol = " + protocol + " address = " + address + " port = " + port);
        String result = ControllerClient.updateLoggerConfig(protocol, address, port, loggers);
        exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        exchange.setStatusCode(200);
        exchange.getResponseSender().send(result);
    }
}
