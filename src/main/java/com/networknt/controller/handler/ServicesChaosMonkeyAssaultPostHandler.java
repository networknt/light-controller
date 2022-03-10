package com.networknt.controller.handler;

import com.networknt.controller.ControllerChaosMonkey;
import com.networknt.body.BodyHandler;
import com.networknt.handler.LightHttpHandler;
import com.networknt.http.MediaType;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static com.networknt.controller.ControllerConstants.*;

public class ServicesChaosMonkeyAssaultPostHandler implements LightHttpHandler {

    private static final Logger logger = LoggerFactory.getLogger(ServicesChaosMonkeyAssaultPostHandler.class);
    private static final String ASSAULT_TYPE_PARAM = "assaultType";
    private static final String REQUESTS_PARAM = "requests";
    private static final String ENDPOINT_PARAM = "endpoint";

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {

        // get data from body
        Map<String, Object> body = (Map<String, Object>) exchange.getAttachment(BodyHandler.REQUEST_BODY);
        String protocol = body.getOrDefault(PROTOCOL, "null").toString();
        String address = body.getOrDefault(ADDRESS, "null").toString();
        int port = Integer.parseInt(body.getOrDefault(PORT, "0").toString());

        String assaultType = body.getOrDefault(ASSAULT_TYPE_PARAM, "null").toString();
        int reqCount = Integer.parseInt(body.getOrDefault(REQUESTS_PARAM, "0").toString());
        String endpointTest = body.getOrDefault(ENDPOINT_PARAM, "null").toString();

        String res = ControllerChaosMonkey.initChaosMonkeyAssault(assaultType, address, port, protocol, endpointTest, reqCount);

        exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        exchange.setStatusCode(200);
        exchange.getResponseSender().send(res);

    }

}
