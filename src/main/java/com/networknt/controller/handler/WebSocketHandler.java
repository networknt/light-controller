package com.networknt.controller.handler;

import com.networknt.config.JsonMapper;
import com.networknt.controller.ServerWebSocketClient;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.core.*;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class WebSocketHandler implements WebSocketConnectionCallback {
    static final Logger logger = LoggerFactory.getLogger(WebSocketHandler.class);

    // a map from channel to a ServerWebSocketClient mapping. From the message channel, you can identify a client.
    public static final Map<WebSocketChannel, ServerWebSocketClient> clients = new ConcurrentHashMap<>();

    // a map from subscription serviceId|tag to a list of subscribed clients. Used to send to update to these clients
    // if the nodes for the serviceId|tag is changed.
    public static final Map<String, List<ServerWebSocketClient>> subscriptions = new ConcurrentHashMap<>();

    @Override
    public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel channel) {
        ServerWebSocketClient client = new ServerWebSocketClient(channel);
        clients.put(channel, client);
        if(logger.isDebugEnabled()) logger.debug("A new channel is opened and added to the clients for " + channel.toString());
        channel.getReceiveSetter().set(new AbstractReceiveListener() {
            @Override
            protected void onFullTextMessage(WebSocketChannel channel, BufferedTextMessage message) {
                final String messageData = message.getData();
                if(logger.isDebugEnabled()) logger.debug("subscription message received " + messageData);
                // The format of the subscription is https://localhost:443/com.networknt.ac-1.0.0?environment=test1&
                try {
                    URL url = new URL(messageData);
                    String serviceId = url.getPath().substring(1);
                    String query = url.getQuery();
                    String tag = null;
                    if(query.indexOf("=") > 0) {
                        tag = query.substring(query.indexOf("=") + 1, query.length() -1);
                    }
                    String key = tag == null ? serviceId : serviceId + "|" + tag;
                    List<ServerWebSocketClient> list = subscriptions.get(key);
                    if(logger.isDebugEnabled()) logger.debug("key = " +  key + " subscription list size = " + (list == null ? 0 : list.size()));
                    if(list != null) {
                        list.add(clients.get(channel));
                    } else {
                        list = new ArrayList<>();
                        list.add(clients.get(channel));
                    }
                    subscriptions.put(key, list);
                } catch (MalformedURLException e) {
                    logger.error("MalformatURLException", e);
                }
            }

            @Override
            protected void onError(WebSocketChannel webSocketChannel, Throwable error) {
                logger.error("WebSocket Server error:", error);
            }

            @Override
            protected void onClose(WebSocketChannel clientChannel, StreamSourceFrameChannel streamSourceChannel) throws IOException {
                logger.info(clientChannel.toString() + " disconnected");
            }
        });
        channel.resumeReceives();
    }

    public static void sendUpdatedNodes(String key, List nodes) {
        // find a list of clients who subscribe the key.
        List<ServerWebSocketClient> list = subscriptions.get(key);
        if(list != null) {
            Map<String, List> nodeMap = new HashMap<>();
            nodeMap.put(key, nodes);
            Iterator<ServerWebSocketClient> iterator = list.iterator();
            while(iterator.hasNext()) {
                ServerWebSocketClient client = iterator.next();
                boolean sent = client.send(JsonMapper.toJson(nodeMap));
                if(logger.isDebugEnabled()) logger.debug("nodes changed for key " + key + " values = " + nodes);
                if(!sent) {
                    // the client is gone and it should be removed from the subscription list.
                    if(logger.isDebugEnabled()) logger.debug("client is closed, remove from the list");
                    iterator.remove();
                }
            }
        }
    }
}
