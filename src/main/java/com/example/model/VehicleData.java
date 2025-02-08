package com.example.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Data
@Document(collection = "vehicle_data")
public class VehicleData {
    @Id
    private String id;
    
    @JsonProperty("vehicle_id")
    private String vehicleId;
    
    private String contractId;
    private RiskAssessment riskAssessment;
    private SensorData data;
    private Instant timestamp;

    @Data
    public static class SensorData {
        @JsonProperty("SPEED")
        private Measurement speed;
        
        @JsonProperty("RPM")
        private Measurement rpm;
        
        @JsonProperty("THROTTLE_POS")
        private Measurement throttlePosition;
        
        @JsonProperty("FUEL_LEVEL")
        private Measurement fuelLevel;
        
        @JsonProperty("MAF")
        private Measurement maf;
        
        @JsonProperty("ENGINE_LOAD")
        private Measurement engineLoad;
        
        @JsonProperty("FUEL_RATE")
        private Measurement fuelRate;
        
        @JsonProperty("INTAKE_PRESSURE")
        private Measurement intakePressure;
        
        @JsonProperty("ACCELERATOR_POS_D")
        private Measurement acceleratorPosition;
        
        @JsonProperty("BAROMETRIC_PRESSURE")
        private Measurement barometricPressure;
        
        private Instant timestamp;
    }

    @Data
    public static class Measurement {
        private double value;
        private String unit;
    }
}