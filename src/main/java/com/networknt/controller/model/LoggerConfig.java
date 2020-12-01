package com.networknt.controller.model;

import java.util.Arrays;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LoggerConfig  {

    private String protocol;
    private String address;
    private Integer port;
    private java.util.List<LoggerInfo> loggers;

    public LoggerConfig () {
    }

    @JsonProperty("protocol")
    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    @JsonProperty("address")
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @JsonProperty("port")
    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    @JsonProperty("loggers")
    public java.util.List<LoggerInfo> getLoggers() {
        return loggers;
    }

    public void setLoggers(java.util.List<LoggerInfo> loggers) {
        this.loggers = loggers;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        LoggerConfig LoggerConfig = (LoggerConfig) o;

        return Objects.equals(protocol, LoggerConfig.protocol) &&
               Objects.equals(address, LoggerConfig.address) &&
               Objects.equals(port, LoggerConfig.port) &&
               Objects.equals(loggers, LoggerConfig.loggers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(protocol, address, port, loggers);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class LoggerConfig {\n");
        sb.append("    protocol: ").append(toIndentedString(protocol)).append("\n");        sb.append("    address: ").append(toIndentedString(address)).append("\n");        sb.append("    port: ").append(toIndentedString(port)).append("\n");        sb.append("    loggers: ").append(toIndentedString(loggers)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString(Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }
}
