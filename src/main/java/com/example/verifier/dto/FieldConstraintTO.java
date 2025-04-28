package com.example.verifier.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class FieldConstraintTO {

    @JsonProperty("path")
    private List<String> path;

    @JsonProperty("filter")
    private Map<String, Object> filter;

    @JsonProperty("intent_to_retain")
    private Boolean intentToRetain;

    public List<String> getPath() {
        return path;
    }

    public void setPath(List<String> path) {
        this.path = path;
    }

    public Map<String, Object> getFilter() {
        return filter;
    }

    public void setFilter(Map<String, Object> filter) {
        this.filter = filter;
    }

    public Boolean getIntentToRetain() {
        return intentToRetain;
    }

    public void setIntentToRetain(Boolean intentToRetain) {
        this.intentToRetain = intentToRetain;
    }
}
