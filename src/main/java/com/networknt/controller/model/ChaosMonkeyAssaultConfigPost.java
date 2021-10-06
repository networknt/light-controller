package com.networknt.controller.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class ChaosMonkeyAssaultConfigPost<T> {

    @JsonProperty("address")
    private String address;

    @JsonProperty("protocol")
    private String protocol;

    @JsonProperty("port")
    private int port;

    @JsonProperty("assaultType")
    private String assaultType;

    @JsonProperty("assaultConfig")
    private T assaultConfig;


    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getAssaultType() {
        return assaultType;
    }

    public void setAssaultType(String assaultType) {
        this.assaultType = assaultType;
    }

    public T getAssaultConfig() {
        return assaultConfig;
    }

    public void setAssaultConfig(T assaultConfig) {
        this.assaultConfig = assaultConfig;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChaosMonkeyAssaultConfigPost<?> that = (ChaosMonkeyAssaultConfigPost<?>) o;
        return port == that.port &&
                Objects.equals(address, that.address) &&
                Objects.equals(protocol, that.protocol) &&
                Objects.equals(assaultType, that.assaultType) &&
                Objects.equals(assaultConfig, that.assaultConfig);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, protocol, port, assaultType, assaultConfig);
    }

    @Override
    public String toString() {
        return "ChaosMonkeyAssaultConfigPost{" +
                "address='" + address + '\'' +
                ", protocol='" + protocol + '\'' +
                ", port=" + port +
                ", assaultType='" + assaultType + '\'' +
                ", assaultConfig=" + assaultConfig +
                '}';
    }
}
