package com.networknt.controller.handler;

import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.core.*;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class WebSocketHandler implements WebSocketConnectionCallback {
    static final Logger logger = LoggerFactory.getLogger(WebSocketHandler.class);

    public WebSocketHandler() {
    }

    @Override
    public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel channel) {
        // TODO validate the jwt token

        channel.getReceiveSetter().set(new AbstractReceiveListener() {
            @Override
            protected void onFullTextMessage(WebSocketChannel channel, BufferedTextMessage message) {
                final String messageData = message.getData();
                for(WebSocketChannel session : channel.getPeerConnections()) {
                    WebSockets.sendText(messageData, session, null);
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
}
