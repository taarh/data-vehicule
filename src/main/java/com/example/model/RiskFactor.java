package com.example.model;

import lombok.Data;

@Data
public class RiskFactor {
    private RiskFactorType type;
    private double value;
    private String unit;
    private double threshold;
    private double weight;
    private String description;
} 