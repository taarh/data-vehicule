package com.example.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Data
@Document(collection = "vehicle_data")
@CompoundIndex(name = "vehicle_contract_time_uidx", def = "{ 'vehicleId': 1, 'contractId': 1, 'timestamp': 1 }", unique = true)
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
    @JsonIgnoreProperties(ignoreUnknown = true)
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
        
        @JsonProperty("BRAKE_PRESSURE")
        private Measurement brakePressure;
        
        private Instant timestamp;
        
        // Champ pour stocker les propriétés supplémentaires inconnues
        @JsonProperty("additionalProperties")
        private Map<String, Measurement> additionalProperties;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Measurement {
        private double value;
        private String unit;
    }
}