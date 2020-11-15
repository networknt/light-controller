package com.networknt.controller.handler;

import com.networknt.body.BodyHandler;
import com.networknt.handler.LightHttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * If TTL check is selected during the service register, the service will send TTL request periodically to indicate
 * if it is healthy or not. This endpoint is telling the controller a particular service instance status with check
 * id and a pass status.
 *
 * @author Steve Hu
 */
public class ServicesCheckPutHandler implements LightHttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(ServicesCheckPutHandler.class);
    private static final String SUC10200 = "SUC10200";

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        Map<String, Object> body = (Map<String, Object>)exchange.getAttachment(BodyHandler.REQUEST_BODY);
        String id = (String)body.get("id");
        boolean pass = (Boolean)body.get("pass");
        if(logger.isTraceEnabled()) logger.trace("id = " + id + " pass = " + pass);


        setExchangeStatus(exchange, SUC10200);
    }
}
