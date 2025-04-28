package com.example.verifier.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum PresentationTypeTO {

    @JsonProperty("id_token")
    IdTokenRequest,

    @JsonProperty("vp_token")
    VpTokenRequest,

    @JsonProperty("vp_token id_token")
    IdAndVpTokenRequest
}
