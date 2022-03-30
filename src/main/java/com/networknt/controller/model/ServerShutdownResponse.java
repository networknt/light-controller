package com.networknt.controller.model;

import java.util.Arrays;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ServerShutdownResponse  {

    private java.lang.Long time;
    private String protocol;
    private String address;
    private Integer port;
    private String serviceId;
    private String tag;

    public ServerShutdownResponse () {
    }

    @JsonProperty("time")
    public java.lang.Long getTime() {
        return time;
    }

    public void setTime(java.lang.Long time) {
        this.time = time;
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

    @JsonProperty("serviceId")
    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    @JsonProperty("tag")
    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ServerShutdownResponse ServerShutdownResponse = (ServerShutdownResponse) o;

        return Objects.equals(time, ServerShutdownResponse.time) &&
               Objects.equals(protocol, ServerShutdownResponse.protocol) &&
               Objects.equals(address, ServerShutdownResponse.address) &&
               Objects.equals(port, ServerShutdownResponse.port) &&
               Objects.equals(serviceId, ServerShutdownResponse.serviceId) &&
               Objects.equals(tag, ServerShutdownResponse.tag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(time, protocol, address, port, serviceId, tag);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class ServerShutdownResponse {\n");
        sb.append("    time: ").append(toIndentedString(time)).append("\n");        sb.append("    protocol: ").append(toIndentedString(protocol)).append("\n");        sb.append("    address: ").append(toIndentedString(address)).append("\n");        sb.append("    port: ").append(toIndentedString(port)).append("\n");        sb.append("    serviceId: ").append(toIndentedString(serviceId)).append("\n");        sb.append("    tag: ").append(toIndentedString(tag)).append("\n");
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
