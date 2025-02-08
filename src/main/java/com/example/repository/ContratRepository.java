package com.example.repository;

import com.example.model.Contrat;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface ContratRepository extends ReactiveMongoRepository<Contrat, String> {
    Flux<Contrat> findByClientId(String clientId);
} 