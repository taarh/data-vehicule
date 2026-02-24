package com.example.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * User for authentication. Password is stored hashed with BCrypt (salt is part of the hash).
 */
@Data
@Document(collection = "users")
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    private String username;

    /**
     * BCrypt hash (includes salt). Never store plain password.
     */
    private String password;

    private List<String> roles = new ArrayList<>();
}
