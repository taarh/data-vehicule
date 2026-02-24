package com.example.service;

import com.example.model.*;
import com.example.repository.RiskEventRepository;
import com.example.model.RiskAssessment;
import com.example.model.RiskFactor;
import com.example.model.RiskFactorType;
import com.example.model.InsuranceImpact;
import com.example.service.InsuranceContractService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class RiskAssessmentService {

    private final RiskEventRepository riskEventRepository;
    private final InsuranceContractService insuranceContractService;

    public Mono<VehicleData> assessRisk(VehicleData data) {
        RiskAssessment assessment = new RiskAssessment();
        List<RiskFactor> factors = evaluateRiskFactors(data);
        
        double totalScore = calculateRiskScore(factors);
        RiskLevel level = determineRiskLevel(totalScore);
        
        assessment.setRiskFactors(factors);
        assessment.setScore(totalScore);
        assessment.setLevel(level);
        assessment.setInsuranceImpact(calculateInsuranceImpact(totalScore, data.getContractId()));
        
        data.setRiskAssessment(assessment);
        
        if (level == RiskLevel.HIGH) {
            return createRiskEvent(data).thenReturn(data);
        }
        
        return Mono.just(data);
    }

    private double calculateRiskScore(List<RiskFactor> factors) {
        double totalScore = 0.0;
        for (RiskFactor factor : factors) {
            double baseScore = (factor.getValue() - factor.getThreshold()) / factor.getThreshold();
            totalScore += baseScore * factor.getWeight();
        }
        return totalScore;
    }

    private RiskLevel determineRiskLevel(double score) {
        if (score >= 0.8) return RiskLevel.HIGH;
        if (score >= 0.4) return RiskLevel.MEDIUM;
        return RiskLevel.LOW;
    }

    private List<RiskFactor> evaluateRiskFactors(VehicleData data) {
        List<RiskFactor> factors = new ArrayList<>();
        VehicleData.SensorData sensorData = data.getData();
        
        if (sensorData == null) {
            return factors;
        }

        addSpeedRiskFactor(factors, sensorData);
        addRpmRiskFactor(factors, sensorData);
        addEngineLoadRiskFactor(factors, sensorData);
        addThrottleRiskFactor(factors, sensorData);
        
        return factors;
    }

    private void addSpeedRiskFactor(List<RiskFactor> factors, VehicleData.SensorData data) {
        double speed = getValueOrDefault(data.getSpeed(), 0.0);
        if (speed > 90) {
            RiskFactor factor = new RiskFactor();
            factor.setType(RiskFactorType.SPEED_VIOLATION);
            factor.setValue(speed);
            factor.setUnit("km/h");
            factor.setThreshold(90.0);
            factor.setWeight(1.5);  // Adjusted weight
            factor.setDescription("Excessive speed detected");
            factors.add(factor);
        }
    }

    private void addRpmRiskFactor(List<RiskFactor> factors, VehicleData.SensorData data) {
        double rpm = getValueOrDefault(data.getRpm(), 0.0);
        if (rpm > 3000) {
            RiskFactor factor = new RiskFactor();
            factor.setType(RiskFactorType.HIGH_RPM);
            factor.setValue(rpm);
            factor.setUnit("rpm");
            factor.setThreshold(3000.0);
            factor.setWeight(1.0);  // Adjusted weight
            factor.setDescription("High engine RPM");
            factors.add(factor);
        }
    }

    private void addEngineLoadRiskFactor(List<RiskFactor> factors, VehicleData.SensorData data) {
        double load = getValueOrDefault(data.getEngineLoad(), 0.0);
        if (load > 60) {
            RiskFactor factor = new RiskFactor();
            factor.setType(RiskFactorType.ENGINE_STRESS);
            factor.setValue(load);
            factor.setUnit("%");
            factor.setThreshold(60.0);
            factor.setWeight(1);
            factor.setDescription("High engine load");
            factors.add(factor);
        }
    }

    private void addThrottleRiskFactor(List<RiskFactor> factors, VehicleData.SensorData data) {
        double throttle = getValueOrDefault(data.getThrottlePosition(), 0.0);
        if (throttle > 80) {
            RiskFactor factor = new RiskFactor();
            factor.setType(RiskFactorType.AGGRESSIVE_ACCELERATION);
            factor.setValue(throttle);
            factor.setUnit("%");
            factor.setThreshold(80.0);
            factor.setWeight(1);
            factor.setDescription("Aggressive throttle usage");
            factors.add(factor);
        }
    }

    private InsuranceImpact calculateInsuranceImpact(double riskScore, String contractId) {
        InsuranceImpact impact = new InsuranceImpact();
        
        if (riskScore >= 0.8) {
            impact.setPremiumAdjustment(new BigDecimal("0.15")); // 15% increase
            impact.setRequiresImmediate(true);
            impact.setRecommendedAction("Immediate review required");
            impact.setJustification("High risk driving behavior detected");
        } else if (riskScore >= 0.4) {
            impact.setPremiumAdjustment(new BigDecimal("0.08")); // 8% increase
            impact.setRecommendedAction("Schedule driver training");
            impact.setJustification("Moderate risk driving patterns observed");
        } else {
            impact.setPremiumAdjustment(BigDecimal.ZERO);
            impact.setRecommendedAction("Continue monitoring");
            impact.setJustification("Low risk driving behavior");
        }
        
        return impact;
    }

    private Mono<RiskEvent> createRiskEvent(VehicleData data) {
        Instant timestamp = data.getTimestamp() != null ? data.getTimestamp() : Instant.now();
        String contractId = data.getContractId() != null ? data.getContractId() : "";
        return riskEventRepository
                .findFirstByVehicleIdAndContractIdAndTimestamp(data.getVehicleId(), contractId, timestamp)
                .flatMap(existing -> Mono.just(existing))
                .switchIfEmpty(Mono.defer(() -> {
                    RiskEvent event = new RiskEvent();
                    event.setVehicleId(data.getVehicleId());
                    event.setContractId(data.getContractId());
                    event.setType(RiskEventType.SPEEDING);
                    event.setSeverity(data.getRiskAssessment().getScore() / 10.0);
                    event.setTimestamp(timestamp);
                    event.setDescription("High risk detected: score " + data.getRiskAssessment().getScore() + ", level " + data.getRiskAssessment().getLevel());
                    return riskEventRepository.save(event);
                }));
    }

    private double getValueOrDefault(VehicleData.Measurement measurement, double defaultValue) {
        return measurement != null ? measurement.getValue() : defaultValue;
    }
} 