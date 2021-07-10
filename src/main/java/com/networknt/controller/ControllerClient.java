package com.networknt.controller;

import com.networknt.client.Http2Client;
import com.networknt.client.oauth.Jwt;
import com.networknt.config.Config;
import com.networknt.config.JsonMapper;
import com.networknt.monad.Result;
import com.networknt.utility.StringUtils;
import io.undertow.UndertowOptions;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.OptionMap;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class ControllerClient {
    private static final Logger logger = LoggerFactory.getLogger(ControllerClient.class);
    private static Http2Client client = Http2Client.getInstance();
    private static OptionMap optionMap = OptionMap.create(UndertowOptions.ENABLE_HTTP2, true);
    private static ControllerConfig config = (ControllerConfig)Config.getInstance().getJsonObjectConfig(ControllerConfig.CONFIG_NAME, ControllerConfig.class);
    private static final int UNUSUAL_STATUS_CODE = 300;

    public static boolean checkHealth(String protocol, String address, int port, String healthPath, String serviceId) {
        String url = protocol + "://" + address + ":" + port;
        if(logger.isTraceEnabled()) logger.trace("url = " + url + " healthPath = " + healthPath + " serviceId = " + serviceId);
        boolean healthy = false;
        ClientConnection connection = null;
        try {
            URI uri = new URI(url);
            if("https".equals(protocol)) {
                connection = client.borrowConnection(uri, Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, optionMap).get();
            } else {
                connection = client.borrowConnection(uri, Http2Client.WORKER, Http2Client.BUFFER_POOL, OptionMap.EMPTY).get();
            }
            if(logger.isTraceEnabled()) logger.trace("borrowed connection = " + connection);
            AtomicReference<ClientResponse> reference = send(connection, Methods.GET, healthPath + serviceId, config.getBootstrapToken(), null);
            if(reference != null && reference.get() != null) {
                int statusCode = reference.get().getResponseCode();
                if (statusCode >= UNUSUAL_STATUS_CODE) {
                    logger.error("Health check error: {} : {}", statusCode, reference.get().getAttachment(Http2Client.RESPONSE_BODY));
                } else {
                    healthy = true;
                }
            }
        } catch (Exception e) {
            logger.error("Health check request exception", e);
        } finally {
            client.returnConnection(connection);
        }
        return healthy;
    }

    public static String getServerInfo(String protocol, String address, int port) {
        String url = protocol + "://" + address + ":" + port;
        String res = "{}";
        ClientConnection connection = null;
        try {
            URI uri = new URI(url);
            if("https".equals(protocol)) {
                connection = client.borrowConnection(uri, Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, optionMap).get();
            } else {
                connection = client.borrowConnection(uri, Http2Client.WORKER, Http2Client.BUFFER_POOL, OptionMap.EMPTY).get();
            }
            AtomicReference<ClientResponse> reference = send(connection, Methods.GET, "/server/info", config.getBootstrapToken(), null);
            if(reference != null && reference.get() != null) {
                int statusCode = reference.get().getResponseCode();
                if (statusCode >= UNUSUAL_STATUS_CODE) {
                    logger.error("Server Info error: {} : {}", statusCode, reference.get().getAttachment(Http2Client.RESPONSE_BODY));
                } else {
                    res = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
                }
            }
        } catch (Exception e) {
            logger.error("Server info request exception", e);
        } finally {
            client.returnConnection(connection);
        }
        return res;
    }

    public static String getLoggerConfig(String protocol, String address, String port) {
        String url = protocol + "://" + address + ":" + port;
        String res = "{}";
        ClientConnection connection = null;
        try {
            URI uri = new URI(url);
            if("https".equals(protocol)) {
                connection = client.borrowConnection(uri, Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, optionMap).get();
            } else {
                connection = client.borrowConnection(uri, Http2Client.WORKER, Http2Client.BUFFER_POOL, OptionMap.EMPTY).get();
            }
            AtomicReference<ClientResponse> reference = send(connection, Methods.GET, "/logger", config.getBootstrapToken(), null);
            if(reference != null && reference.get() != null) {
                int statusCode = reference.get().getResponseCode();
                if (statusCode >= UNUSUAL_STATUS_CODE) {
                    logger.error("Logger config error: {} : {}", statusCode, reference.get().getAttachment(Http2Client.RESPONSE_BODY));
                } else {
                    res = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
                }
            }
        } catch (Exception e) {
            logger.error("Logger config request exception", e);
        } finally {
            client.returnConnection(connection);
        }
        return res;
    }

    public static String updateLoggerConfig(String protocol, String address, int port, List loggers) {
        String url = protocol + "://" + address + ":" + port;
        String res = "{}";
        ClientConnection connection = null;
        try {
            URI uri = new URI(url);
            if("https".equals(protocol)) {
                connection = client.borrowConnection(uri, Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, optionMap).get();
            } else {
                connection = client.borrowConnection(uri, Http2Client.WORKER, Http2Client.BUFFER_POOL, OptionMap.EMPTY).get();
            }
            AtomicReference<ClientResponse> reference = send(connection, Methods.POST, "/logger", config.getBootstrapToken(), JsonMapper.toJson(loggers));
            if(reference != null && reference.get() != null) {
                int statusCode = reference.get().getResponseCode();
                if (statusCode >= UNUSUAL_STATUS_CODE) {
                    logger.error("Logger config error: {} : {}", statusCode, reference.get().getAttachment(Http2Client.RESPONSE_BODY));
                } else {
                    res = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
                }
            }
        } catch (Exception e) {
            logger.error("Logger config request exception", e);
        } finally {
            client.returnConnection(connection);
        }
        return res;
    }

    /**
     * send to service from controller with the health check and server info
     *
     * @param connection ClientConnection
     * @param path       path to send to controller
     * @param token      token to put in header
     * @return AtomicReference<ClientResponse> response
     */
    private static AtomicReference<ClientResponse> send(ClientConnection connection, HttpString method, String path, String token, String json) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        ClientRequest request = new ClientRequest().setMethod(method).setPath(path);
        // add host header for HTTP/1.1 server when HTTP is used.
        request.getRequestHeaders().put(Headers.HOST, "localhost");
        if(config.isDynamicToken()) {
            Result result = client.addCcToken(request);
            if(result.isFailure()) {
                logger.error(result.getError().toString());
            } else {
                if (logger.isTraceEnabled()) logger.trace("Dynamic token  = " + ((Jwt)result.getResult()).getJwt());
            }
        } else {
            if(logger.isTraceEnabled()) logger.trace("Static token = " + token);
            if (token != null) request.getRequestHeaders().put(Headers.AUTHORIZATION, "Bearer "  + token);
        }
        if(StringUtils.isBlank(json)) {
            connection.sendRequest(request, client.createClientCallback(reference, latch));
        } else {
            request.getRequestHeaders().put(Headers.CONTENT_TYPE, "application/json");
            request.getRequestHeaders().put(Headers.TRANSFER_ENCODING, "chunked");
            connection.sendRequest(request, client.createClientCallback(reference, latch, json));
        }
        latch.await(config.getClientTimeout(), TimeUnit.MILLISECONDS);
        return reference;
    }
}
