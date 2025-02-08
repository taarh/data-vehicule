package com.example.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document
public class Contrat {
    @Id
    private String id;
    private String clientId;
    private String type;
    private Double montant;
    private String statut;
} 