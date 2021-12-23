package com.networknt.controller;

import com.networknt.controller.model.LoggerInfo;
import io.undertow.util.Methods;
import java.util.List;

import static com.networknt.controller.ControllerConstants.*;

public class ControllerClient {

    public static boolean checkHealth(String protocol, String address, int port, String healthPath, String serviceId) {
        ServiceRequest serviceRequest = new ServiceRequest.Builder(protocol, address, String.valueOf(port), Methods.GET)
                .buildFullPath(healthPath + serviceId)
                .build();
        serviceRequest.sendRequest();
        return serviceRequest.getStatusCode() == 200;
    }

    public static String getServerInfo(String protocol, String address, int port) {
        ServiceRequest serviceRequest = new ServiceRequest.Builder(protocol, address, String.valueOf(port), Methods.GET)
                .buildFullPath(SERVER_INFO_ENDPOINT)
                .build();
        serviceRequest.sendRequest();
        return serviceRequest.getResponseBody();
    }

    public static String getLoggerConfig(String protocol, String address, String port) {
        ServiceRequest serviceRequest = new ServiceRequest.Builder(protocol, address, String.valueOf(port), Methods.GET)
                .buildFullPath(LOGGER_ENDPOINT)
                .build();
        serviceRequest.sendRequest();
        return serviceRequest.getResponseBody();
    }

    public static String updateLoggerConfig(String protocol, String address, int port, List<?> loggers) {
        ServiceRequest serviceRequest = new ServiceRequest.Builder(protocol, address, String.valueOf(port), Methods.POST)
                .withRequestBody(loggers)
                .buildFullPath(LOGGER_ENDPOINT)
                .build();
        serviceRequest.sendRequest();
        return serviceRequest.getResponseBody();
    }

    public static String getLogContents(String protocol, String address, int port, LoggerInfo loggerInfo, String startTime, String endTime) {
        ServiceRequest.Builder serviceRequestBuilder = new ServiceRequest.Builder(protocol, address, String.valueOf(port), Methods.GET)
                .addQueryParam("startTime", startTime)
                .addQueryParam("endTime", endTime)
                .buildFullPath(LOGGER_CONTENT_ENDPOINT);

        if(loggerInfo.getName() != null) {
            serviceRequestBuilder.addQueryParam("loggerName", loggerInfo.getName());
        }

        if(loggerInfo.getLevel() != null) {
            serviceRequestBuilder.addQueryParam("loggerLevel", loggerInfo.getLevel().toString());
        }

        ServiceRequest serviceRequest = serviceRequestBuilder.build();
        serviceRequest.sendRequest();
        return serviceRequest.getResponseBody();
    }

}
