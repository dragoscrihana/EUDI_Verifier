package com.example.verifier.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class PresentationDefinitionTO {

    @JsonProperty("id")
    private String id;

    @JsonProperty("input_descriptors")
    private List<InputDescriptorTO> input_descriptors;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<InputDescriptorTO> getInputDescriptors() {
        return input_descriptors;
    }

    public void setInputDescriptors(List<InputDescriptorTO> inputDescriptors) {
        this.input_descriptors = inputDescriptors;
    }
}
