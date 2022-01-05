package com.networknt.controller.handler;

import com.networknt.controller.ControllerChaosMonkey;
import com.networknt.controller.model.ChaosMonkeyAssaultConfigPost;
import com.networknt.body.BodyHandler;
import com.networknt.handler.LightHttpHandler;
import com.networknt.http.MediaType;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;

import static com.networknt.controller.ControllerConstants.*;

public class ServicesChaosMonkeyPostHandler implements LightHttpHandler {

    private static final Logger logger = LoggerFactory.getLogger(ServicesChaosMonkeyPostHandler.class);
    private static final String ASSAULT_TYPE_PARAM = "assaultType";
    private static final String ASSAULT_CONFIG_PARAM = "assaultType";

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        // get data from body
        Map<String, Object> body = (Map<String, Object>)exchange.getAttachment(BodyHandler.REQUEST_BODY);
        String protocol = body.getOrDefault(PROTOCOL, "null").toString();
        String address = body.getOrDefault(ADDRESS, "null").toString();
        int port = Integer.parseInt(body.getOrDefault(PORT, "null").toString());

        String assaultType = body.getOrDefault(ASSAULT_TYPE_PARAM, "null").toString();

        Object config = body.getOrDefault(ASSAULT_CONFIG_PARAM, "null");

        // check if node exists
        ChaosMonkeyAssaultConfigPost<?> postBody = ControllerChaosMonkey.getChaosMonkeyAssaultConfigPostBody(assaultType, address, port, protocol, config);

        // query chaos monkey handlers from service
        if(postBody != null) {
            String res = ControllerChaosMonkey.postChaosMonkeyAssault(postBody);
            exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            exchange.setStatusCode(200);
            exchange.getResponseSender().send(res);
        } else {
            exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            exchange.setStatusCode(404);
        }
    }
}
