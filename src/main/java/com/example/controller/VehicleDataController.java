package com.example.controller;

import com.example.model.VehicleData;
import com.example.service.VehicleDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Slf4j
@RestController
@RequestMapping("/api/vehicle-data")
@RequiredArgsConstructor
public class VehicleDataController {

    private final VehicleDataService vehicleDataService;

    @GetMapping("/test")
    public Mono<String> test() {
        return Mono.just("Controller is working!");
    }

    @GetMapping("/{vehicleId}")
    public Mono<ResponseEntity<Flux<VehicleData>>> getVehicleDataHistory(
            @PathVariable String vehicleId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Fetching vehicle data history for vehicleId: {}, page: {}, size: {}", vehicleId, page, size);
        
        return vehicleDataService.getVehicleDataCount(vehicleId)
            .flatMap(count -> {
                if (count == 0) {
                    log.warn("No data found for vehicleId: {}", vehicleId);
                    return Mono.just(ResponseEntity.notFound().<Flux<VehicleData>>build());
                }
                
                Flux<VehicleData> data = vehicleDataService.getVehicleDataHistory(vehicleId, PageRequest.of(page, size))
                    .doOnComplete(() -> log.info("Completed fetching data for vehicleId: {}", vehicleId))
                    .doOnError(error -> log.error("Error fetching data for vehicleId: {}", vehicleId, error));
                
                return Mono.just(ResponseEntity.ok(data));
            })
            .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/{vehicleId}/latest")
    public Mono<ResponseEntity<VehicleData>> getLatestVehicleData(@PathVariable String vehicleId) {
        log.info("Received request for latest vehicle data - vehicleId: {}", vehicleId);
        return vehicleDataService.getLatestVehicleData(vehicleId)
            .doOnSubscribe(s -> log.info("Starting to fetch latest data for vehicleId: {}", vehicleId))
            .doOnNext(data -> log.info("Found data: {}", data))
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.notFound().build())
            .doOnSuccess(response -> {
                if (response.getStatusCode().is2xxSuccessful()) {
                    log.info("Successfully returned data for vehicleId: {}", vehicleId);
                } else {
                    log.warn("No data found for vehicleId: {}", vehicleId);
                }
            })
            .doOnError(error -> log.error("Error fetching latest data for vehicleId: {}", vehicleId, error));
    }

    @GetMapping("/{vehicleId}/timerange")
    public Mono<ResponseEntity<Flux<VehicleData>>> getVehicleDataByTimeRange(
            @PathVariable String vehicleId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime) {
        log.info("Fetching vehicle data for vehicleId: {} between {} and {}", vehicleId, startTime, endTime);
        
        Flux<VehicleData> data = vehicleDataService.getVehicleDataByTimeRange(vehicleId, startTime, endTime)
            .doOnComplete(() -> log.info("Completed fetching time range data for vehicleId: {}", vehicleId))
            .doOnError(error -> log.error("Error fetching time range data for vehicleId: {}", vehicleId, error));
        
        return Mono.just(ResponseEntity.ok(data));
    }

    @GetMapping("/{vehicleId}/count")
    public Mono<ResponseEntity<Long>> getVehicleDataCount(@PathVariable String vehicleId) {
        log.info("Fetching count for vehicleId: {}", vehicleId);
        return vehicleDataService.getVehicleDataCount(vehicleId)
            .map(count -> {
                log.info("Found {} records for vehicleId: {}", count, vehicleId);
                return ResponseEntity.ok(count);
            })
            .defaultIfEmpty(ResponseEntity.ok(0L))
            .doOnError(error -> log.error("Error fetching count for vehicleId: {}", vehicleId, error));
    }
} 