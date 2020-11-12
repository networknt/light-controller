package com.networknt.controller.model;

import java.util.Arrays;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Service  {

    private String name;
    private String id;
    private String tag;
    private String address;
    private Check check;
    private Integer port;

    public Service () {
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty("tag")
    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    @JsonProperty("address")
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @JsonProperty("check")
    public Check getCheck() {
        return check;
    }

    public void setCheck(Check check) {
        this.check = check;
    }

    @JsonProperty("port")
    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Service Service = (Service) o;

        return Objects.equals(name, Service.name) &&
               Objects.equals(id, Service.id) &&
               Objects.equals(tag, Service.tag) &&
               Objects.equals(address, Service.address) &&
               Objects.equals(check, Service.check) &&
               Objects.equals(port, Service.port);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, id, tag, address, check, port);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Service {\n");
        sb.append("    name: ").append(toIndentedString(name)).append("\n");        sb.append("    id: ").append(toIndentedString(id)).append("\n");        sb.append("    tag: ").append(toIndentedString(tag)).append("\n");        sb.append("    address: ").append(toIndentedString(address)).append("\n");        sb.append("    check: ").append(toIndentedString(check)).append("\n");        sb.append("    port: ").append(toIndentedString(port)).append("\n");
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
