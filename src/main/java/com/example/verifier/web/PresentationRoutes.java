package com.example.verifier.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.*;

@Configuration
public class PresentationRoutes {

    @Bean
    public RouterFunction<ServerResponse> routes(PresentationHandler handler) {
        return RouterFunctions
                .route()
                .POST("/ui/presentations", RequestPredicates.accept(MediaType.APPLICATION_JSON), handler::postInitTransaction)
                .build();
    }
}
