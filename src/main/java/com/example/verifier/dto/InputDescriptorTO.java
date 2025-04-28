package com.example.verifier.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class InputDescriptorTO {

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("purpose")
    private String purpose;

    @JsonProperty("format")
    private Object format;

    @JsonProperty("constraints")
    private ConstraintsTO constraints;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public Object getFormat() {
        return format;
    }

    public void setFormat(Object format) {
        this.format = format;
    }

    public ConstraintsTO getConstraints() {
        return constraints;
    }

    public void setConstraints(ConstraintsTO constraints) {
        this.constraints = constraints;
    }
}
