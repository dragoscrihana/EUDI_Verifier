package com.example.verifier.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FormatTO {

    @JsonProperty("vc+sd-jwt")
    private VcSdJwtTO vcSdJwt;

    public VcSdJwtTO getVcSdJwt() {
        return vcSdJwt;
    }

    public void setVcSdJwt(VcSdJwtTO vcSdJwt) {
        this.vcSdJwt = vcSdJwt;
    }
}
