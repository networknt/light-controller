package com.networknt.controller;

import com.networknt.config.JsonMapper;
import com.networknt.controller.model.LoggerInfo;
import com.networknt.controller.model.ServerShutdownRequest;
import com.networknt.monad.Failure;
import com.networknt.monad.Result;
import com.networknt.monad.Success;
import com.networknt.status.Status;

import io.undertow.util.Methods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.networknt.controller.ControllerConstants.*;

public class ControllerClient {
    public static final Logger logger = LoggerFactory.getLogger(ControllerClient.class);

    public static boolean checkHealth(String protocol, String address, int port, String healthPath, String serviceId) {
        if(logger.isTraceEnabled()) logger.trace("checkHealth protocol = " + protocol + " address = " + address + " port = " + port + " healthPath = " + healthPath + " serviceId = " + serviceId);
        ServiceRequest serviceRequest = new ServiceRequest.Builder(protocol, address, String.valueOf(port), Methods.GET)
                .buildFullPath(healthPath + serviceId)
                .build();
        serviceRequest.sendRequest();
        return serviceRequest.getStatusCode() == 200;
    }

    public static String getServerInfo(String protocol, String address, int port) {
        if(logger.isTraceEnabled()) logger.trace("getServerInfo protocol = " + protocol + " address = " + address + " port = " + port);
        ServiceRequest serviceRequest = new ServiceRequest.Builder(protocol, address, String.valueOf(port), Methods.GET)
                .buildFullPath(SERVER_INFO_ENDPOINT)
                .build();
        serviceRequest.sendRequest();
        if(logger.isTraceEnabled()) logger.trace("response code = " + serviceRequest.getStatusCode() + " body size = " + (serviceRequest.getResponseBody() == null ? 0 : serviceRequest.getResponseBody().length()));
        return serviceRequest.getResponseBody();
    }

    public static String getLoggerConfig(String protocol, String address, String port) {
        if(logger.isTraceEnabled()) logger.trace("protocol = " + protocol + " address = " + address + " port = " + port);
        ServiceRequest serviceRequest = new ServiceRequest.Builder(protocol, address, String.valueOf(port), Methods.GET)
                .buildFullPath(LOGGER_ENDPOINT)
                .build();
        serviceRequest.sendRequest();
        return serviceRequest.getResponseBody();
    }

    public static String updateLoggerConfig(String protocol, String address, int port, List<?> loggers) {
        if(logger.isTraceEnabled()) logger.trace("protocol = " + protocol + " address = " + address + " port = " + port + " loggers = " + JsonMapper.toJson(loggers));
        ServiceRequest serviceRequest = new ServiceRequest.Builder(protocol, address, String.valueOf(port), Methods.POST)
                .withRequestBody(loggers)
                .buildFullPath(LOGGER_ENDPOINT)
                .build();
        serviceRequest.sendRequest();
        return serviceRequest.getResponseBody();
    }

    public static String getLogContents(String protocol, String address, int port, LoggerInfo loggerInfo, String startTime, String endTime) {
        if(logger.isTraceEnabled()) {
            logger.trace("protocol = " + protocol + " address = " + address + " port = " + port);
            logger.trace("loggerInfo = " + loggerInfo + " startTime = " +  startTime + " endTime = " + endTime);
        }
        String loggerLevel = LoggerInfo.LevelEnum.ERROR.toString();
        if (loggerInfo.getLevel() != null) {
            loggerLevel = loggerInfo.getLevel().toString();
        }

        ServiceRequest serviceRequest = new ServiceRequest.Builder(protocol, address, String.valueOf(port), Methods.GET)
                .addQueryParam("startTime", startTime)
                .addQueryParam("endTime", endTime)
                .addQueryParam("loggerName", loggerInfo.getName())
                .addQueryParam("loggerLevel", loggerLevel)
                .buildFullPath(LOGGER_CONTENT_ENDPOINT)
                .build();

        serviceRequest.sendRequest();
        return serviceRequest.getResponseBody();
    }
    
    public static String getModuleList(String protocol, String address, String port) {
        if(logger.isTraceEnabled()) logger.trace("protocol = " + protocol + " address = " + address + " port = " + port);
        ServiceRequest serviceRequest = new ServiceRequest.Builder(protocol, address, String.valueOf(port), Methods.GET)
                .buildFullPath(RELOAD_CONFIG_ENDPOINT)
                .build();
        serviceRequest.sendRequest();
        return serviceRequest.getResponseBody();
    }
    
    public static String reloadModuleConfig(String protocol, String address, int port, List<String> modules) {
        if(logger.isTraceEnabled()) logger.trace("protocol = " + protocol + " address = " + address + " port = " + port + " loggers = " + JsonMapper.toJson(modules));
        ServiceRequest serviceRequest = new ServiceRequest.Builder(protocol, address, String.valueOf(port), Methods.POST)
                .withRequestBody(modules)
                .buildFullPath(RELOAD_CONFIG_ENDPOINT)
                .build();
        serviceRequest.sendRequest();
        return serviceRequest.getResponseBody();
    }
    
    public static Result<String> shutdownService(ServerShutdownRequest request) {
        if(logger.isTraceEnabled()) logger.trace("protocol = " + request.getProtocol() + " address = " + request.getAddress() + " port = " + request.getPort() + " loggers = " + JsonMapper.toJson(request));
        ServiceRequest serviceRequest = new ServiceRequest.Builder(request.getProtocol(), request.getAddress(), String.valueOf(request.getPort()), Methods.POST)
                .withRequestBody(request)
                .buildFullPath(SHUTDOWN_SERVICE_ENDPOINT)
                .build();
        serviceRequest.sendRequest();
        
        int statusCode = serviceRequest.getStatusCode();
        String responseBody = serviceRequest.getResponseBody();
        if (statusCode >= 400) {
            return Failure.of(JsonMapper.fromJson(responseBody, Status.class));
        } else {
            return Success.of(responseBody);
        }
    }

}
