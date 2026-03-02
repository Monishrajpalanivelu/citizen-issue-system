package com.smartcity.issues.web.controller;

import com.smartcity.issues.config.JwtUtils;
import com.smartcity.issues.domain.entity.User;
import com.smartcity.issues.domain.enums.UserRole;
import com.smartcity.issues.domain.repository.UserRepository;
import com.smartcity.issues.web.dto.Dtos.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register and login")
public class AuthController {

    private final UserRepository userRepo;
    private final PasswordEncoder encoder;
    private final JwtUtils jwtUtils;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new citizen account")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        if (userRepo.existsByEmail(req.getEmail())) {
            return ApiResponse.error("Email already registered");
        }

        User user = User.builder()
            .fullName(req.getFullName())
            .email(req.getEmail())
            .password(encoder.encode(req.getPassword()))
            .role(UserRole.CITIZEN)
            .phone(req.getPhone())
            .isActive(true)
            .build();

        User saved = userRepo.save(user);
        String token = jwtUtils.generateToken(saved.getId(), saved.getEmail(), saved.getRole().name());

        return ApiResponse.ok("Registration successful",
            AuthResponse.builder()
                .token(token)
                .email(saved.getEmail())
                .fullName(saved.getFullName())
                .role(saved.getRole())
                .build());
    }

    @PostMapping("/login")
    @Operation(summary = "Login and receive JWT token")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest req) {
        return userRepo.findByEmail(req.getEmail())
            .filter(u -> encoder.matches(req.getPassword(), u.getPassword()))
            .filter(User::isActive)
            .map(user -> {
                String token = jwtUtils.generateToken(
                    user.getId(), user.getEmail(), user.getRole().name()
                );
                return ResponseEntity.ok(ApiResponse.ok(
                    AuthResponse.builder()
                        .token(token)
                        .email(user.getEmail())
                        .fullName(user.getFullName())
                        .role(user.getRole())
                        .message("Login successful")
                        .build()
                ));
            })
            .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Invalid credentials")));
    }
}
