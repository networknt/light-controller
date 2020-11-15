package com.networknt.controller.handler;

import com.networknt.body.BodyHandler;
import com.networknt.config.Config;
import com.networknt.config.JsonMapper;
import com.networknt.controller.ControllerClient;
import com.networknt.controller.ControllerConfig;
import com.networknt.controller.ControllerStartupHook;
import com.networknt.controller.model.Check;
import com.networknt.handler.LightHttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Register a service to the controller to indicate it is alive. It is normally called
 * during the service startup phase.
 *
 * @author Steve Hu
 */
public class ServicesPostHandler implements LightHttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(ServicesPostHandler.class);
    private static final String SUC10200 = "SUC10200";

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        Map<String, Object> body = (Map<String, Object>)exchange.getAttachment(BodyHandler.REQUEST_BODY);
        String serviceId = (String)body.get("serviceId");
        String tag = (String)body.get("tag");
        String address = (String)body.get("address");
        int port = (Integer)body.get("port");
        if(logger.isDebugEnabled()) logger.debug("serviceId = " + serviceId + " tag = " + tag + " address = " + address + " port = " + port);
        Map<String, Object> nodeMap = new ConcurrentHashMap<>();
        nodeMap.put("address", address);
        nodeMap.put("port", port);

        if(tag != null) {
            List nodes = (List)ControllerStartupHook.services.get(serviceId + "|" + tag);
            ControllerStartupHook.services.put(serviceId + "|" + tag, addService(nodes, nodeMap));
        } else {
            List nodes = (List)ControllerStartupHook.services.get(serviceId);
            ControllerStartupHook.services.put(serviceId, addService(nodes, nodeMap));
        }
        // save the check Object in another map for background process to perform check periodically.
        Check check = JsonMapper.objectMapper.convertValue(body.get("check"), Check.class);
        ControllerStartupHook.checks.put(check.getId(), check);
        // now try to get server info from by accessing the endpoint with a URL constructed with address and port
        // we assume that the server is running with https and it can verify the bootstrap token from the controller.
        String info = ControllerClient.getServerInfo(address, port);
        if(info !=  null) {
            ControllerStartupHook.infos.put(address + ":" + port, info);
        }
        setExchangeStatus(exchange, SUC10200);
    }

    private List addService(List nodes, Map<String, Object> nodeMap) {
        if(nodes == null) {
            nodes = new ArrayList<>();
            nodes.add(nodeMap);
        } else {
            // delete the nodeMap if it is already there before adding it.
            String address = (String)nodeMap.get("address");
            int port = (Integer)nodeMap.get("port");
            for(Iterator<Map<String, Object>> iter = nodes.iterator(); iter.hasNext(); ) {
                Map<String, Object> map = iter.next();
                String a = (String)map.get("address");
                int p = (Integer)map.get("port");
                if (address.equals(a) && port == p)
                    iter.remove();
            }
            nodes.add(nodeMap);
        }
        return nodes;
    }
}
