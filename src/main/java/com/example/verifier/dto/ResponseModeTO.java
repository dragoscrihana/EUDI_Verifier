package com.example.verifier.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum ResponseModeTO {

    @JsonProperty("direct_post")
    DirectPost,

    @JsonProperty("direct_post.jwt")
    DirectPostJwt
}
