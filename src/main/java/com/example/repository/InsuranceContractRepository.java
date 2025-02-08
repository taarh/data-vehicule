package com.example.repository;

import com.example.model.InsuranceContract;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface InsuranceContractRepository extends ReactiveMongoRepository<InsuranceContract, String> {
    Flux<InsuranceContract> findByVehicleId(String vehicleId);
} 