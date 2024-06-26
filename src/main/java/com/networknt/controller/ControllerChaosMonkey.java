package com.networknt.controller;

import com.networknt.controller.model.ChaosMonkeyAssaultConfigPost;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.chaos.ExceptionAssaultConfig;
import com.networknt.chaos.KillappAssaultConfig;
import com.networknt.chaos.LatencyAssaultConfig;
import com.networknt.chaos.MemoryAssaultConfig;
import com.networknt.config.JsonMapper;
import com.networknt.monad.Failure;
import com.networknt.monad.Result;
import com.networknt.monad.Success;
import com.networknt.status.HttpStatus;
import com.networknt.status.Status;
import io.undertow.util.Methods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.networknt.controller.ControllerConstants.*;

public class ControllerChaosMonkey {

    private static final Logger logger = LoggerFactory.getLogger(ControllerChaosMonkey.class);
    private static final String EXCEPTION_ASSAULT = "com.networknt.chaos.ExceptionAssaultHandler";
    private static final String KILL_APP_ASSAULT = "com.networknt.chaos.KillappAssaultHandler";
    private static final String LATENCY_ASSAULT = "com.networknt.chaos.LatencyAssaultHandler";
    private static final String MEMORY_ASSAULT = "com.networknt.chaos.MemoryAssaultHandler";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final long TIMEOUT = 30000;

    public static Result<String> getChaosMonkeyInfo(String protocol, String address, int port) {
        ServiceRequest serviceRequest = new ServiceRequest.Builder(protocol, address, String.valueOf(port), Methods.GET)
                .buildFullPath(CHAOS_MONKEY_ENDPOINT)
                .build();
        serviceRequest.sendRequest();
        int statusCode = serviceRequest.getStatusCode();
        String responseBody = serviceRequest.getResponseBody();
        if (statusCode >= 400) {
            // error response with a status
            return Failure.of(JsonMapper.fromJson(responseBody, Status.class));
        } else {
            return Success.of(responseBody);
        }
    }

    public static Result<String> postChaosMonkeyAssault(ChaosMonkeyAssaultConfigPost body) {
        ServiceRequest serviceRequest = new ServiceRequest.Builder(body.getProtocol(), body.getAddress(), String.valueOf(body.getPort()), Methods.POST)
                .withRequestBody(body.getAssaultConfig())
                .addPathParam("{assaultType}", body.getAssaultType())
                .buildFullPath(CHAOS_MONKEY_ASSAULT_ENDPOINT)
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

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static ChaosMonkeyAssaultConfigPost<?> getChaosMonkeyAssaultConfigPostBody(String assaultType, String address, int port, String protocol, Object assaultConfig) throws JsonProcessingException {
        ChaosMonkeyAssaultConfigPost config;
        switch (assaultType) {
            case EXCEPTION_ASSAULT:
                config = new ChaosMonkeyAssaultConfigPost<ExceptionAssaultConfig>();
                config.setAssaultConfig(objectMapper.readValue(JsonMapper.toJson(assaultConfig), ExceptionAssaultConfig.class));
                break;
            case KILL_APP_ASSAULT:
                config = new ChaosMonkeyAssaultConfigPost<KillappAssaultConfig>();
                config.setAssaultConfig(objectMapper.readValue(JsonMapper.toJson(assaultConfig), KillappAssaultConfig.class));
                break;
            case LATENCY_ASSAULT:
                config = new ChaosMonkeyAssaultConfigPost<LatencyAssaultConfig>();
                config.setAssaultConfig(objectMapper.readValue(JsonMapper.toJson(assaultConfig), LatencyAssaultConfig.class));
                break;
            case MEMORY_ASSAULT:
                config = new ChaosMonkeyAssaultConfigPost<MemoryAssaultConfig>();
                config.setAssaultConfig(objectMapper.readValue(JsonMapper.toJson(assaultConfig), MemoryAssaultConfig.class));
                break;
            default:
                return null;
        }
        config.setAssaultType(assaultType);
        config.setPort(port);
        config.setProtocol(protocol);
        config.setAddress(address);
        return config;
    }

    /**
     * Initiate a specified chaos monkey assault and get the logs of the service after.
     * We do not want to try to grab logs of killapp or exception, because this means the service is shutdown.
     *
     * @param assaultType - chaos monkey assault handler.
     * @param address     - address of service we are going to assault.
     * @param port        - port of service we are going to assault.
     * @param protocol    - protocol of service we are going to assault.
     * @param endpoint    - endpoint of service we are going to hit
     * @param reqCount    - the amount of times we are going to request until the assault is complete.
     * @return - return confirmation string that the assault happened or the log entries.
     */
    public static String initChaosMonkeyAssault(String assaultType, String address, int port, String protocol, String endpoint, int reqCount) {
        boolean testCompleted;
        String assaultHandlerName;
        String startTime = String.valueOf(System.currentTimeMillis());
        switch (assaultType) {
            case EXCEPTION_ASSAULT:
                testCompleted = initExceptionAssault(protocol, address, String.valueOf(port), endpoint, reqCount);
                assaultHandlerName = "Exception Assault";
                break;
            case KILL_APP_ASSAULT:
                testCompleted = initKillAppAssault(protocol, address, String.valueOf(port), endpoint, reqCount);
                assaultHandlerName = "Kill App Assault";
                break;
            case LATENCY_ASSAULT:
                testCompleted = initLatencyAssault(protocol, address, String.valueOf(port), endpoint, reqCount);
                assaultHandlerName = "Latency Assault";
                break;
            case MEMORY_ASSAULT:
                testCompleted = initMemoryAssault(protocol, address, String.valueOf(port), endpoint, reqCount);
                assaultHandlerName = "Memory Assault";
                break;
            default:
                assaultHandlerName = "Unknown Assault Type: " + assaultType;
                testCompleted = false;
                break;
        }
        String endTime = String.valueOf(System.currentTimeMillis());

        if (testCompleted) {
            return confirmLog(address, port, protocol, startTime, endTime);
        } else {
            return "Test was not completed for triggered assault: " + assaultHandlerName + "\n" + "Is the handler name correct? Is the specified handler enabled with bypass disabled?";
        }
    }

    /**
     * Retrieve the logs after chaos monkey assault to confirm the test happened.
     *
     * @param address   - address of service
     * @param port      - port of service
     * @param protocol  - protocol of service.
     * @param startTime - startTime of log
     * @param endTime   - endTime of log
     * @return - return JSON string of log entries from service.
     */
    private static String confirmLog(String address, int port, String protocol, String startTime, String endTime) {
        long currentTime = System.currentTimeMillis();
        long timeout = currentTime + TIMEOUT;
        while (currentTime < timeout) {
            ServiceRequest serviceRequest = new ServiceRequest.Builder(protocol, address, String.valueOf(port), Methods.GET)
                    .addQueryParam("startTime", startTime)
                    .addQueryParam("endTime", endTime)
                    .buildFullPath(LOGGER_CONTENT_ENDPOINT)
                    .build();
            serviceRequest.sendRequest();

            if (serviceRequest.getStatusCode() == HttpStatus.OK.value()) {
                return serviceRequest.getResponseBody();
            }
            currentTime = System.currentTimeMillis();
        }
        return "Timeout reached " + TIMEOUT + "ms, cannot retrieve logs.";

    }

    private static boolean initExceptionAssault(String protocol, String address, String port, String endpoint, int reqCount) {
        for (int i = 0; i < reqCount; i++) {
            ServiceRequest serviceRequest = new ServiceRequest.Builder(protocol, address, port, Methods.GET)
                    .buildFullPath(endpoint)
                    .build();
            serviceRequest.sendRequest();
            if (serviceRequest.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                logger.info("Service responded with 500 and is no longer reachable");
                return true;
            }
            if (serviceRequest.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                return true;
            }
        }

        return false;
    }

    private static boolean initLatencyAssault(String protocol, String address, String port, String endpoint, int reqCount) {
        for (int i = 0; i < reqCount; i++) {
            ServiceRequest serviceRequest = new ServiceRequest.Builder(protocol, address, port, Methods.GET)
                    .buildFullPath(endpoint)
                    .build();
            serviceRequest.sendRequest();
            if (serviceRequest.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                logger.info("Service responded with 500 and is no longer reachable");
                return true;
            }
            if (serviceRequest.getStatusCode() == HttpStatus.REQUEST_TIMEOUT.value()) {
                return true;
            }
        }
        return false;
    }

    private static boolean initKillAppAssault(String protocol, String address, String port, String endpoint, int reqCount) {
        for (int i = 0; i < reqCount; i++) {
            ServiceRequest serviceRequest = new ServiceRequest.Builder(protocol, address, port, Methods.GET)
                    .buildFullPath(endpoint)
                    .build();
            serviceRequest.sendRequest();
            if (serviceRequest.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                logger.info("Service responded with 500 and is no longer reachable");
                return true;
            }
            if (serviceRequest.getStatusCode() == HttpStatus.BAD_GATEWAY.value() ||
                    serviceRequest.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE.value()) {
                return true;
            }
        }
        return false;
    }

    private static boolean initMemoryAssault(String protocol, String address, String port, String endpoint, int reqCount) {
        for (int i = 0; i < reqCount; i++) {
            ServiceRequest serviceRequest = new ServiceRequest.Builder(protocol, address, port, Methods.GET)
                    .buildFullPath(endpoint)
                    .build();
            serviceRequest.sendRequest();
            if (serviceRequest.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                logger.info("Service responded with 500 and is no longer reachable");
                return true;
            }
            if (serviceRequest.getStatusCode() == HttpStatus.BAD_REQUEST.value() ||
                    serviceRequest.getStatusCode() == HttpStatus.BAD_GATEWAY.value() ||
                    serviceRequest.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE.value()) {
                return true;
            }
        }
        return false;
    }
}
