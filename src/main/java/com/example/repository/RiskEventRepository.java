package com.example.repository;

import com.example.model.RiskEvent;
import com.example.model.RiskEventType;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface RiskEventRepository extends ReactiveMongoRepository<RiskEvent, String> {
    Flux<RiskEvent> findByVehicleId(String vehicleId);
    Flux<RiskEvent> findByVehicleIdAndType(String vehicleId, RiskEventType type);
} 