package com.example.service;

import com.example.model.InsuranceContract;
import com.example.model.VehicleData;
import com.example.repository.InsuranceContractRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class InsuranceContractServiceTest {

    @Mock
    private InsuranceContractRepository contractRepository;

    private InsuranceContractService contractService;

    @BeforeEach
    void setUp() {
        contractService = new InsuranceContractService(contractRepository);
    }

    @Test
    void shouldUpdatePricingBasedOnRiskAssessment() {
        // Arrange
        String contractId = "CONTRACT-001";
        VehicleData vehicleData = createHighRiskVehicleData();
        InsuranceContract contract = createTestContract(contractId);

        when(contractRepository.findById(contractId))
            .thenReturn(Mono.just(contract));
        when(contractRepository.save(any(InsuranceContract.class)))
            .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        // Act & Assert
        StepVerifier.create(contractService.updatePricing(contractId, vehicleData))
            .expectNextMatches(updatedContract -> {
                assertThat(updatedContract.getId()).isEqualTo(contractId);
                assertThat(updatedContract.getLastUpdated()).isNotNull();
                assertThat(updatedContract.getCurrentPremium())
                    .isGreaterThan(updatedContract.getBasePremium());
                return true;
            })
            .verifyComplete();
    }

    private VehicleData createHighRiskVehicleData() {
        VehicleData data = new VehicleData();
        data.setVehicleId("VEH-001");
        data.setContractId("CONTRACT-001");
        
        VehicleData.SensorData sensorData = new VehicleData.SensorData();
        sensorData.setSpeed(createMeasurement(140.0, "km/h")); // High speed
        sensorData.setRpm(createMeasurement(4500.0, "rpm")); // High RPM
        sensorData.setEngineLoad(createMeasurement(85.0, "%")); // High load
        sensorData.setTimestamp(Instant.now());
        
        data.setData(sensorData);
        return data;
    }

    private VehicleData.Measurement createMeasurement(double value, String unit) {
        VehicleData.Measurement measurement = new VehicleData.Measurement();
        measurement.setValue(value);
        measurement.setUnit(unit);
        return measurement;
    }

    private InsuranceContract createTestContract(String contractId) {
        InsuranceContract contract = new InsuranceContract();
        contract.setId(contractId);
        contract.setVehicleId("VEH-001");
        contract.setBasePremium(new BigDecimal("1000.00"));
        contract.setCurrentPremium(new BigDecimal("1000.00"));
        contract.setStartDate(Instant.now());
        contract.setLastUpdated(Instant.now());
        return contract;
    }
} 