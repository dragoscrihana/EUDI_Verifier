package com.example.verifier.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FilterTO {

    @JsonProperty("type")
    private String type;

    @JsonProperty("const")
    private String constant;

    public String getType() {
        return type;
    }

    public String getConstant() {
        return constant;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setConstant(String constant) {
        this.constant = constant;
    }
}
