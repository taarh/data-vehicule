package com.example.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.math.BigDecimal;
import java.time.Instant;

@Data
@Document
public class InsuranceContract {
    @Id
    private String id;
    private String vehicleId;
    private BigDecimal basePremium;
    private BigDecimal currentPremium;
    private Instant startDate;
    private Instant lastUpdated;
    private String status;
}