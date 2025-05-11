package com.example.verifier;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = "com.example.verifier.model")
@EnableJpaRepositories(basePackages = "com.example.verifier.repository")
public class  VerifierApplication {

	public static void main(String[] args) {
		SpringApplication.run(VerifierApplication.class, args);
	}

}
