package com.networknt.controller.handler;

import com.networknt.controller.ControllerChaosMonkey;
import com.networknt.handler.LightHttpHandler;
import com.networknt.http.MediaType;
import com.networknt.monad.Result;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static com.networknt.controller.ControllerConstants.*;

/**
 * This is the handler to retrieve all the chaos monkey meta information from the target server for update
 * on the portal-view. If the target server doesn't have the chaos monkey handler implemented, an 404 will
 * be returned to the portal-view so that the Chaos Monkey form won't be shown up.
 *
 */
public class ServicesChaosMonkeyGetHandler implements LightHttpHandler {

    private static final Logger logger = LoggerFactory.getLogger(ServicesChaosMonkeyGetHandler.class);

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {

        // get all params
        String protocol = exchange.getQueryParameters().get(PROTOCOL).getFirst();
        String address = exchange.getQueryParameters().get(ADDRESS).getFirst();
        int port = Integer.parseInt(exchange.getQueryParameters().get(PORT).getFirst());

        Result<String> res = ControllerChaosMonkey.getChaosMonkeyInfo(protocol, address, port);
        exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        if(res.isSuccess()) {
            exchange.setStatusCode(200);
            exchange.getResponseSender().send(res.getResult());
        } else {
            exchange.setStatusCode(res.getError().getStatusCode());
            exchange.getResponseSender().send(res.getError().toString());
        }
    }

}
