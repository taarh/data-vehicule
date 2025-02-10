package com.example.service;

import com.example.model.VehicleData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Flux;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleDataConsumer {

    private final VehicleDataService vehicleDataService;
    private final ObjectMapper objectMapper;
    private final Sinks.Many<VehicleData> vehicleDataSink = Sinks.many().multicast().onBackpressureBuffer();

    @KafkaListener(
        topics = "${kafka.topic.vehicle-data:vehicle-data}",
        groupId = "${kafka.consumer.group-id:insurance-group}"
    )
    public void consume(String message) {
        log.debug("Received message: {}", message);
        try {
            VehicleData vehicleData = objectMapper.readValue(message, VehicleData.class);
            processMessage(vehicleData)
                .doOnNext(data -> log.info("Processed vehicle data for vehicle: {}", data.getVehicleId()))
                .doOnError(error -> log.error("Error processing vehicle data: {}", error.getMessage()))
                .subscribe(
                    vehicleDataSink::tryEmitNext,
                    error -> log.error("Error emitting to sink: {}", error.getMessage())
                );
        } catch (JsonProcessingException e) {
            log.error("Error deserializing message: {}. Message content: {}", e.getMessage(), message);
        }
    }

    public Flux<VehicleData> consumeVehicleData() {
        return vehicleDataSink.asFlux();
    }

    private Mono<VehicleData> processMessage(VehicleData data) {
        if (data == null) {
            return Mono.error(new IllegalArgumentException("Vehicle data cannot be null"));
        }
        return vehicleDataService.processVehicleData(data);
    }
} 