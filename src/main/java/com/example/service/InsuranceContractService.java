package com.example.service;

import com.example.model.InsuranceContract;
import com.example.model.VehicleData;
import com.example.repository.InsuranceContractRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.math.BigDecimal;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class InsuranceContractService {
    private final InsuranceContractRepository contractRepository;

    public Mono<InsuranceContract> createContract(InsuranceContract contract) {
        contract.setLastUpdated(Instant.now());
        if (contract.getStartDate() == null) {
            contract.setStartDate(Instant.now());
        }
        if (contract.getCurrentPremium() == null) {
            contract.setCurrentPremium(contract.getBasePremium());
        }
        return contractRepository.save(contract);
    }

    public Flux<InsuranceContract> getClientContracts(String clientId) {
        return contractRepository.findByVehicleId(clientId);
    }

    public Mono<InsuranceContract> updatePricing(String contractId, VehicleData vehicleData) {
        return contractRepository.findById(contractId)
            .map(contract -> {
                contract.setLastUpdated(Instant.now());
                contract.setCurrentPremium(calculateAdjustedPremium(contract, vehicleData));
                return contract;
            })
            .flatMap(contractRepository::save);
    }

    private BigDecimal calculateAdjustedPremium(InsuranceContract contract, VehicleData vehicleData) {
        double riskMultiplier = calculateRiskMultiplier(vehicleData);
        return contract.getBasePremium().multiply(BigDecimal.valueOf(riskMultiplier));
    }

    private double calculateRiskMultiplier(VehicleData vehicleData) {
        if (vehicleData.getData() == null) {
            return 1.0;
        }

        double speedFactor = getSpeedFactor(vehicleData.getData().getSpeed());
        double rpmFactor = getRpmFactor(vehicleData.getData().getRpm());
        double engineLoadFactor = getEngineLoadFactor(vehicleData.getData().getEngineLoad());

        return Math.max(1.0, speedFactor * rpmFactor * engineLoadFactor);
    }

    private double getSpeedFactor(VehicleData.Measurement speed) {
        if (speed == null) return 1.0;
        double value = speed.getValue();
        if (value > 130) return 1.5;
        if (value > 110) return 1.3;
        if (value > 90) return 1.1;
        return 1.0;
    }

    private double getRpmFactor(VehicleData.Measurement rpm) {
        if (rpm == null) return 1.0;
        double value = rpm.getValue();
        if (value > 4000) return 1.3;
        if (value > 3000) return 1.1;
        return 1.0;
    }

    private double getEngineLoadFactor(VehicleData.Measurement engineLoad) {
        if (engineLoad == null) return 1.0;
        double value = engineLoad.getValue();
        if (value > 80) return 1.3;
        if (value > 60) return 1.1;
        return 1.0;
    }
} 