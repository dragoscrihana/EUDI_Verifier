package com.example.verifier.repository;

import com.example.verifier.model.IpfsStatusList;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IpfsStatusListRepository extends JpaRepository<IpfsStatusList, Long> {
    Optional<IpfsStatusList> findByIssuerName(String issuerName);
}
