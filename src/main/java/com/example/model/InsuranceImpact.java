package com.example.model;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class InsuranceImpact {
    private BigDecimal premiumAdjustment;
    private BigDecimal deductibleAdjustment;
    private String recommendedAction;
    private boolean requiresImmediate;
    private String justification;
} 