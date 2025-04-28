package com.example.verifier.util;

import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.KeyUse;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.UUID;

public class EphemeralKeyGenerator {

    public static ECKey generateEphemeralEncryptionKey() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
            keyGen.initialize(Curve.P_256.toECParameterSpec());
            KeyPair keyPair = keyGen.generateKeyPair();

            ECPublicKey publicKey = (ECPublicKey) keyPair.getPublic();
            ECPrivateKey privateKey = (ECPrivateKey) keyPair.getPrivate();

            return new ECKey.Builder(Curve.P_256, publicKey)
                    .privateKey(privateKey)
                    .keyUse(KeyUse.ENCRYPTION)
                    .algorithm(JWEAlgorithm.ECDH_ES)
                    .keyID(UUID.randomUUID().toString())
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate ephemeral EC key", e);
        }
    }
}
