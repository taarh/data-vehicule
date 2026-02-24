package com.example.controller;

import com.example.auth.AuthRequest;
import com.example.auth.AuthResponse;
import com.example.model.User;
import com.example.security.JwtService;
import com.example.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Login and token")
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;

    @PostMapping("/login")
    @Operation(summary = "Login with username and password", description = "Returns a JWT token to use in Authorization header: Bearer <token>")
    public Mono<ResponseEntity<AuthResponse>> login(@Valid @RequestBody AuthRequest request) {
        return userService.authenticate(request.getUsername(), request.getPassword())
                .map(user -> {
                    String token = jwtService.generateToken(user.getUsername());
                    return ResponseEntity.<AuthResponse>ok(new AuthResponse(token, "Bearer"));
                })
                .switchIfEmpty(Mono.just(ResponseEntity.<AuthResponse>status(HttpStatus.UNAUTHORIZED).build()));
    }
}
