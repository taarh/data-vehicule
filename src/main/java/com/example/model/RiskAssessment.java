package com.example.model;

import lombok.Data;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
public class RiskAssessment {
    private RiskLevel level;
    private double score;
    private Instant timestamp;
    private List<RiskFactor> riskFactors = new ArrayList<>();
    private InsuranceImpact insuranceImpact;
} 