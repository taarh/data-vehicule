package com.example.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Document
public class RiskEvent {
    @Id
    private String id;
    private String vehicleId;
    private String contractId;
    private RiskEventType type;
    private Double severity;
    private Instant timestamp;
    private String description;
}
