package com.example.verifier;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EntityScan(basePackages = "com.example.verifier.model")
@EnableJpaRepositories(basePackages = "com.example.verifier.repository")
@EnableScheduling
public class  VerifierApplication {

	public static void main(String[] args) {
		SpringApplication.run(VerifierApplication.class, args);
	}

}
