package com.example.service;

import com.example.model.RiskLevel;
import com.example.model.VehicleData;
import com.example.model.RiskAssessment;
import com.example.repository.VehicleDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehicleDataServiceTest {

    @Mock
    private VehicleDataRepository vehicleDataRepository;
    
    @Mock
    private RiskAssessmentService riskAssessmentService;

    private VehicleDataService vehicleDataService;

    @BeforeEach
    void setUp() {
        vehicleDataService = new VehicleDataService(vehicleDataRepository, riskAssessmentService);
        when(riskAssessmentService.assessRisk(any()))
            .thenReturn(Mono.just(createVehicleDataWithRiskAssessment()));
        when(vehicleDataRepository.save(any()))
            .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
    }

    private VehicleData createTestVehicleData() {
        VehicleData data = new VehicleData();
        data.setVehicleId("TEST-001");
        
        VehicleData.SensorData sensorData = new VehicleData.SensorData();
        sensorData.setSpeed(createMeasurement(60.0, "km/h"));
        sensorData.setRpm(createMeasurement(2500.0, "rpm"));
        sensorData.setTimestamp(Instant.now());
        
        data.setData(sensorData);
        return data;
    }

    private VehicleData createVehicleDataWithRiskAssessment() {
        VehicleData data = createTestVehicleData();
        RiskAssessment assessment = new RiskAssessment();
        assessment.setLevel(RiskLevel.LOW);
        assessment.setScore(2.0);
        //assessment.setTimestamp(System.currentTimeMillis());
        data.setRiskAssessment(assessment);
        return data;
    }

    private VehicleData.Measurement createMeasurement(double value, String unit) {
        VehicleData.Measurement measurement = new VehicleData.Measurement();
        measurement.setValue(value);
        measurement.setUnit(unit);
        return measurement;
    }

    @Test
    void shouldProcessVehicleData() {
        VehicleData testData = createTestVehicleData();

        StepVerifier.create(vehicleDataService.processVehicleData(testData))
            .expectNextMatches(data -> {
                assertThat(data.getVehicleId()).isEqualTo("TEST-001");
                assertThat(data.getRiskAssessment()).isNotNull();
                assertThat(data.getRiskAssessment().getLevel()).isEqualTo(RiskLevel.LOW);
                return true;
            })
            .verifyComplete();
    }
} 