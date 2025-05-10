package com.gayale.transport.controller;

import com.gayale.transport.dto.auth.LoginRequest;
import com.gayale.transport.dto.auth.LoginResponse;
import com.gayale.transport.dto.auth.RefreshTokenRequest;
import com.gayale.transport.dto.auth.RefreshTokenResponse;
import com.gayale.transport.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "API for user authentication and token management")
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and generate JWT token",
            description = "Takes username and password, returns JWT token with user details")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        LoginResponse response = authService.login(loginRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Refresh JWT token",
            description = "Takes a refresh token and returns a new JWT token")
    public ResponseEntity<RefreshTokenResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        RefreshTokenResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user",
            description = "Invalidates the user's refresh token")
    public ResponseEntity<Void> logout(@RequestParam String userId) {
        authService.logout(userId);
        return ResponseEntity.ok().build();
    }
}