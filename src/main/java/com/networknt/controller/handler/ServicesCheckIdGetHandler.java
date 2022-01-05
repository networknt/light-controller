package com.networknt.controller.handler;

import com.networknt.controller.ControllerConstants;
import com.networknt.controller.ControllerStartupHook;
import com.networknt.client.Http2Client;
import com.networknt.config.Config;
import com.networknt.config.JsonMapper;
import com.networknt.handler.LightHttpHandler;
import com.networknt.http.MediaType;
import com.networknt.monad.Failure;
import com.networknt.monad.Result;
import com.networknt.monad.Success;
import com.networknt.scheduler.TaskDefinitionKey;
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
import org.apache.kafka.streams.KeyQueryMetadata;
import org.apache.kafka.streams.state.HostInfo;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.OptionMap;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Query server info based on the serviceId and tag. There might be multiple instances that separated
 * by IP and Port. List all of them in an array of service info objects returned from the /server/info
 * endpoint.
 *
 * @author Steve Hu
 */
public class ServicesCheckIdGetHandler implements LightHttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(ServicesCheckIdGetHandler.class);
    static Http2Client client = Http2Client.getInstance();
    private static final String OBJECT_NOT_FOUND = "ERR11637";
    private static final String GENERIC_EXCEPTION = "ERR10014";

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        String id = exchange.getQueryParameters().get("id").getFirst();
        if(logger.isTraceEnabled()) logger.trace("id = " + id);
        exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        if(ControllerStartupHook.config.isClusterMode()) {
            // get from the Kafka Streams store.
            ReadOnlyKeyValueStore<String, String> healthStore = ControllerStartupHook.hcStreams.getHealthStore();
            String data = healthStore.get(id);
            if(data != null) {
                exchange.getResponseSender().send(data);
            } else {
                TaskDefinitionKey taskDefinitionKey = TaskDefinitionKey.newBuilder()
                        .setName(id)
                        .setHost(ControllerConstants.HOST)
                        .build();
                KeyQueryMetadata metadata = ControllerStartupHook.hcStreams.getHealthStreamsMetadata(taskDefinitionKey);
                HostInfo hostInfo = metadata.activeHost();
                if(logger.isDebugEnabled()) logger.debug("found address in another instance " + hostInfo.host() + ":" + hostInfo.port());
                String url = "https://" + hostInfo.host() + ":" + hostInfo.port();
                if(NetUtils.getLocalAddressByDatagram().equals(hostInfo.host()) && Server.getServerConfig().getHttpsPort() == hostInfo.port()) {
                    logger.error("******Kafka returns the same instance!");
                    setExchangeStatus(exchange, OBJECT_NOT_FOUND, "health check", id);
                    return;
                } else {
                    Result<String> resultEntity = getHealthCheck(exchange, url, id);
                    if (resultEntity.isSuccess()) {
                        exchange.getResponseSender().send(resultEntity.getResult());
                        return;
                    }
                }
                setExchangeStatus(exchange, OBJECT_NOT_FOUND, "health check", id);
            }
        } else {
            exchange.getResponseSender().send(JsonMapper.toJson(ControllerStartupHook.checks.get(id)));
        }
    }

    /**
     * Get registered controller services from other nodes with exchange and url. The result contains a map of services.
     *
     * @param exchange HttpServerExchange
     * @param url of the target server
     * @param id of check id
     * @return Result the service map in JSON
     */
    public static Result<String> getHealthCheck(HttpServerExchange exchange, String url, String id) {
        return callQueryExchangeUrl(exchange, url, id);
    }

    public static Result<String> callQueryExchangeUrl(HttpServerExchange exchange, String url, String id) {
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
            // encode url as there is a vertical bar between the serviceId and the tag.
            String path = "/services/check/" + URLEncoder.encode(id, StandardCharsets.UTF_8.toString());
            if(logger.isTraceEnabled()) logger.trace("encoded url = " + path);
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
