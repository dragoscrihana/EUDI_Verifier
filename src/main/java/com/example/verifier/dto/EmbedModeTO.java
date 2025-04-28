package com.example.verifier.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum EmbedModeTO {

    @JsonProperty("by_value")
    ByValue,

    @JsonProperty("by_reference")
    ByReference
}
