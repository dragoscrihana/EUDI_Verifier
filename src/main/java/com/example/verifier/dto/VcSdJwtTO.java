package com.example.verifier.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class VcSdJwtTO {

    @JsonProperty("sd-jwt_alg_values")
    private List<String> sdJwtAlgValues;

    @JsonProperty("kb-jwt_alg_values")
    private List<String> kbJwtAlgValues;

    public List<String> getSdJwtAlgValues() {
        return sdJwtAlgValues;
    }

    public List<String> getKbJwtAlgValues() {
        return kbJwtAlgValues;
    }

    public void setSdJwtAlgValues(List<String> sdJwtAlgValues) {
        this.sdJwtAlgValues = sdJwtAlgValues;
    }

    public void setKbJwtAlgValues(List<String> kbJwtAlgValues) {
        this.kbJwtAlgValues = kbJwtAlgValues;
    }
}
