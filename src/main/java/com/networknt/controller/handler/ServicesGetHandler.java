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
import org.apache.kafka.streams.StreamsMetadata;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
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

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, "application/json");
        exchange.setStatusCode(200);
        if (ControllerStartupHook.config.isClusterMode()) {
            if(logger.isTraceEnabled()) logger.trace("Running in cluster mode");
            // get query parameter for the local indicator.
            boolean local = false;
            Deque<String> localDeque = exchange.getQueryParameters().get("local");
            if (localDeque != null && !localDeque.isEmpty()) local = true;
            if (local) {
                if(logger.isTraceEnabled()) logger.trace("Local call, get the data from the current instance");
                exchange.getResponseSender().send(JsonMapper.toJson(getLocalServices()));
            } else {
                if(logger.isTraceEnabled()) logger.trace("Called by client, iterate all instances");
                Collection<StreamsMetadata> metadataList = ControllerStartupHook.srStreams.getAllServiceStreamsMetadata();
                Map<String, Object> services = new HashMap<>();
                if(logger.isTraceEnabled()) logger.trace("found {} instances", metadataList.size());
                for (StreamsMetadata metadata : metadataList) {
                    if (logger.isDebugEnabled())
                        logger.debug("Found one address in the collection " + metadata.host() + ":" + metadata.port());
                    String url = "https://" + metadata.host() + ":" + metadata.port();
                    if (NetUtils.getLocalAddressByDatagram().equals(metadata.host()) && Server.getServerConfig().getHttpsPort() == metadata.port()) {
                        if(logger.isTraceEnabled()) logger.trace("On the same host. Get the local service");
                        services.putAll(getLocalServices());
                    } else {
                        // remote store through API access.
                        if(logger.isTraceEnabled()) logger.trace("Get services from remote store with url {}", url);
                        Result<String> resultServices = getControllerServices(exchange, url);
                        if (resultServices.isSuccess()) {
                            if(logger.isTraceEnabled()) logger.trace("Success result string length = " + resultServices.getResult().length());
                            services.putAll(JsonMapper.string2Map(resultServices.getResult()));
                        } else {
                            logger.error("Failure result = " + resultServices.getError());
                        }
                    }
                }
                // get the stale health checks and filter out the un-healthy services
                // TODO this action is very slow on 5 instance cluster with a lot of registered services. skip it for now.
                ///Map<String, Object> checks = ServicesCheckGetHandler.getClusterHealthChecks(exchange, true);
                ///if(logger.isTraceEnabled()) logger.trace("Get stale health checks size = " + checks.size());
                ///exchange.getResponseSender().send(JsonMapper.toJson(filterServiceByCheck(services, checks)));
                exchange.getResponseSender().send(JsonMapper.toJson(services));
            }
        } else {
            if(logger.isTraceEnabled()) logger.trace("Not running in cluster mode. Get services from ControllerStartupHook");
            exchange.getResponseSender().send(JsonMapper.toJson(ControllerStartupHook.services));
        }
    }

    private Map<String, Object> filterServiceByCheck(Map<String, Object> services, Map<String, Object> checks) {
        if(logger.isTraceEnabled()) logger.trace("Before filter services size = " + services.size());
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
        if(logger.isTraceEnabled()) logger.trace("After filter services size = " + services.size());
        return services;
    }

    private Map<String, Object> getLocalServices() {
        Map<String, Object> services = new HashMap<>();
        ReadOnlyKeyValueStore<String, String> serviceStore = ControllerStartupHook.srStreams.getServiceStore();
        if(logger.isTraceEnabled()) logger.trace("Got serviceStore from the srStreams");
        try(KeyValueIterator<String, String> iterator = (KeyValueIterator<String, String>) ControllerStartupHook.srStreams.getAllKafkaValue(serviceStore)) {
            if(logger.isTraceEnabled()) logger.trace("Start iterate KeyValue pairs");
            while (iterator.hasNext()) {
                KeyValue<String, String> keyValue = iterator.next();
                String key = keyValue.key;
                String value = keyValue.value;
                if (value != null) {
                    List nodes = JsonMapper.string2List(value);
                    services.put(key, nodes);
                }
            }
        }
        if (logger.isTraceEnabled()) logger.trace("The number of services at local is " + services.size());
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
                if(logger.isTraceEnabled()) logger.trace("Connection from catch is null or not open for url" + url);
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
            if(logger.isTraceEnabled() && token != null) logger.trace("Got a token from the incoming request token size = " + token.length() + "last chars = " + token.substring(token.length() - 10));
            if (token != null) request.getRequestHeaders().put(Headers.AUTHORIZATION, token);
            request.getRequestHeaders().put(Headers.HOST, "localhost");
            conn.sendRequest(request, client.createClientCallback(reference, latch));
            latch.await();
            int statusCode = reference.get().getResponseCode();
            String body = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
            if (statusCode != 200) {
                if(logger.isTraceEnabled()) logger.trace("Error statusCode = {} and body = {}", statusCode, body);
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
