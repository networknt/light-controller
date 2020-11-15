package com.networknt.controller;

import com.networknt.client.Http2Client;
import com.networknt.config.Config;
import io.undertow.UndertowOptions;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.OptionMap;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class ControllerClient {
    private static final Logger logger = LoggerFactory.getLogger(ControllerClient.class);
    private static Http2Client client = Http2Client.getInstance();
    private static OptionMap optionMap = OptionMap.create(UndertowOptions.ENABLE_HTTP2, true);
    private static ControllerConfig config = (ControllerConfig)Config.getInstance().getJsonObjectConfig(ControllerConfig.CONFIG_NAME, ControllerConfig.class);
    private static final int UNUSUAL_STATUS_CODE = 300;

    public static boolean checkHealth(String address, int port, String serviceId) {
        String url = "https://" + address + ":" + port;
        boolean healthy = false;
        ClientConnection connection = null;
        try {
            URI uri = new URI(url);
            connection = client.borrowConnection(uri, Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, optionMap).get();
            AtomicReference<ClientResponse> reference = send(connection, "/health/" + serviceId, config.getBootstrapToken());
            int statusCode = reference.get().getResponseCode();
            if (statusCode >= UNUSUAL_STATUS_CODE) {
                logger.error("Health check error: {} : {}", statusCode, reference.get().getAttachment(Http2Client.RESPONSE_BODY));
            } else {
                healthy = true;
            }
        } catch (Exception e) {
            logger.error("Health check request exception", e);
        } finally {
            client.returnConnection(connection);
        }
        return healthy;
    }

    public static String getServerInfo(String address, int port) {
        String url = "https://" + address + ":" + port;
        String res = null;
        ClientConnection connection = null;
        try {
            URI uri = new URI(url);
            connection = client.borrowConnection(uri, Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, optionMap).get();
            AtomicReference<ClientResponse> reference = send(connection, "/server/info", config.getBootstrapToken());
            int statusCode = reference.get().getResponseCode();
            if (statusCode >= UNUSUAL_STATUS_CODE) {
                logger.error("Server Info error: {} : {}", statusCode, reference.get().getAttachment(Http2Client.RESPONSE_BODY));
            } else {
                res = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
            }
        } catch (Exception e) {
            logger.error("Server info request exception", e);
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
    private static AtomicReference<ClientResponse> send(ClientConnection connection, String path, String token) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        ClientRequest request = new ClientRequest().setMethod(Methods.GET).setPath(path);
        if (token != null) request.getRequestHeaders().put(Headers.AUTHORIZATION, "Bearer "  + token);
        connection.sendRequest(request, client.createClientCallback(reference, latch));
        latch.await();
        return reference;
    }
}
