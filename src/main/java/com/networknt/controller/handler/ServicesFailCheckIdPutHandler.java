package com.networknt.controller.handler;

import com.networknt.handler.LightHttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * If TTL check is selected during the service register, the service will send TTL request periodically to indicate
 * if it is healthy or not. This endpoint is telling the controller a particular service instance is not healthy.
 *
 * @author Steve Hu
 */
public class ServicesFailCheckIdPutHandler implements LightHttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(ServicesFailCheckIdPutHandler.class);

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        exchange.endExchange();
    }
}
