package com.interview.platform.controller;

import com.interview.platform.model.User;
import com.interview.platform.repository.UserRepository;
import com.interview.platform.security.JwtService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(
            UserRepository userRepository,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {

        if (request.name() == null || request.name().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Name is required"));
        }

        if (request.email() == null || request.email().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Email is required"));
        }

        if (request.password() == null || request.password().length() < 6) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            "Password must be at least 6 characters"
                    ));
        }

        String email = request.email()
                .trim()
                .toLowerCase();

        Optional<User> existingUser =
                userRepository.findByEmail(email);

        if (existingUser.isPresent()) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            "Email already registered"
                    ));
        }

        User user = new User(
                request.name().trim(),
                email,
                passwordEncoder.encode(request.password())
        );

        User savedUser =
                userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "message", "Registration successful",
                "userId", savedUser.getId(),
                "name", savedUser.getName(),
                "email", savedUser.getEmail()
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {

        if (request.email() == null
                || request.email().isBlank()
                || request.password() == null
                || request.password().isBlank()) {

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            "Email and password are required"
                    ));
        }

        String email = request.email()
                .trim()
                .toLowerCase();

        Optional<User> optionalUser =
                userRepository.findByEmail(email);

        if (optionalUser.isEmpty()) {
            return ResponseEntity.status(401)
                    .body(Map.of(
                            "message",
                            "Invalid email or password"
                    ));
        }

        User user = optionalUser.get();

        if (user.getPassword() == null
                || !passwordEncoder.matches(
                        request.password(),
                        user.getPassword()
                )) {

            return ResponseEntity.status(401)
                    .body(Map.of(
                            "message",
                            "Invalid email or password"
                    ));
        }

        String token =
                jwtService.generateToken(user.getEmail());

        return ResponseEntity.ok(Map.of(
                "message", "Login successful",
                "userId", user.getId(),
                "name", user.getName(),
                "email", user.getEmail(),
                "token", token
        ));
    }

    public record RegisterRequest(
            String name,
            String email,
            String password
    ) {
    }

    public record LoginRequest(
            String email,
            String password
    ) {
    }
}