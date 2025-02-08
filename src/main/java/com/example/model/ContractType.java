package com.example.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDate;



public enum ContractType {
    PAY_PER_KM,
    PAY_PER_USE,
    STANDARD
}
