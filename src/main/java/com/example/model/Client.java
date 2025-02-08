package com.example.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document
public class Client {
    @Id
    private String id;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
} 