package com.example.service;

import com.example.model.*;
import com.example.repository.RiskEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class RiskAssessmentServiceTest {

    @Mock
    private RiskEventRepository riskEventRepository;

    @Mock
    private InsuranceContractService insuranceContractService;

    private RiskAssessmentService riskAssessmentService;

    @BeforeEach
    void setUp() {
        riskAssessmentService = new RiskAssessmentService(riskEventRepository, insuranceContractService);
        // Use lenient() to avoid unnecessary stubbing warnings
        lenient().when(riskEventRepository.save(any(RiskEvent.class)))
            .thenReturn(Mono.just(new RiskEvent()));
    }

    private VehicleData createTestData(double speed, double rpm) {
        VehicleData data = new VehicleData();
        VehicleData.SensorData sensorData = new VehicleData.SensorData();
        sensorData.setSpeed(createMeasurement(speed, "km/h"));
        sensorData.setRpm(createMeasurement(rpm, "rpm"));
        data.setData(sensorData);
        return data;
    }

    private VehicleData.Measurement createMeasurement(double value, String unit) {
        VehicleData.Measurement measurement = new VehicleData.Measurement();
        measurement.setValue(value);
        measurement.setUnit(unit);
        return measurement;
    }

/*    @Test
    void shouldAssessLowRisk() {
        VehicleData data = createTestData(60.0, 2000.0);
        
        StepVerifier.create(riskAssessmentService.assessRisk(data))
            .expectNextMatches(result -> {
                RiskAssessment assessment = result.getRiskAssessment();
                return assessment.getLevel() == RiskLevel.LOW &&
                       assessment.getScore() < 5.0;
            })
            .verifyComplete();
    }

    @Test
    void shouldAssessModerateRisk() {
        VehicleData data = createTestData(100.0, 3500.0);
        
        StepVerifier.create(riskAssessmentService.assessRisk(data))
            .expectNextMatches(result -> {
                RiskAssessment assessment = result.getRiskAssessment();
                return assessment.getLevel() == RiskLevel.MEDIUM &&
                       assessment.getScore() >= 5.0 &&
                       assessment.getScore() < 8.0;
            })
            .verifyComplete();
    }

    @Test
    void shouldAssessHighRisk() {
        VehicleData data = createTestData(150.0, 5000.0);
        
        StepVerifier.create(riskAssessmentService.assessRisk(data))
            .expectNextMatches(result -> {
                RiskAssessment assessment = result.getRiskAssessment();
                return assessment.getLevel() == RiskLevel.HIGH &&
                       assessment.getScore() >= 8.0;
            })
            .verifyComplete();
    }
    */

} 