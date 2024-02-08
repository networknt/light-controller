package com.networknt.controller.handler;

import static com.networknt.controller.ControllerConstants.ADDRESS;
import static com.networknt.controller.ControllerConstants.PORT;
import static com.networknt.controller.ControllerConstants.PROTOCOL;

import com.networknt.controller.ControllerClient;
import com.networknt.handler.LightHttpHandler;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;

/***
 * This endpoint is used to get the registered module, include middleware
 * handlers, server, client etc
 *
 *
 */
public class ServicesModuleGetHandler implements LightHttpHandler {

	@Override
	public void handleRequest(HttpServerExchange exchange) throws Exception {

		String protocol = exchange.getQueryParameters().get(PROTOCOL).getFirst();
		String address = exchange.getQueryParameters().get(ADDRESS).getFirst();
		String port = exchange.getQueryParameters().get(PORT).getFirst();

		if (logger.isTraceEnabled())
			logger.trace("protocol = " + protocol + " address = " + address + " port = " + port);
		String result = ControllerClient.getModuleList(protocol, address, port);
		exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
		exchange.setStatusCode(200);
		exchange.getResponseSender().send(result);
	}
}
