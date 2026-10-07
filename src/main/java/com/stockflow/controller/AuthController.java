package com.stockflow.controller;

import com.stockflow.domain.dto.auth.*;
import com.stockflow.domain.dto.common.ApiResponseDTO;
import com.stockflow.security.JwtTokenProvider;
import com.stockflow.service.AuthService;
import com.stockflow.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, login, token management")
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/register")
    @Operation(summary = "Register a new house + owner user")
    public ResponseEntity<ApiResponseDTO<LoginResponseDTO>> register(@Valid @RequestBody RegisterRequestDTO request) {
        LoginResponseDTO response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseDTO.ok("Registered successfully", response));
    }

    @PostMapping("/join")
    @Operation(summary = "Join an existing house using its invite code")
    public ResponseEntity<ApiResponseDTO<LoginResponseDTO>> join(@Valid @RequestBody JoinRequestDTO request) {
        LoginResponseDTO response = authService.join(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseDTO.ok("Joined house successfully", response));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and get JWT tokens")
    public ResponseEntity<ApiResponseDTO<LoginResponseDTO>> login(@Valid @RequestBody LoginRequestDTO request) {
        return ResponseEntity.ok(ApiResponseDTO.ok(authService.login(request)));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token using refresh token")
    public ResponseEntity<ApiResponseDTO<LoginResponseDTO>> refresh(@Valid @RequestBody RefreshTokenRequestDTO request) {
        return ResponseEntity.ok(ApiResponseDTO.ok(authService.refreshToken(request)));
    }

    @PostMapping("/logout")
    @Operation(summary = "Invalidate refresh token")
    public ResponseEntity<ApiResponseDTO<Void>> logout(HttpServletRequest request) {
        var userId = SecurityUtils.getCurrentUserId(jwtTokenProvider, request);
        authService.logout(userId);
        return ResponseEntity.ok(ApiResponseDTO.ok("Logged out successfully", null));
    }
}
