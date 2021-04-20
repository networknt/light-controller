package com.networknt.controller;

public class ControllerConfig {
    public static String CONFIG_NAME = "controller";
    private boolean dynamicToken;
    private String bootstrapToken;
    private int clientTimeout;
    public ControllerConfig() {
    }

    public boolean isDynamicToken() {
        return dynamicToken;
    }

    public void setDynamicToken(boolean dynamicToken) {
        this.dynamicToken = dynamicToken;
    }

    public String getBootstrapToken() {
        return bootstrapToken;
    }

    public void setBootstrapToken(String bootstrapToken) {
        this.bootstrapToken = bootstrapToken;
    }

    public int getClientTimeout() {
        return clientTimeout;
    }

    public void setClientTimeout(int clientTimeout) {
        this.clientTimeout = clientTimeout;
    }
}
