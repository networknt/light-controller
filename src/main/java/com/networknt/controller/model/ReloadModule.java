package com.networknt.controller.model;

import java.util.Arrays;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ReloadModule  {

    private String protocol;
    private String address;
    private Integer port;
    private java.util.List<String> modules;

    public ReloadModule () {
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

    @JsonProperty("modules")
    public java.util.List<String> getModules() {
        return modules;
    }

    public void setModules(java.util.List<String> modules) {
        this.modules = modules;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ReloadModule ReloadModule = (ReloadModule) o;

        return Objects.equals(protocol, ReloadModule.protocol) &&
               Objects.equals(address, ReloadModule.address) &&
               Objects.equals(port, ReloadModule.port) &&
               Objects.equals(modules, ReloadModule.modules);
    }

    @Override
    public int hashCode() {
        return Objects.hash(protocol, address, port, modules);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class ReloadModule {\n");
        sb.append("    protocol: ").append(toIndentedString(protocol)).append("\n");        sb.append("    address: ").append(toIndentedString(address)).append("\n");        sb.append("    port: ").append(toIndentedString(port)).append("\n");        sb.append("    modules: ").append(toIndentedString(modules)).append("\n");
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
