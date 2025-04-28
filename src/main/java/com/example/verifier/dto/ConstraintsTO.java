package com.example.verifier.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ConstraintsTO {

    @JsonProperty("fields")
    private List<FieldConstraintTO> fields;

    public List<FieldConstraintTO> getFields() {
        return fields;
    }

    public void setFields(List<FieldConstraintTO> fields) {
        this.fields = fields;
    }
}
