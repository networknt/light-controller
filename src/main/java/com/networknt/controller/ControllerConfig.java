package com.networknt.controller;

public class ControllerConfig {
    public static String CONFIG_NAME = "controller";
    private String bootstrapToken;

    public ControllerConfig() {
    }

    public String getBootstrapToken() {
        return bootstrapToken;
    }

    public void setBootstrapToken(String bootstrapToken) {
        this.bootstrapToken = bootstrapToken;
    }
}
