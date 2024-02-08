package com.networknt.controller.handler;

import static com.networknt.controller.ControllerConstants.ADDRESS;
import static com.networknt.controller.ControllerConstants.PORT;
import static com.networknt.controller.ControllerConstants.PROTOCOL;

import java.util.List;
import java.util.Map;

import com.networknt.body.BodyHandler;
import com.networknt.controller.ControllerClient;
import com.networknt.handler.LightHttpHandler;
import com.networknt.http.MediaType;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

/**
 *
 * This is the endpoint to reload the configuration of all or a list of selected
 * modules on a target service instance.
 *
 */
public class ServicesModulePostHandler implements LightHttpHandler {

	@Override
	public void handleRequest(HttpServerExchange exchange) throws Exception {
		Map<String, Object> bodyMap = (Map<String, Object>) exchange.getAttachment(BodyHandler.REQUEST_BODY);

		String protocol = (String) bodyMap.get(PROTOCOL);
		String address = (String) bodyMap.get(ADDRESS);
		Integer port = (Integer) bodyMap.get(PORT);

		List modules = (List) bodyMap.get("modules");
		if (logger.isTraceEnabled())
			logger.trace("protocol = " + protocol + " address = " + address + " port = " + port);

		String result = ControllerClient.reloadModuleConfig(protocol, address, port, modules);
		exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
		exchange.setStatusCode(200);
		exchange.getResponseSender().send(result);

	}
}
