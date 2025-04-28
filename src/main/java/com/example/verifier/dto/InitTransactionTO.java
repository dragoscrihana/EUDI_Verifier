package com.example.verifier.dto;

import com.example.verifier.model.DCQL;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import eu.europa.ec.eudi.prex.PresentationDefinition ;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class InitTransactionTO {

    @JsonProperty("type")
    private PresentationTypeTO type = PresentationTypeTO.IdAndVpTokenRequest;

    @JsonProperty("id_token_type")
    private IdTokenTypeTO idTokenType;

    @JsonProperty("presentation_definition")
    private PresentationDefinitionTO presentationDefinition;

    @JsonProperty("dcql_query")
    private DCQL dcqlQuery;

    @JsonProperty("nonce")
    private String nonce;

    @JsonProperty("response_mode")
    private ResponseModeTO responseMode;

    @JsonProperty("jar_mode")
    private EmbedModeTO jarMode;

    @JsonProperty("request_uri_method")
    private RequestUriMethodTO requestUriMethod;

    @JsonProperty("presentation_definition_mode")
    private EmbedModeTO presentationDefinitionMode;

    @JsonProperty("wallet_response_redirect_uri_template")
    private String redirectUriTemplate;

    @JsonProperty("transaction_data")
    private List<JsonNode> transactionData;

    public PresentationTypeTO getType() {
        return type;
    }

    public void setType(PresentationTypeTO type) {
        this.type = type;
    }

    public IdTokenTypeTO getIdTokenType() {
        return idTokenType;
    }

    public void setIdTokenType(IdTokenTypeTO idTokenType) {
        this.idTokenType = idTokenType;
    }

    public PresentationDefinitionTO getPresentationDefinition() {
        return presentationDefinition;
    }

    public void setPresentationDefinition(PresentationDefinitionTO presentationDefinition) {
        this.presentationDefinition = presentationDefinition;
    }

    public DCQL getDcqlQuery() {
        return dcqlQuery;
    }

    public void setDcqlQuery(DCQL dcqlQuery) {
        this.dcqlQuery = dcqlQuery;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public ResponseModeTO getResponseMode() {
        return responseMode;
    }

    public void setResponseMode(ResponseModeTO responseMode) {
        this.responseMode = responseMode;
    }

    public EmbedModeTO getJarMode() {
        return jarMode;
    }

    public void setJarMode(EmbedModeTO jarMode) {
        this.jarMode = jarMode;
    }

    public RequestUriMethodTO getRequestUriMethod() {
        return requestUriMethod;
    }

    public void setRequestUriMethod(RequestUriMethodTO requestUriMethod) {
        this.requestUriMethod = requestUriMethod;
    }

    public EmbedModeTO getPresentationDefinitionMode() {
        return presentationDefinitionMode;
    }

    public void setPresentationDefinitionMode(EmbedModeTO presentationDefinitionMode) {
        this.presentationDefinitionMode = presentationDefinitionMode;
    }

    public String getRedirectUriTemplate() {
        return redirectUriTemplate;
    }

    public void setRedirectUriTemplate(String redirectUriTemplate) {
        this.redirectUriTemplate = redirectUriTemplate;
    }

    public List<JsonNode> getTransactionData() {
        return transactionData;
    }

    public void setTransactionData(List<JsonNode> transactionData) {
        this.transactionData = transactionData;
    }
}
