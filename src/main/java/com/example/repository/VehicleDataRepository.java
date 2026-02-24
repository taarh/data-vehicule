package com.example.repository;

import com.example.model.RiskLevel;
import com.example.model.VehicleData;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface VehicleDataRepository extends ReactiveMongoRepository<VehicleData, String> {
    
    Flux<VehicleData> findByVehicleId(String vehicleId);
    
    Mono<VehicleData> findFirstByVehicleIdOrderByTimestampDesc(String vehicleId);
    
    Flux<VehicleData> findByVehicleIdAndRiskAssessment_Level(String vehicleId, RiskLevel riskLevel);
    
    Flux<VehicleData> findByContractId(String contractId);
    
    Flux<VehicleData> findByVehicleIdOrderByTimestampDesc(String vehicleId, Pageable pageable);
    
    Flux<VehicleData> findByVehicleIdAndTimestampBetween(
        String vehicleId, 
        java.time.Instant startTime, 
        java.time.Instant endTime
    );
    
    Mono<Long> countByVehicleId(String vehicleId);

    /**
     * Used for idempotent consumption: find by business key to detect duplicates.
     */
    Mono<VehicleData> findByVehicleIdAndContractIdAndTimestamp(
            String vehicleId, String contractId, java.time.Instant timestamp);
} 