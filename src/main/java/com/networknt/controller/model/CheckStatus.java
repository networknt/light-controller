package com.networknt.controller.model;

import java.util.Arrays;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CheckStatus  {

    private Boolean pass;
    private String id;

    public CheckStatus () {
    }

    @JsonProperty("pass")
    public Boolean getPass() {
        return pass;
    }

    public void setPass(Boolean pass) {
        this.pass = pass;
    }

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CheckStatus CheckStatus = (CheckStatus) o;

        return Objects.equals(pass, CheckStatus.pass) &&
               Objects.equals(id, CheckStatus.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pass, id);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class CheckStatus {\n");
        sb.append("    pass: ").append(toIndentedString(pass)).append("\n");        sb.append("    id: ").append(toIndentedString(id)).append("\n");
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
