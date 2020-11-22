package com.networknt.controller;

import io.undertow.websockets.core.WebSocketCallback;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerWebSocketClient {
    private static final Logger logger = LoggerFactory.getLogger(ServerWebSocketClient.class);

    private WebSocketChannel channel;
    private long idleTimeout = -1;

    public ServerWebSocketClient(WebSocketChannel channel) {
        this.channel = channel;
        this.channel.setIdleTimeout(this.idleTimeout);
    }

    public boolean send(String text) {
        return this.send(text, null);
    }

    public boolean send(String text, ServerWebSocketCallback callback) {
        if (this.channel != null && this.channel.isOpen()) {
            WebSockets.sendText(text, this.channel, new WebSocketCallback<Void>() {
                @Override
                public void complete(WebSocketChannel channel, Void ignore) {
                    if (callback != null) {
                        callback.complete(null);
                    }
                }

                @Override
                public void onError(WebSocketChannel channel, Void ignore, Throwable throwable) {
                    if (callback != null) {
                        callback.complete(throwable);
                    }
                }
            });
            return true;
        } else {
            return false;
        }
    }
}
