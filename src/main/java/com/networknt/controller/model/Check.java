package com.networknt.controller.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.networknt.utility.StringUtils;

public class Check  {

    private Boolean tlsSkipVerify;
    private String notes;
    private String name;
    private String http;
    private Integer interval;
    private String id;
    private Integer deregisterCriticalServiceAfter;
    private long lastExecuteTimestamp = 0L;
    private long lastFailedTimestamp = 0L;
    private String serviceId;
    private String tag;
    private String address;
    private int port;

    public Check () {
    }

    @JsonProperty("tlsSkipVerify")
    public Boolean getTlsSkipVerify() {
        return tlsSkipVerify;
    }

    public void setTlsSkipVerify(Boolean tlsSkipVerify) {
        this.tlsSkipVerify = tlsSkipVerify;
    }

    @JsonProperty("notes")
    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("http")
    public String getHttp() {
        return http;
    }

    public void setHttp(String http) {
        this.http = http;
    }

    @JsonProperty("interval")
    public Integer getInterval() {
        return interval;
    }

    public void setInterval(Integer interval) {
        this.interval = interval;
    }

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
        String[] parts = StringUtils.split(id, ":");
        int index = parts[0].indexOf("|");
        if(index > 0) {
            this.serviceId = parts[0].substring(0, index);
            this.tag = parts[0].substring(index + 1);
        } else {
            this.serviceId = parts[0];
        }
        this.address = parts[1];
        this.port = Integer.valueOf(parts[2]);
    }

    @JsonProperty("deregisterCriticalServiceAfter")
    public Integer getDeregisterCriticalServiceAfter() {
        return deregisterCriticalServiceAfter;
    }

    public void setDeregisterCriticalServiceAfter(Integer deregisterCriticalServiceAfter) {
        this.deregisterCriticalServiceAfter = deregisterCriticalServiceAfter;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Check Check = (Check) o;

        return Objects.equals(tlsSkipVerify, Check.tlsSkipVerify) &&
               Objects.equals(notes, Check.notes) &&
               Objects.equals(name, Check.name) &&
               Objects.equals(http, Check.http) &&
               Objects.equals(interval, Check.interval) &&
               Objects.equals(id, Check.id) &&
               Objects.equals(deregisterCriticalServiceAfter, Check.deregisterCriticalServiceAfter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tlsSkipVerify, notes, name, http, interval, id, deregisterCriticalServiceAfter);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Check {\n");
        sb.append("    tlsSkipVerify: ").append(toIndentedString(tlsSkipVerify)).append("\n");        sb.append("    notes: ").append(toIndentedString(notes)).append("\n");        sb.append("    name: ").append(toIndentedString(name)).append("\n");        sb.append("    http: ").append(toIndentedString(http)).append("\n");        sb.append("    interval: ").append(toIndentedString(interval)).append("\n");        sb.append("    id: ").append(toIndentedString(id)).append("\n");        sb.append("    deregisterCriticalServiceAfter: ").append(toIndentedString(deregisterCriticalServiceAfter)).append("\n");
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

    public long getLastFailedTimestamp() {
        return lastFailedTimestamp;
    }

    public void setLastFailedTimestamp(long lastFailedTimestamp) {
        this.lastFailedTimestamp = lastFailedTimestamp;
    }

    public long getLastExecuteTimestamp() {
        return lastExecuteTimestamp;
    }

    public void setLastExecuteTimestamp(long lastExecuteTimestamp) {
        this.lastExecuteTimestamp = lastExecuteTimestamp;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
