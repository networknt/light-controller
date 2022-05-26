package com.networknt.controller.handler;

import static com.networknt.controller.ControllerConstants.ADDRESS;
import static com.networknt.controller.ControllerConstants.PORT;
import static com.networknt.controller.ControllerConstants.PROTOCOL;

import java.util.Map;

import com.networknt.body.BodyHandler;
import com.networknt.controller.ControllerClient;
import com.networknt.controller.model.ServerShutdownRequest;
import com.networknt.handler.LightHttpHandler;
import com.networknt.http.MediaType;
import com.networknt.monad.Result;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

/**
 * 
 * This is the post request to shutdown the target service instance to force a restart.
 * 
 * 
*/
public class ServicesShutdownPostHandler implements LightHttpHandler {

    public ServicesShutdownPostHandler () {
    }

    
    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
             
        Map<String, Object> bodyMap = (Map<String, Object>) exchange.getAttachment(BodyHandler.REQUEST_BODY);

		String protocol = (String) bodyMap.get(PROTOCOL);
		String address = (String) bodyMap.get(ADDRESS);
		Integer port = (Integer) bodyMap.get(PORT);

		if (logger.isTraceEnabled())
			logger.trace("protocol = " + protocol + " address = " + address + " port = " + port);
		
		ServerShutdownRequest request = new ServerShutdownRequest();
		request.setProtocol(protocol);
		request.setPort(port);
		request.setAddress(address);

		Result<String> result = ControllerClient.shutdownService(request);
		exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
		
        if (result.isSuccess()) {
            exchange.setStatusCode(200);
            exchange.getResponseSender().send(result.getResult());
        } else {
            exchange.setStatusCode(result.getError().getStatusCode());
            exchange.getResponseSender().send(result.getError().toString());
        }
    }
}
