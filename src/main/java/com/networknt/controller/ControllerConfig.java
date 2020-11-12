package com.networknt.controller;

public class ControllerConfig {
    public static String CONFIG_NAME = "controller";
    private String dataPath;

    public ControllerConfig() {
    }

    public String getDataPath() {
        return dataPath;
    }

    public void setDataPath(String dataPath) {
        this.dataPath = dataPath;
    }
}
