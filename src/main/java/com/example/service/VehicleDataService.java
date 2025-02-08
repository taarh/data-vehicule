package com.example.service;

import com.example.model.VehicleData;
import com.example.repository.VehicleDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleDataService {

    private final VehicleDataRepository vehicleDataRepository;
    private final RiskAssessmentService riskAssessmentService;

    public Mono<VehicleData> processVehicleData(VehicleData data) {
        if (data == null) {
            return Mono.error(new IllegalArgumentException("Vehicle data cannot be null"));
        }
        
        log.debug("Processing vehicle data for vehicleId: {}", data.getVehicleId());
        return riskAssessmentService.assessRisk(data)
            .flatMap(this::saveVehicleData)
            .doOnSuccess(savedData -> 
                log.info("Processed and saved vehicle data for vehicle: {}", savedData.getVehicleId()))
            .doOnError(error -> 
                log.error("Error processing vehicle data: {}", error.getMessage()));
    }

    public Mono<VehicleData> saveVehicleData(VehicleData data) {
        if (data.getTimestamp() == null) {
            data.setTimestamp(Instant.now());
        }
        log.debug("Saving vehicle data for vehicleId: {}", data.getVehicleId());
        return vehicleDataRepository.save(data)
            .doOnSuccess(savedData -> log.debug("Successfully saved data for vehicleId: {}", savedData.getVehicleId()))
            .doOnError(error -> log.error("Error saving data for vehicleId: {}", data.getVehicleId(), error));
    }

    public Mono<VehicleData> getLatestVehicleData(String vehicleId) {
        log.info("Service - Fetching latest vehicle data for vehicleId: {}", vehicleId);
        return vehicleDataRepository.findFirstByVehicleIdOrderByTimestampDesc(vehicleId)
            .doOnSubscribe(s -> log.info("Starting database query for vehicleId: {}", vehicleId))
            .doOnNext(data -> log.info("Found data in database: {}", data))
            .doOnError(error -> log.error("Database error for vehicleId: {}", vehicleId, error));
    }

    public Flux<VehicleData> getVehicleDataHistory(String vehicleId, Pageable pageable) {
        log.debug("Fetching vehicle data history for vehicleId: {} with pageable: {}", vehicleId, pageable);
        return vehicleDataRepository.findByVehicleIdOrderByTimestampDesc(vehicleId, pageable)
            .doOnComplete(() -> log.debug("Completed fetching history for vehicleId: {}", vehicleId))
            .doOnError(error -> log.error("Error fetching history for vehicleId: {}", vehicleId, error));
    }

    public Flux<VehicleData> getVehicleDataByTimeRange(String vehicleId, Instant startTime, Instant endTime) {
        log.debug("Fetching vehicle data for vehicleId: {} between {} and {}", vehicleId, startTime, endTime);
        return vehicleDataRepository.findByVehicleIdAndTimestampBetween(vehicleId, startTime, endTime)
            .doOnComplete(() -> log.debug("Completed fetching time range data for vehicleId: {}", vehicleId))
            .doOnError(error -> log.error("Error fetching time range data for vehicleId: {}", vehicleId, error));
    }

    public Mono<Long> getVehicleDataCount(String vehicleId) {
        log.debug("Counting records for vehicleId: {}", vehicleId);
        return vehicleDataRepository.countByVehicleId(vehicleId)
            .doOnSuccess(count -> log.debug("Found {} records for vehicleId: {}", count, vehicleId))
            .doOnError(error -> log.error("Error counting records for vehicleId: {}", vehicleId, error));
    }

    public Mono<Boolean> checkDatabaseConnection() {
        return vehicleDataRepository.count()
            .map(count -> {
                log.info("Total documents in collection: {}", count);
                return true;
            })
            .doOnError(error -> log.error("Database connection error: {}", error.getMessage()));
    }
} 