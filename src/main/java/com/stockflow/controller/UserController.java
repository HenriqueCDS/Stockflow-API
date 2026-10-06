package com.stockflow.controller;

import com.stockflow.domain.dto.common.ApiResponseDTO;
import com.stockflow.domain.dto.user.UserProfileRequestDTO;
import com.stockflow.domain.dto.user.UserProfileResponseDTO;
import com.stockflow.security.JwtTokenProvider;
import com.stockflow.service.UserProfileService;
import com.stockflow.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "Profile of the authenticated user")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserProfileService userProfileService;
    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping
    @Operation(summary = "Get authenticated user's profile")
    public ResponseEntity<ApiResponseDTO<UserProfileResponseDTO>> getProfile(HttpServletRequest request) {
        var tenantId = SecurityUtils.getCurrentTenantId(jwtTokenProvider, request);
        var userId = SecurityUtils.getCurrentUserId(jwtTokenProvider, request);
        return ResponseEntity.ok(ApiResponseDTO.ok(userProfileService.getProfile(tenantId, userId)));
    }

    @PutMapping
    @Operation(summary = "Update authenticated user's profile (name)")
    public ResponseEntity<ApiResponseDTO<UserProfileResponseDTO>> updateProfile(
        @Valid @RequestBody UserProfileRequestDTO body,
        HttpServletRequest request) {
        var tenantId = SecurityUtils.getCurrentTenantId(jwtTokenProvider, request);
        var userId = SecurityUtils.getCurrentUserId(jwtTokenProvider, request);
        return ResponseEntity.ok(ApiResponseDTO.ok(userProfileService.updateProfile(tenantId, userId, body)));
    }
}
