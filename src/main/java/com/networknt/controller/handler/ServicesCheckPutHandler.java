package com.networknt.controller.handler;

import com.networknt.body.BodyHandler;
import com.networknt.controller.ControllerStartupHook;
import com.networknt.controller.model.Check;
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
        if(pass) {
            // if the lastFailedTimestamp is not 0L, then reset it to 0L as it is passed. If it doesn't exist, it
            // means the node is removed already due to pass the de-register after period.
            Check check = ControllerStartupHook.checks.get(id);
            if(check != null) {
                if(check.getLastFailedTimestamp() != 0L) {
                    check.setLastFailedTimestamp(0L);
                }
                check.setLastExecuteTimestamp(System.currentTimeMillis());
            }
        } else {
            // update the lastFailedTimestamp in the check object in checks map. If it keeps failing, then don't
            // update it. If it is passed, then reset to 0L again. This will allow the job to remove the node if
            // the it fails for a long time greater than the de-register after setting in the check.
            Check check = ControllerStartupHook.checks.get(id);
            if(check != null) {
                long timestamp = System.currentTimeMillis();
                if(check.getLastFailedTimestamp() == 0L) {
                    check.setLastFailedTimestamp(timestamp);
                }
                check.setLastExecuteTimestamp(timestamp);
            }
        }
        setExchangeStatus(exchange, SUC10200);
    }
}
