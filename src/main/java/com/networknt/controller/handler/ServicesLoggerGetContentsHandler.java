package com.networknt.controller.handler;

import com.networknt.controller.ControllerClient;
import com.networknt.controller.model.LoggerInfo;
import com.networknt.handler.LightHttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;

import static com.networknt.controller.ControllerConstants.*;

public class ServicesLoggerGetContentsHandler implements LightHttpHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ServicesLoggerGetContentsHandler.class);
    private static final String LOGGER_NAME_PARAM = "loggerName";
    private static final String LOGGER_LEVEL_PARAM = "loggerLevel";
    private static final String START_TIME_PARAM = "startTime";
    private static final String END_TIME_PARAM = "endTime";

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        String protocol = exchange.getQueryParameters().get(PROTOCOL).getFirst();
        String address = exchange.getQueryParameters().get(ADDRESS).getFirst();
        String port = exchange.getQueryParameters().get(PORT).getFirst();

        String loggerName = exchange.getQueryParameters().get(LOGGER_NAME_PARAM).getFirst();
        String loggerLevel = exchange.getQueryParameters().get(LOGGER_LEVEL_PARAM).getFirst();
        String startTime = exchange.getQueryParameters().get(START_TIME_PARAM).getFirst();
        String endTime = exchange.getQueryParameters().get(END_TIME_PARAM).getFirst();

        LoggerInfo loggerInfo = new LoggerInfo();
        if(loggerName != null) {
            loggerInfo.setName(loggerName);
        }
        if(loggerLevel != null) {
            loggerInfo.setLevel(LoggerInfo.LevelEnum.fromValue(loggerLevel));
        }

        if(LOG.isTraceEnabled()) LOG.trace("protocol = " + protocol + " address = " + address + " port = " + port);

        // use the above info to call the service to get the loggers.
        String result = ControllerClient.getLogContents(protocol, address, Integer.parseInt(port), loggerInfo, startTime, endTime);

        exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        exchange.setStatusCode(200);
        exchange.getResponseSender().send(result);
    }
}