package com.networknt.controller.model;

import java.util.Arrays;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Check  {

    private Boolean tlsSkipVerify;
    private String notes;
    private String name;
    private String http;
    private String interval;
    private String id;
    private String deregisterCriticalServiceAfter;
    private String ttl;
    private long lastFailedTimestamp = 0L;

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
    public String getInterval() {
        return interval;
    }

    public void setInterval(String interval) {
        this.interval = interval;
    }

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty("deregisterCriticalServiceAfter")
    public String getDeregisterCriticalServiceAfter() {
        return deregisterCriticalServiceAfter;
    }

    public void setDeregisterCriticalServiceAfter(String deregisterCriticalServiceAfter) {
        this.deregisterCriticalServiceAfter = deregisterCriticalServiceAfter;
    }

    @JsonProperty("ttl")
    public String getTtl() {
        return ttl;
    }

    public void setTtl(String ttl) {
        this.ttl = ttl;
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
               Objects.equals(deregisterCriticalServiceAfter, Check.deregisterCriticalServiceAfter) &&
               Objects.equals(ttl, Check.ttl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tlsSkipVerify, notes, name, http, interval, id, deregisterCriticalServiceAfter, ttl);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Check {\n");
        sb.append("    tlsSkipVerify: ").append(toIndentedString(tlsSkipVerify)).append("\n");        sb.append("    notes: ").append(toIndentedString(notes)).append("\n");        sb.append("    name: ").append(toIndentedString(name)).append("\n");        sb.append("    http: ").append(toIndentedString(http)).append("\n");        sb.append("    interval: ").append(toIndentedString(interval)).append("\n");        sb.append("    id: ").append(toIndentedString(id)).append("\n");        sb.append("    deregisterCriticalServiceAfter: ").append(toIndentedString(deregisterCriticalServiceAfter)).append("\n");        sb.append("    ttl: ").append(toIndentedString(ttl)).append("\n");
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
}
