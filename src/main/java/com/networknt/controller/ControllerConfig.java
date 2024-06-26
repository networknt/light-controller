package com.networknt.controller;

public class ControllerConfig {
    public static String CONFIG_NAME = "controller";
    private boolean clusterMode;
    private String topic;
    private boolean dynamicToken;
    private String bootstrapToken;
    private int clientTimeout;
    private String schedulerTopic;
    private String healthCheckTopic;
    private String registryApplicationId;
    private String healthApplicationId;

    public ControllerConfig() {
    }

    public boolean isClusterMode() {
        return clusterMode;
    }

    public void setClusterMode(boolean clusterMode) {
        this.clusterMode = clusterMode;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
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

    public String getSchedulerTopic() {
        return schedulerTopic;
    }

    public void setSchedulerTopic(String schedulerTopic) {
        this.schedulerTopic = schedulerTopic;
    }

    public String getHealthCheckTopic() {
        return healthCheckTopic;
    }

    public void setHealthCheckTopic(String healthCheckTopic) {
        this.healthCheckTopic = healthCheckTopic;
    }

    public String getRegistryApplicationId() {
        return registryApplicationId;
    }

    public void setRegistryApplicationId(String registryApplicationId) {
        this.registryApplicationId = registryApplicationId;
    }

    public String getHealthApplicationId() {
        return healthApplicationId;
    }

    public void setHealthApplicationId(String healthApplicationId) {
        this.healthApplicationId = healthApplicationId;
    }
}
