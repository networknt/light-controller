package com.networknt.controller.handler;

import com.networknt.body.BodyHandler;
import com.networknt.config.Config;
import com.networknt.handler.LightHttpHandler;
import com.networknt.http.HttpMethod;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import com.networknt.controller.model.ServerShutdownRequest;
import java.util.Deque;
import java.util.Map;

/**
For more information on how to write business handlers, please check the link below.
https://doc.networknt.com/development/business-handler/rest/
*/
public class ServicesShutdownPostHandler implements LightHttpHandler {

    public ServicesShutdownPostHandler () {
    }

    
    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        HeaderMap requestHeaders = exchange.getRequestHeaders();
        Map<String, Deque<String>> queryParameters = exchange.getQueryParameters();
        Map<String, Deque<String>> pathParameters = exchange.getPathParameters();
        HttpMethod httpMethod = HttpMethod.resolve(exchange.getRequestMethod().toString());
        Map<String, Object> bodyMap = (Map<String, Object>)exchange.getAttachment(BodyHandler.REQUEST_BODY);
        ServerShutdownRequest requestBody = Config.getInstance().getMapper().convertValue(bodyMap, ServerShutdownRequest.class);
        exchange.setStatusCode(200);
        exchange.getResponseSender().send("");
    }
}
