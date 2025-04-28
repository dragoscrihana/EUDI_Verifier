package com.example.verifier.config;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.time.Clock;
import java.util.UUID;

@Configuration
public class SigningConfig {

    @Bean
    public ECKey ecKey() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
        keyPairGenerator.initialize(Curve.P_256.toECParameterSpec());
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        return new ECKey.Builder(Curve.P_256, (ECPublicKey) keyPair.getPublic())
                .privateKey((ECPrivateKey) keyPair.getPrivate())
                .keyUse(KeyUse.SIGNATURE)
                .keyID(UUID.randomUUID().toString())
                .build();
    }

    @Bean
    public ECPrivateKey ecPrivateKey(ECKey ecKey) throws JOSEException {
        return (ECPrivateKey) ecKey.toECPrivateKey();
    }
}
