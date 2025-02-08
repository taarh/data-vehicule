package com.example.controller;

import com.example.model.VehicleData;
import com.example.service.VehicleDataService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@WebFluxTest(VehicleDataController.class)
class VehicleDataControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private VehicleDataService vehicleDataService;

    @Test
    void shouldGetLatestVehicleData() {
        String vehicleId = "TEST-001";
        VehicleData testData = createTestVehicleData(vehicleId);
        
        when(vehicleDataService.getLatestVehicleData(vehicleId))
            .thenReturn(Mono.just(testData));

        webTestClient.get()
            .uri("/api/vehicle-data/{vehicleId}/latest", vehicleId)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.vehicleId").isEqualTo(vehicleId);
    }

    @Test
    void shouldReturn404WhenNoLatestDataFound() {
        String vehicleId = "NONEXISTENT";
        
        when(vehicleDataService.getLatestVehicleData(vehicleId))
            .thenReturn(Mono.empty());

        webTestClient.get()
            .uri("/api/vehicle-data/{vehicleId}/latest", vehicleId)
            .exchange()
            .expectStatus().isNotFound();
    }

    @Test
    void shouldGetVehicleDataHistory() {
        String vehicleId = "TEST-001";
        VehicleData testData = createTestVehicleData(vehicleId);
        
        when(vehicleDataService.getVehicleDataHistory(eq(vehicleId), any(PageRequest.class)))
            .thenReturn(Flux.just(testData));

        webTestClient.get()
            .uri("/api/vehicle-data/{vehicleId}?page=0&size=20", vehicleId)
            .exchange()
            .expectStatus().isOk()
            .expectBodyList(VehicleData.class)
            .hasSize(1);
    }

    @Test
    void shouldGetVehicleDataByTimeRange() {
        String vehicleId = "TEST-001";
        VehicleData testData = createTestVehicleData(vehicleId);
        Instant startTime = Instant.parse("2024-01-01T00:00:00Z");
        Instant endTime = Instant.parse("2024-01-02T00:00:00Z");
        
        when(vehicleDataService.getVehicleDataByTimeRange(vehicleId, startTime, endTime))
            .thenReturn(Flux.just(testData));

        webTestClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/api/vehicle-data/{vehicleId}/timerange")
                .queryParam("startTime", startTime.toString())
                .queryParam("endTime", endTime.toString())
                .build(vehicleId))
            .exchange()
            .expectStatus().isOk()
            .expectBodyList(VehicleData.class)
            .hasSize(1);
    }

    @Test
    void shouldGetVehicleDataCount() {
        String vehicleId = "TEST-001";
        long expectedCount = 10L;
        
        when(vehicleDataService.getVehicleDataCount(vehicleId))
            .thenReturn(Mono.just(expectedCount));

        webTestClient.get()
            .uri("/api/vehicle-data/{vehicleId}/count", vehicleId)
            .exchange()
            .expectStatus().isOk()
            .expectBody(Long.class)
            .isEqualTo(expectedCount);
    }

    private VehicleData createTestVehicleData(String vehicleId) {
        VehicleData data = new VehicleData();
        data.setVehicleId(vehicleId);
        data.setTimestamp(Instant.now());
        
        VehicleData.SensorData sensorData = new VehicleData.SensorData();
        sensorData.setSpeed(createMeasurement(60.0, "km/h"));
        sensorData.setRpm(createMeasurement(2500.0, "rpm"));
        data.setData(sensorData);
        
        return data;
    }

    private VehicleData.Measurement createMeasurement(double value, String unit) {
        VehicleData.Measurement measurement = new VehicleData.Measurement();
        measurement.setValue(value);
        measurement.setUnit(unit);
        return measurement;
    }
} 