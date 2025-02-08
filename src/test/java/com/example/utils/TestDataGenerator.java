package com.example.utils;

import com.example.model.Client;
import com.example.model.Contrat;
import com.example.repository.ClientRepository;
import com.example.repository.ContratRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import reactor.core.publisher.Flux;

@Configuration
@Profile("test")
public class TestDataGenerator {

    @Bean
    public CommandLineRunner generateTestData(ClientRepository clientRepository, 
                                           ContratRepository contratRepository) {
        return args -> {
            // Supprimer les données existantes
            clientRepository.deleteAll().block();
            contratRepository.deleteAll().block();

            // Générer 10 clients
            Flux.range(1, 10)
                .map(i -> {
                    Client client = new Client();
                    client.setNom("Nom" + i);
                    client.setPrenom("Prenom" + i);
                    client.setEmail("client" + i + "@example.com");
                    client.setTelephone("0600000" + i);
                    return client;
                })
                .flatMap(clientRepository::save)
                .flatMap(client -> {
                    // Générer 2 contrats par client
                    return Flux.range(1, 2)
                        .map(j -> {
                            Contrat contrat = new Contrat();
                            contrat.setClientId(client.getId());
                            contrat.setType("Type" + j);
                            contrat.setMontant(1000.0 * j);
                            contrat.setStatut("ACTIF");
                            return contrat;
                        })
                        .flatMap(contratRepository::save);
                })
                .blockLast();
        };
    }
} 