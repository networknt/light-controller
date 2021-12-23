package com.networknt.controller.handler;

import com.networknt.controller.ControllerClient;
import com.networknt.controller.model.LoggerInfo;
import com.networknt.handler.LightHttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.networknt.controller.ControllerConstants.*;

public class ServicesLoggerGetContentsHandler implements LightHttpHandler {

    private static final Logger logger = LoggerFactory.getLogger(ServicesLoggerGetContentsHandler.class);

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        String protocol = exchange.getQueryParameters().get(PROTOCOL).getFirst();
        String address = exchange.getQueryParameters().get(ADDRESS).getFirst();
        String port = exchange.getQueryParameters().get(PORT).getFirst();

        String loggerName = exchange.getQueryParameters().get("loggerName").getFirst();
        String loggerLevel = exchange.getQueryParameters().get("loggerLevel").getFirst();
        String startTime = exchange.getQueryParameters().get("startTime").getFirst();
        String endTime = exchange.getQueryParameters().get("endTime").getFirst();

        LoggerInfo loggerInfo = new LoggerInfo();
        if(loggerName != null) {
            loggerInfo.setName(loggerName);
        }
        if(loggerLevel != null) {
            loggerInfo.setLevel(LoggerInfo.LevelEnum.fromValue(loggerLevel));
        }


        if(logger.isTraceEnabled()) logger.trace("protocol = " + protocol + " address = " + address + " port = " + port);

        // use the above info to call the service to get the loggers.
        String result = ControllerClient.getLogContents(protocol, address, Integer.parseInt(port), loggerInfo, startTime, endTime);


        exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
        exchange.setStatusCode(200);
        exchange.getResponseSender().send(result);
    }
}
