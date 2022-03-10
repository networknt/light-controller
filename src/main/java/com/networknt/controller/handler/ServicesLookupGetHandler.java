package com.networknt.controller.handler;

import com.networknt.client.Http2Client;
import com.networknt.config.Config;
import com.networknt.controller.ControllerStartupHook;
import com.networknt.config.JsonMapper;
import com.networknt.controller.ControllerUtil;
import com.networknt.handler.LightHttpHandler;
import com.networknt.http.MediaType;
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
import org.apache.kafka.streams.KeyQueryMetadata;
import org.apache.kafka.streams.state.HostInfo;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.OptionMap;

import java.net.URI;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static com.networknt.controller.ControllerConstants.*;

/**
 * Get all healthy nodes for a registered service from the controller. This endpoint is used for
 * service discovery from the portal-registry. A serviceId query parameter is required and a tag
 * parameter is optional.
 * <p>
 * For the cluster mode, it will filter out all the services that health check is stale so that
 * only live instances will be returned.
 *
 * @author Steve Hu
 */
public class ServicesLookupGetHandler implements LightHttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(ServicesLookupGetHandler.class);
    static Http2Client client = Http2Client.getInstance();
    private static final String OBJECT_NOT_FOUND = "ERR11637";
    private static final String GENERIC_EXCEPTION = "ERR10014";
    // prevent access the health check store too often. One time per minute maximum.
    static long checkCachePeriod = 60000;
    // we cached the stale checks as this won't happen very often, and we only want to check it
    // periodically. Like last check was over 1 minute ago.
    private SortedMap<String, Object> checks = null;
    private long lastLoadChecks = System.currentTimeMillis();

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        String serviceId = exchange.getQueryParameters().get(SERVICE_ID).getFirst();
        String tag = null;
        Deque<String> tagDeque = exchange.getQueryParameters().get(TAG);
        if (tagDeque != null && !tagDeque.isEmpty()) 
          tag = tagDeque.getFirst();
          
        String key = tag == null ? serviceId : serviceId + "|" + tag;
          
        if (logger.isDebugEnabled()) logger.debug("key = " + key + " serviceId = " + serviceId + " tag = " + tag);
          
        exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        
          if (ControllerStartupHook.config.isClusterMode()) {
            // get the stale health checks and potentially filter out the un-healthy services in the result.
            if (checks == null || System.currentTimeMillis() - lastLoadChecks > checkCachePeriod) {
                checks = (SortedMap<String, Object>) ServicesCheckGetHandler.getClusterHealthChecks(exchange, true);
                lastLoadChecks = System.currentTimeMillis();
            }
            
            ReadOnlyKeyValueStore<String, String> serviceStore = ControllerStartupHook.srStreams.getServiceStore();
            String data = (String) ControllerStartupHook.srStreams.getKafkaValueByKey(serviceStore, key);
            
            if (data != null) {
                
              if (checks.size() > 0) {
                    SortedMap<String, Object> tailMap = checks.tailMap(key);
                    if (!tailMap.isEmpty() && tailMap.firstKey().startsWith(key)) {
                        List<Map<String, Object>> instances = filterInstanceByCheck(JsonMapper.string2List(data), checks);
                        exchange.getResponseSender().send(JsonMapper.toJson(instances));
                    } else {
                        exchange.getResponseSender().send(data);
                    }

                } else {
                    exchange.getResponseSender().send(data);
                }

            } else {
                KeyQueryMetadata metadata = ControllerStartupHook.srStreams.getServiceStreamsMetadata(key);
                HostInfo hostInfo = metadata.activeHost();
                if (logger.isDebugEnabled())
                    logger.debug("found address in another instance " + hostInfo.host() + ":" + hostInfo.port());
                String url = "https://" + hostInfo.host() + ":" + hostInfo.port();
                if (NetUtils.getLocalAddressByDatagram().equals(hostInfo.host()) && Server.getServerConfig().getHttpsPort() == hostInfo.port()) {
                    logger.error("******Kafka returns the same instance!");
                    setExchangeStatus(exchange, OBJECT_NOT_FOUND, "service registry", key);
                    return;
                } else {
                    Result<String> resultEntity = getServiceRegistry(exchange, url, serviceId, tag);
                    if (resultEntity.isSuccess()) {

                        if (checks.size() > 0) {
                            SortedMap<String, Object> tailMap = checks.tailMap(key);
                            if (!tailMap.isEmpty() && tailMap.firstKey().startsWith(key)) {
                                List<Map<String, Object>> instances = filterInstanceByCheck(JsonMapper.string2List(resultEntity.getResult()), checks);
                                exchange.getResponseSender().send(JsonMapper.toJson(instances));
                            } else {
                                exchange.getResponseSender().send(resultEntity.getResult());
                            }
                        } else {
                            exchange.getResponseSender().send(resultEntity.getResult());
                        }
                        return;
                    }
                }
                setExchangeStatus(exchange, OBJECT_NOT_FOUND, "service registry", key);
            }

        } else {
            List<?> nodes = (List<?>) ControllerStartupHook.services.get(key);
            exchange.getResponseSender().send(JsonMapper.toJson(nodes));
        }
    }


    private List<Map<String, Object>> filterInstanceByCheck(List<Map<String, Object>> instances, Map<String, Object> checks) {
        for (Map.Entry<String, Object> entry : checks.entrySet()) {
            String key = entry.getKey();
            String[] elements = StringUtils.split(key, ":");

            if (instances != null && instances.size() > 0) {
                // only do that if there are instances available for the service.
                instances = ControllerUtil.delService(instances, elements[2], Integer.valueOf(elements[3]));
            }
        }
        return instances;
    }

    /**
     * Get registered controller services from other nodes with exchange and url. The result contains a map of services.
     *
     * @param exchange  HttpServerExchange
     * @param url       of the target server
     * @param serviceId service id
     * @param tag       service tag
     * @return Result the service map in JSON
     */
    public static Result<String> getServiceRegistry(HttpServerExchange exchange, String url, String serviceId, String tag) {
        return callQueryExchangeUrl(exchange, url, serviceId, tag);
    }

    public static Result<String> callQueryExchangeUrl(HttpServerExchange exchange, String url, String serviceId, String tag) {
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
            String path = "/services/lookup?serviceId=" + serviceId;

            if (tag != null)
                path = path + "&tag=" + tag;

            final ClientRequest request = new ClientRequest().setMethod(Methods.GET).setPath(path);
            String token = exchange.getRequestHeaders().getFirst(Headers.AUTHORIZATION);

            if (token != null)
                request.getRequestHeaders().put(Headers.AUTHORIZATION, token);

            request.getRequestHeaders().put(Headers.HOST, "localhost");
            conn.sendRequest(request, client.createClientCallback(reference, latch));
            latch.await();

            int statusCode = reference.get().getResponseCode();
            String body = reference.get().getAttachment(Http2Client.RESPONSE_BODY);

            if (statusCode != 200) {
                Status status = Config.getInstance().getMapper().readValue(body, Status.class);
                result = Failure.of(status);
            } else
                result = Success.of(body);

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            Status status = new Status(GENERIC_EXCEPTION, e.getMessage());
            result = Failure.of(status);
        }
        return result;
    }

}
