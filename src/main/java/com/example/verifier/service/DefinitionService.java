package com.example.verifier.service;

import com.example.verifier.dto.PresentationDefinitionTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import java.io.File;
import java.io.FileInputStream;

@Service
public class DefinitionService {

    private final ObjectMapper objectMapper;

    @Value("${presentation.definitions.path}")
    private String basePath;

    public DefinitionService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public PresentationDefinitionTO loadDefinition(String name) {
        File file = new File(basePath + "/" + name + ".json");

        if (!file.exists()) {
            throw new RuntimeException("Definition file not found: " + file.getAbsolutePath());
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            PresentationDefinitionTO def = objectMapper.readValue(fis, PresentationDefinitionTO.class);
            def.setId(UUID.randomUUID().toString());
            return def;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load definition: " + name, e);
        }
    }
}
