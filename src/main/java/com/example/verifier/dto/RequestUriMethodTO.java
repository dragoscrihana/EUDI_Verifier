package com.example.verifier.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum RequestUriMethodTO {

    @JsonProperty("get")
    Get,

    @JsonProperty("post")
    Post
}
