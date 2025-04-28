package com.example.verifier.service;

import com.example.verifier.dto.InitTransactionResponse;
import com.example.verifier.dto.InitTransactionTO;
import com.example.verifier.dto.PresentationDefinitionTO;
import com.example.verifier.model.Presentation;
import com.example.verifier.storage.PresentationStore;
import com.example.verifier.util.EphemeralKeyGenerator;
import com.example.verifier.util.IdGenerator;
import com.nimbusds.jose.jwk.ECKey;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Service
public class VerifierService {

    private final Clock clock;
    private final PresentationStore presentationStore;

    private static final String BASE_REQUEST_URI = "https://glowing-gradually-midge.ngrok-free.app/wallet/request.jwt/";

    public VerifierService(Clock clock, PresentationStore presentationStore) {
        this.clock = clock;
        this.presentationStore = presentationStore;
    }

    public InitTransactionResponse handleInitTransaction(InitTransactionTO initTransactionTO) {
        String transactionId = IdGenerator.generateTransactionId();
        String requestId = IdGenerator.generateRequestId();
        Instant initiatedAt = clock.instant();
        PresentationDefinitionTO presentationDefinition = initTransactionTO.getPresentationDefinition();
        ECKey ephemeralKey = EphemeralKeyGenerator.generateEphemeralEncryptionKey();
        String state = requestId;

        Presentation.Requested requestedPresentation = new Presentation.Requested(
                transactionId,
                initiatedAt,
                requestId,
                presentationDefinition,
                ephemeralKey,
                initTransactionTO.getNonce(),
                state
        );

        presentationStore.store(requestedPresentation);

        String requestUri = BASE_REQUEST_URI + requestId;
        return new InitTransactionResponse(transactionId, "Verifier", requestUri);
    }
}
