package com.example.verifier.repository;

import com.example.verifier.model.StatusList;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StatusListRepository extends JpaRepository<StatusList, Long> {
    Optional<StatusList> findByUrl(String url);
}
