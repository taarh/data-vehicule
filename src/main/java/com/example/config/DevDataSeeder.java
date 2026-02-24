package com.example.config;

import com.example.model.Client;
import com.example.model.Contrat;
import com.example.model.VehicleData;
import com.example.repository.ClientRepository;
import com.example.repository.ContratRepository;
import com.example.repository.VehicleDataRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.kafka.core.KafkaTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Dev profile: loads vehicle data from a JSON file, sends it to Kafka.
 * The existing Kafka consumer then inserts the data into MongoDB.
 * Also seeds clients and contrats directly (no Kafka flow for them in the app).
 */
@Configuration
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DevDataSeeder {

    private final ClientRepository clientRepository;
    private final ContratRepository contratRepository;
    private final VehicleDataRepository vehicleDataRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topic.vehicle-data:vehicle-data}")
    private String vehicleDataTopic;

    private static final String VEHICLE_DATA_FILE = "data/vehicle-data-dev.json";

    @Bean
    public CommandLineRunner seedDevData() {
        return args -> {
            log.info("Dev profile: seeding data (JSON -> Kafka -> MongoDB for vehicle data)...");
            seedClientsAndContrats()
                .then(sendVehicleDataFromJsonToKafka())
                .doOnSuccess(v -> log.info("Dev data seeding completed."))
                .doOnError(e -> log.error("Dev data seeding failed: {}", e.getMessage()))
                .block();
        };
    }

    private Mono<Void> seedClientsAndContrats() {
        return clientRepository.count()
                .filter(count -> count == 0)
                .flatMap(c -> clientRepository.deleteAll().then(contratRepository.deleteAll()))
                .then(Mono.defer(() -> Flux.range(1, 5)
                        .map(i -> {
                            Client client = new Client();
                            client.setNom("DevNom" + i);
                            client.setPrenom("DevPrenom" + i);
                            client.setEmail("dev" + i + "@example.com");
                            client.setTelephone("060000000" + i);
                            return client;
                        })
                        .flatMap(clientRepository::save)
                        .flatMap(client -> Flux.range(1, 2)
                                .map(j -> {
                                    Contrat contrat = new Contrat();
                                    contrat.setClientId(client.getId());
                                    contrat.setType("AUTO");
                                    contrat.setMontant(800.0 * j);
                                    contrat.setStatut("ACTIF");
                                    return contrat;
                                })
                                .flatMap(contratRepository::save))
                        .then()))
                .then()
                .onErrorResume(e -> Mono.empty());
    }

    /**
     * Sends vehicle data from JSON to Kafka only when vehicle_data collection is empty.
     * Avoids duplicates on every app restart.
     */
    private Mono<Void> sendVehicleDataFromJsonToKafka() {
        return vehicleDataRepository.count()
                .filter(count -> count == 0)
                .flatMap(c -> Mono.fromCallable(() -> {
                    ObjectMapper mapper = objectMapper.copy();
                    ClassPathResource resource = new ClassPathResource(VEHICLE_DATA_FILE);
                    if (!resource.exists()) {
                        log.warn("Dev data file not found: {}", VEHICLE_DATA_FILE);
                        return 0;
                    }
                    try (InputStream is = resource.getInputStream()) {
                        List<VehicleData> list = mapper.readValue(is, new TypeReference<>() {});
                        for (VehicleData data : list) {
                            String payload = mapper.writeValueAsString(data);
                            kafkaTemplate.send(vehicleDataTopic, payload).get(10, TimeUnit.SECONDS);
                            log.debug("Sent vehicle data to Kafka: vehicleId={}", data.getVehicleId());
                        }
                        log.info("Sent {} vehicle data record(s) from {} to Kafka (collection was empty)", list.size(), VEHICLE_DATA_FILE);
                        return list.size();
                    }
                }))
                .then()
                .onErrorResume(e -> {
                    log.error("Failed to send vehicle data from JSON to Kafka: {}", e.getMessage());
                    return Mono.empty();
                });
    }
}
