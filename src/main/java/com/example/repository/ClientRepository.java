package com.example.repository;

import com.example.model.Client;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface ClientRepository extends ReactiveMongoRepository<Client, String> {
    Flux<Client> findByNom(String nom);
} 