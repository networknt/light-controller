package com.networknt.controller.handler;

import com.networknt.body.BodyHandler;
import com.networknt.controller.ControllerClient;
import com.networknt.controller.model.LoggerInfo;
import com.networknt.handler.LightHttpHandler;
import com.networknt.http.MediaType;
import com.networknt.utility.StringUtils;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

import static com.networknt.controller.ControllerConstants.*;

/**
 * Called from the portal-view UI to retrieve logs from the registered service.
 */
public class ServicesLoggerContentPostHandler implements LightHttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(ServicesLoggerContentPostHandler.class);
    private static final String LOGGER_NAME_PARAM = "loggerName";
    private static final String LOGGER_LEVEL_PARAM = "loggerLevel";
    private static final String START_TIME_PARAM = "startTime";
    private static final String END_TIME_PARAM = "endTime";

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        Map<String, Object> bodyMap = (Map<String, Object>) exchange.getAttachment(BodyHandler.REQUEST_BODY);
        String protocol = (String) bodyMap.get(PROTOCOL);
        String address = (String) bodyMap.get(ADDRESS);
        Integer port = (Integer) bodyMap.get(PORT);

        String loggerName = (String) bodyMap.get(LOGGER_NAME_PARAM);
        String loggerLevel = (String) bodyMap.get(LOGGER_LEVEL_PARAM);
        String startTimeStr = (String) bodyMap.get(START_TIME_PARAM);
        String endTimeStr = (String) bodyMap.get(END_TIME_PARAM);

        // The startTime and endTime might be milliseconds or datetime format.
        String startTime = startTimeStr;
        if (startTimeStr != null && !StringUtils.isNumeric(startTimeStr)) {
            LocalDateTime startDateTime = LocalDateTime.parse(startTimeStr);
            Instant instant = startDateTime.atZone(ZoneId.systemDefault()).toInstant();
            long timeInMillis = instant.toEpochMilli();
            startTime = "" + timeInMillis;
        }
        String endTime = endTimeStr;
        if (endTimeStr != null && !StringUtils.isNumeric(endTimeStr)) {
            LocalDateTime endDateTime = LocalDateTime.parse(endTimeStr);
            Instant instant = endDateTime.atZone(ZoneId.systemDefault()).toInstant();
            long timeInMillis = instant.toEpochMilli();
            endTime = "" + timeInMillis;
        }

        LoggerInfo loggerInfo = new LoggerInfo();
        if (loggerName != null) {
            loggerInfo.setName(loggerName);
        }
        if (loggerLevel != null) {
            loggerInfo.setLevel(LoggerInfo.LevelEnum.fromValue(loggerLevel));
        }

        if (logger.isTraceEnabled())
            logger.trace("protocol = " + protocol +
                    " address = " + address +
                    " port = " + port +
                    " loggerName = " + loggerName +
                    " loggerLevel = " + loggerLevel +
                    " startTime = " + startTime +
                    " endTime = " + endTime);

        // use the above info to call the service to get the loggers.
        String result = ControllerClient.getLogContents(protocol, address, port, loggerInfo, startTime, endTime);
        exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        exchange.setStatusCode(200);
        exchange.getResponseSender().send(result);
    }
}
