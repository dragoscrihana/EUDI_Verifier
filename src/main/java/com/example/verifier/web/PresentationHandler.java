package com.example.verifier.web;

import com.example.verifier.dto.InitTransactionTO;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.*;

import reactor.core.publisher.Mono;

@Component
public class PresentationHandler {

    public Mono<ServerResponse> postInitTransaction(ServerRequest request) {
        return request.bodyToMono(InitTransactionTO.class)
                .flatMap(input -> {
                    String response = "Received InitTransactionTO. Nonce = " + input.getNonce();
                    return ServerResponse.ok()
                            .contentType(MediaType.TEXT_PLAIN)
                            .bodyValue(response);
                });
    }
}
