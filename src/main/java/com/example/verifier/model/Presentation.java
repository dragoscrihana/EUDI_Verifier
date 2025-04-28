package com.example.verifier.model;

import com.example.verifier.dto.PresentationDefinitionTO;
import com.nimbusds.jose.jwk.ECKey;

import java.time.Instant;

public class Presentation {

    public static class Requested {
        private final String id;
        private final Instant initiatedAt;
        private final String requestId;
        private final PresentationDefinitionTO presentationDefinition;
        private final ECKey jarmEncryptionEphemeralKey;
        private final String nonce;
        private final String state;

        public Requested(
                String id,
                Instant initiatedAt,
                String requestId,
                PresentationDefinitionTO presentationDefinition,
                ECKey jarmEncryptionEphemeralKey,
                String nonce,
                String state
        ) {
            this.id = id;
            this.initiatedAt = initiatedAt;
            this.requestId = requestId;
            this.presentationDefinition = presentationDefinition;
            this.jarmEncryptionEphemeralKey = jarmEncryptionEphemeralKey;
            this.nonce = nonce;
            this.state = state;
        }

        public String getId() {
            return id;
        }

        public Instant getInitiatedAt() {
            return initiatedAt;
        }

        public String getRequestId() {
            return requestId;
        }

        public PresentationDefinitionTO getPresentationDefinition() {
            return presentationDefinition;
        }

        public ECKey getJarmEncryptionEphemeralKey() {
            return jarmEncryptionEphemeralKey;
        }

        public String getNonce() {
            return nonce;
        }

        public String getState() {
            return state;
        }
    }
}
