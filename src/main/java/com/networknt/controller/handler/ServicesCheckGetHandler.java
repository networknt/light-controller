package com.networknt.controller.handler;

import com.networknt.client.Http2Client;
import com.networknt.config.Config;
import com.networknt.config.JsonMapper;
import com.networknt.controller.ControllerStartupHook;
import com.networknt.handler.LightHttpHandler;
import com.networknt.kafka.common.AvroSerializer;
import com.networknt.monad.Failure;
import com.networknt.monad.Result;
import com.networknt.monad.Success;
import com.networknt.server.Server;
import com.networknt.status.Status;
import com.networknt.utility.NetUtils;
import io.undertow.UndertowOptions;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import org.apache.kafka.streams.KeyValue;
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
 * This is a handler to get all the health check results in the cluster. If a parameter local is true,
 * then, only the current node health checks will be returned. This is only for the cluster mode.
 *
 * There is also another flag stale to indicate only the stale checks will be returned for cluster mode.
 *
 * This endpoint also sync with the health check execution task to ensure that staled services should
 * be removed immediately.
 *
 * The returned result is a map of object with key is the check id and value is the check object.
 *
 * @author Steve Hu
 */
public class ServicesCheckGetHandler implements LightHttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(ServicesCheckGetHandler.class);
    static Http2Client client = Http2Client.getInstance();
    static final String GENERIC_EXCEPTION = "ERR10014";

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, "application/json");
        exchange.setStatusCode(200);
        if(ControllerStartupHook.config.isClusterMode()) {
            // get query parameter for the local indicator.
            boolean local = false;
            Deque<String> localDeque = exchange.getQueryParameters().get("local");
            if(localDeque != null && !localDeque.isEmpty()) local = true;
            boolean stale = false;
            Deque<String> staleDeque = exchange.getQueryParameters().get("stale");
            if(staleDeque != null && !staleDeque.isEmpty()) stale = true;
            if(local) {
                exchange.getResponseSender().send(JsonMapper.toJson(getLocalChecks(stale)));
            } else {
                exchange.getResponseSender().send(JsonMapper.toJson(getClusterHealthChecks(exchange, stale)));
            }

        } else {
            exchange.getResponseSender().send(JsonMapper.toJson(ControllerStartupHook.checks));
        }
    }

    public static Map<String, Object> getClusterHealthChecks(HttpServerExchange exchange, boolean stale) throws Exception {
        Collection<StreamsMetadata> metadataList = ControllerStartupHook.hcStreams.getAllHealthStreamsMetadata();
        Map<String, Object> checks;
        if(stale) {
            checks = new TreeMap<>();
        } else {
            checks = new HashMap<>();
        }
        for (StreamsMetadata metadata : metadataList) {
            if (logger.isDebugEnabled()) logger.debug("found one address in the collection " + metadata.host() + ":" + metadata.port());
            String url = "https://" + metadata.host() + ":" + metadata.port();
            if (NetUtils.getLocalAddressByDatagram().equals(metadata.host()) && Server.getServerConfig().getHttpsPort() == metadata.port()) {
                checks.putAll(getLocalChecks(stale));
            } else {
                // remote store through API access.
                Result<String> resultChecks = getControllerChecks(exchange, url, stale);
                if (resultChecks.isSuccess()) {
                    checks.putAll(JsonMapper.string2Map(resultChecks.getResult()));
                }
            }
        }
        return checks;
    }

    private static Map<String, Object> getLocalChecks(boolean stale) throws Exception {
        Map<String, Object> checks = new HashMap<>();
        // local store access.
        ReadOnlyKeyValueStore<String, String> healthStore = ControllerStartupHook.hcStreams.getHealthStore();
        KeyValueIterator<String, String> iterator = healthStore.all();
        while(iterator.hasNext()) {
            KeyValue<String, String> keyValue = iterator.next();
            String key = keyValue.key;
            String value = keyValue.value;
            if(value != null) {
                Map<String, Object> check = JsonMapper.string2Map(value);
                if(stale) {
                    // only put the stale check into the map.
                    if(isStaleCheck(check)) checks.put(key, check);
                } else {
                    // only put the non-stale check into the map.
                    if(!isStaleCheck(check)) checks.put(key, check);
                }
            }
        }
        if(logger.isDebugEnabled()) logger.debug("The number of checks at local is " + checks.size());
        return checks;
    }

    private static boolean isStaleCheck(Map<String, Object> checkMap) throws Exception {
        boolean stale = false;
        long lastExecuteTimestamp = Long.valueOf((String)checkMap.get("lastExecuteTimestamp"));
        long deregisterCriticalServiceAfter = Long.valueOf((String)checkMap.get("deregisterCriticalServiceAfter"));
        if(System.currentTimeMillis() - lastExecuteTimestamp > deregisterCriticalServiceAfter) {
            // it has been a long time that heath check status is not update. That means the health check event for missed.
            stale = true;
            // de-register the service when health check task is dead.
            String serviceId = (String)checkMap.get("serviceId");
            String tag = (String)checkMap.get("tag");
            String protocol = (String)checkMap.get("protocol");
            String address = (String)checkMap.get("address");
            String executeInterval = (String)checkMap.get("executeInterval");
            int port = Integer.valueOf((String)checkMap.get("port"));
            String key = tag == null ? serviceId : serviceId + "|" + tag;
            AvroSerializer serializer = new AvroSerializer();
            ServicesDeleteHandler.pushDeregisterEvent(serializer, key, serviceId, protocol, tag, address, port);
            // the above statement will remove the service registry; however, the health check store is no cleaned and this
            // piece code will be running again and again. To avoid it, we can remove the health check task here.
            ServicesDeleteHandler.pushDeleteTaskDefinition(serializer, key, protocol, address, port, executeInterval);
        }
        return stale;
    }

    /**
     * Get health checks from other nodes with exchange and url. The result contains a map of checks.
     *
     * @param exchange HttpServerExchange
     * @param url of the target server
     * @return Result the check map in JSON
     */
    public static Result<String> getControllerChecks(HttpServerExchange exchange, String url, boolean stale) {
        return callQueryExchangeUrl(exchange, url, stale);
    }

    public static Result<String> callQueryExchangeUrl(HttpServerExchange exchange, String url, boolean stale) {
        Result<String> result = null;
        try {
            ClientConnection conn = ControllerStartupHook.connCache.get(url);
            if(conn == null || !conn.isOpen()) {
                conn = client.connect(new URI(url), Http2Client.WORKER, client.getDefaultXnioSsl(), Http2Client.BUFFER_POOL, OptionMap.create(UndertowOptions.ENABLE_HTTP2, true)).get();
                ControllerStartupHook.connCache.put(url, conn);
            }
            // Create one CountDownLatch that will be reset in the callback function
            final CountDownLatch latch = new CountDownLatch(1);
            // Create an AtomicReference object to receive ClientResponse from callback function
            final AtomicReference<ClientResponse> reference = new AtomicReference<>();
            String path = "/services/check?local=true";
            if(stale) path = path + "&stale=true";
            final ClientRequest request = new ClientRequest().setMethod(Methods.GET).setPath(path);
            String token = exchange.getRequestHeaders().getFirst(Headers.AUTHORIZATION);
            if(token != null) request.getRequestHeaders().put(Headers.AUTHORIZATION, token);
            request.getRequestHeaders().put(Headers.HOST, "localhost");
            conn.sendRequest(request, client.createClientCallback(reference, latch));
            latch.await();
            int statusCode = reference.get().getResponseCode();
            String body = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
            if(statusCode != 200) {
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
