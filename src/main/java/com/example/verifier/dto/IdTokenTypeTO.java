package com.example.verifier.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum IdTokenTypeTO {

    @JsonProperty("subject_signed_id_token")
    SubjectSigned,

    @JsonProperty("attester_signed_id_token")
    AttesterSigned
}
