package com.networknt.controller;

import io.undertow.util.Methods;
import java.util.List;

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
                .buildFullPath("/server/info")
                .build();
        serviceRequest.sendRequest();
        return serviceRequest.getResponseBody();
    }

    public static String getLoggerConfig(String protocol, String address, String port) {
        ServiceRequest serviceRequest = new ServiceRequest.Builder(protocol, address, String.valueOf(port), Methods.GET)
                .buildFullPath("/logger")
                .build();
        serviceRequest.sendRequest();
        return serviceRequest.getResponseBody();
    }

    public static String updateLoggerConfig(String protocol, String address, int port, List loggers) {
        ServiceRequest serviceRequest = new ServiceRequest.Builder(protocol, address, String.valueOf(port), Methods.POST)
                .withRequestBody(loggers)
                .buildFullPath("/logger")
                .build();
        serviceRequest.sendRequest();
        return serviceRequest.getResponseBody();
    }

}
