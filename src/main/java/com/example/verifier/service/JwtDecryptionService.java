package com.example.verifier.service;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDHDecrypter;
import com.nimbusds.jose.jwk.ECKey;
import org.springframework.stereotype.Service;

import java.text.ParseException;

@Service
public class JwtDecryptionService {

    public String decryptJwt(String encryptedJwt, ECKey ephemeralEcKey) throws JOSEException, ParseException, ParseException {
        JWEObject jweObject = JWEObject.parse(encryptedJwt);

        ECDHDecrypter decrypter = new ECDHDecrypter(ephemeralEcKey.toECPrivateKey());
        jweObject.decrypt(decrypter);

        Payload payload = jweObject.getPayload();
        return payload.toString();
    }
}
