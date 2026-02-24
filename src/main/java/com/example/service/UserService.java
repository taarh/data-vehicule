package com.example.service;

import com.example.model.User;
import com.example.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public Mono<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Validates credentials. Password in DB is stored hashed with BCrypt (salt included in hash).
     */
    public Mono<User> authenticate(String username, String rawPassword) {
        return userRepository.findByUsername(username)
                .filter(user -> passwordEncoder.matches(rawPassword, user.getPassword()));
    }

    public Mono<User> createUser(String username, String rawPassword, String... roles) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword)); // BCrypt hash with salt
        if (roles != null && roles.length > 0) {
            user.setRoles(java.util.Arrays.asList(roles));
        }
        return userRepository.save(user);
    }
}
