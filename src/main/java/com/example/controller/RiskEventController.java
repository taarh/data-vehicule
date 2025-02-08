package com.example.controller;

import com.example.model.RiskEvent;
import com.example.model.RiskEventType;
import com.example.repository.RiskEventRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/risk-events")
@RequiredArgsConstructor
@Tag(name = "Risk Event API", description = "API pour consulter les événements à risque")
public class RiskEventController {
    private final RiskEventRepository riskEventRepository;

    @GetMapping("/vehicle/{vehicleId}")
    @Operation(summary = "Obtenir tous les événements à risque d'un véhicule")
    public Flux<RiskEvent> getVehicleRiskEvents(@PathVariable String vehicleId) {
        return riskEventRepository.findByVehicleId(vehicleId);
    }

    @GetMapping("/vehicle/{vehicleId}/type/{type}")
    @Operation(summary = "Obtenir les événements à risque d'un véhicule par type")
    public Flux<RiskEvent> getVehicleRiskEventsByType(
            @PathVariable String vehicleId,
            @PathVariable RiskEventType type) {
        return riskEventRepository.findByVehicleIdAndType(vehicleId, type);
    }
} 