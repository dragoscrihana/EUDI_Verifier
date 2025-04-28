package com.example.verifier.service;

import com.example.verifier.model.Presentation;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.id.ClientID;
import org.springframework.stereotype.Service;

import java.security.interfaces.ECPrivateKey;
import java.time.Instant;
import java.util.*;

@Service
public class CreateJarService {

    private final ECPrivateKey privateKey;
    private final ECKey ecKey;
    private final String clientId;
    private final String audience;

    public CreateJarService(ECPrivateKey privateKey, ECKey ecKey) {
        this.privateKey = privateKey;
        this.ecKey = ecKey;
        this.clientId = "Verifier";
        this.audience = "https://self-issued.me/v2";
    }

    public String createSignedRequestObject(Presentation.Requested presentation) throws Exception {
        Instant now = Instant.now();

        Map<String, Object> clientMetadata = buildClientMetadata(presentation);

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .audience(audience)
                .issueTime(Date.from(now))
                .claim("scope", "")
                .claim("response_type", "vp_token")
                .claim("response_uri", "https://glowing-gradually-midge.ngrok-free.app/wallet/direct_post/" + presentation.getRequestId())
                .claim("presentation_definition", presentation.getPresentationDefinition())
                .claim("state", presentation.getRequestId())
                .claim("nonce", UUID.randomUUID().toString())
                .claim("client_id", clientId)
                .claim("response_mode", "direct_post.jwt")
                .claim("client_metadata", clientMetadata)
                .build();

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
                .type(new JOSEObjectType("oauth-authz-req+jwt"))
                .keyID(ecKey.getKeyID())
                .build();

        SignedJWT signedJWT = new SignedJWT(header, claimsSet);

        signedJWT.sign(new ECDSASigner(privateKey));

        return signedJWT.serialize();
    }

    private Map<String, Object> buildClientMetadata(Presentation.Requested presentation) {
        Map<String, Object> clientMetadata = new HashMap<>();

        clientMetadata.put("authorization_encrypted_response_alg", "ECDH-ES");
        clientMetadata.put("authorization_encrypted_response_enc", "A128CBC-HS256");

        ECKey ephemeralKey = presentation.getJarmEncryptionEphemeralKey();
        if (ephemeralKey == null) {
            throw new IllegalStateException("Ephemeral encryption key must not be null");
        }

        Map<String, Object> jwk = new HashMap<>();
        jwk.put("kty", ephemeralKey.getKeyType().getValue());
        jwk.put("use", "enc");
        jwk.put("crv", ephemeralKey.getCurve().getName());
        jwk.put("kid", ephemeralKey.getKeyID());
        jwk.put("x", ephemeralKey.getX().toString());
        jwk.put("y", ephemeralKey.getY().toString());
        jwk.put("alg", "ECDH-ES");

        Map<String, Object> jwks = new HashMap<>();
        jwks.put("keys", List.of(jwk));

        clientMetadata.put("jwks", jwks);
        clientMetadata.put("id_token_encrypted_response_alg", "RSA-OAEP-256");
        clientMetadata.put("id_token_encrypted_response_enc", "A128CBC-HS256");

        Map<String, Object> vpFormats = new HashMap<>();
        Map<String, Object> vcSdJwt = Map.of(
                "sd-jwt_alg_values", List.of("ES256"),
                "kb-jwt_alg_values", List.of("ES256")
        );
        Map<String, Object> dcSdJwt = Map.of(
                "sd-jwt_alg_values", List.of("ES256"),
                "kb-jwt_alg_values", List.of("ES256")
        );
        Map<String, Object> msoMdoc = Map.of(
                "alg", List.of("ES256")
        );

        vpFormats.put("vc+sd-jwt", vcSdJwt);
        vpFormats.put("dc+sd-jwt", dcSdJwt);
        vpFormats.put("mso_mdoc", msoMdoc);

        clientMetadata.put("vp_formats", vpFormats);
        clientMetadata.put("subject_syntax_types_supported", List.of("urn:ietf:params:oauth:jwk-thumbprint"));
        clientMetadata.put("id_token_signed_response_alg", "RS256");

        return clientMetadata;
    }

}
