package com.networknt.controller.handler;

import com.networknt.controller.ControllerStartupHook;
import com.networknt.client.Http2Client;
import com.networknt.config.Config;
import com.networknt.config.JsonMapper;
import com.networknt.controller.ControllerUtil;
import com.networknt.handler.LightHttpHandler;
import com.networknt.monad.Failure;
import com.networknt.monad.Result;
import com.networknt.monad.Success;
import com.networknt.server.Server;
import com.networknt.status.Status;
import com.networknt.utility.NetUtils;
import com.networknt.utility.StringUtils;
import io.undertow.UndertowOptions;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.errors.InvalidStateStoreException;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.apache.kafka.streams.state.StreamsMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.OptionMap;

import java.net.URI;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Get all the registered services from the controller for the view to display. This is a separate
 * endpoint as the view might use basic authentication instead of OAuth 2.0 jwt token for service
 * lookup from discovery. In normal cases, all endpoints should have the same authentication handler;
 * however, we need to allow basic authentication as an option for some users with small scale usage.
 *
 * @author Steve Hu
 */
public class ServicesGetHandler implements LightHttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(ServicesGetHandler.class);
    static Http2Client client = Http2Client.getInstance();
    static final String GENERIC_EXCEPTION = "ERR10014";
    static final long WAIT_THRESHOLD = 30000;

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, "application/json");
        exchange.setStatusCode(200);
        if (ControllerStartupHook.config.isClusterMode()) {
            // get query parameter for the local indicator.
            boolean local = false;
            Deque<String> localDeque = exchange.getQueryParameters().get("local");
            if (localDeque != null && !localDeque.isEmpty()) local = true;
            if (local) {
                exchange.getResponseSender().send(JsonMapper.toJson(getLocalServices()));
            } else {
                Collection<StreamsMetadata> metadataList = ControllerStartupHook.srStreams.getAllServiceStreamsMetadata();
                Map<String, Object> services = new HashMap<>();

                for (StreamsMetadata metadata : metadataList) {
                    if (logger.isDebugEnabled())
                        logger.debug("found one address in the collection " + metadata.host() + ":" + metadata.port());
                    String url = "https://" + metadata.host() + ":" + metadata.port();
                    if (NetUtils.getLocalAddressByDatagram().equals(metadata.host()) && Server.getServerConfig().getHttpsPort() == metadata.port()) {
                        services.putAll(getLocalServices());
                    } else {
                        // remote store through API access.
                        Result<String> resultServices = getControllerServices(exchange, url);
                        if (resultServices.isSuccess()) {
                            services.putAll(JsonMapper.string2Map(resultServices.getResult()));
                        }
                    }
                }
                // get the stale health checks and filter out the un-healthy services
                Map<String, Object> checks = ServicesCheckGetHandler.getClusterHealthChecks(exchange, true);
                exchange.getResponseSender().send(JsonMapper.toJson(filterServiceByCheck(services, checks)));
            }
        } else {
            exchange.getResponseSender().send(JsonMapper.toJson(ControllerStartupHook.services));
        }
    }

    private Map<String, Object> filterServiceByCheck(Map<String, Object> services, Map<String, Object> checks) {
        for (Map.Entry<String, Object> entry : checks.entrySet()) {
            String key = entry.getKey();
            String[] elements = StringUtils.split(key, ":");
            List<Map<String, Object>> instances = (List<Map<String, Object>>) services.get(elements[0]);
            if (instances != null && instances.size() > 0) {
                // only do that if there are instances available for the service.
                instances = ControllerUtil.delService(instances, elements[2], Integer.valueOf(elements[3]));
                // remove the service is number of instances is 0
                if (instances.size() > 0) {
                    services.put(elements[0], instances);
                } else {
                    services.remove(elements[0]);
                }
            }
        }
        return services;
    }

    private Map<String, Object> getLocalServices() {
        Map<String, Object> services = new HashMap<>();
        ReadOnlyKeyValueStore<String, String> serviceStore = ControllerStartupHook.srStreams.getServiceStore();
        KeyValueIterator<String, String> iterator = null;
        long timeout = System.currentTimeMillis() + WAIT_THRESHOLD;
        do {
            if (System.currentTimeMillis() >= timeout) {
                break;
            }
            try {
                iterator = serviceStore.all();
            } catch (InvalidStateStoreException e) {
                try {
                    logger.debug(e.getMessage());
                    Thread.sleep(100L);
                } catch (InterruptedException interruptedException) {
                    logger.error(interruptedException.getMessage(), interruptedException);
                }
            }
        } while (iterator == null);

        while (iterator.hasNext()) {
            KeyValue<String, String> keyValue = iterator.next();
            String key = keyValue.key;
            String value = keyValue.value;
            if (value != null) {
                List nodes = JsonMapper.string2List(value);
                services.put(key, nodes);
            }
        }
        if (logger.isDebugEnabled()) logger.debug("The number of services at local is " + services.size());
        return services;
    }

    /**
     * Get registered controller services from other nodes with exchange and url. The result contains a map of services.
     *
     * @param exchange HttpServerExchange
     * @param url      of the target server
     * @return Result the service map in JSON
     */
    public static Result<String> getControllerServices(HttpServerExchange exchange, String url) {
        return callQueryExchangeUrl(exchange, url);
    }

    public static Result<String> callQueryExchangeUrl(HttpServerExchange exchange, String url) {
        Result<String> result = null;
        try {
            ClientConnection conn = ControllerStartupHook.connCache.get(url);
            if (conn == null || !conn.isOpen()) {
                conn = client.connect(new URI(url), Http2Client.WORKER, client.getDefaultXnioSsl(), Http2Client.BUFFER_POOL, OptionMap.create(UndertowOptions.ENABLE_HTTP2, true)).get();
                ControllerStartupHook.connCache.put(url, conn);
            }
            // Create one CountDownLatch that will be reset in the callback function
            final CountDownLatch latch = new CountDownLatch(1);
            // Create an AtomicReference object to receive ClientResponse from callback function
            final AtomicReference<ClientResponse> reference = new AtomicReference<>();
            String message = "/services?local=true";
            final ClientRequest request = new ClientRequest().setMethod(Methods.GET).setPath(message);
            String token = exchange.getRequestHeaders().getFirst(Headers.AUTHORIZATION);
            if (token != null) request.getRequestHeaders().put(Headers.AUTHORIZATION, token);
            request.getRequestHeaders().put(Headers.HOST, "localhost");
            conn.sendRequest(request, client.createClientCallback(reference, latch));
            latch.await();
            int statusCode = reference.get().getResponseCode();
            String body = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
            if (statusCode != 200) {
                Status status = Config.getInstance().getMapper().readValue(body, Status.class);
                result = Failure.of(status);
            } else result = Success.of(body);
        } catch (Exception e) {
            logger.error("Exception:", e);
            Status status = new Status(GENERIC_EXCEPTION, e.getMessage());
            result = Failure.of(status);
        }
        return result;
    }
}
