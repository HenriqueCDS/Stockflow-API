package com.stockflow.controller;

import com.stockflow.domain.dto.common.ApiResponseDTO;
import com.stockflow.domain.dto.company.CompanyRequestDTO;
import com.stockflow.domain.dto.company.CompanyResponseDTO;
import com.stockflow.domain.dto.membership.InviteCodeResponseDTO;
import com.stockflow.domain.dto.membership.MemberResponseDTO;
import com.stockflow.security.JwtTokenProvider;
import com.stockflow.service.CompanyService;
import com.stockflow.service.MembershipService;
import com.stockflow.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/company")
@RequiredArgsConstructor
@Tag(name = "Company", description = "House management: profile, members and invite code")
@SecurityRequirement(name = "bearerAuth")
public class CompanyController {

    private final CompanyService companyService;
    private final MembershipService membershipService;
    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping
    @Operation(summary = "Get current house data (includes the invite code)")
    public ResponseEntity<ApiResponseDTO<CompanyResponseDTO>> get(HttpServletRequest request) {
        var tenantId = SecurityUtils.getCurrentTenantId(jwtTokenProvider, request);
        return ResponseEntity.ok(ApiResponseDTO.ok(companyService.getByTenantId(tenantId)));
    }

    @PutMapping
    @Operation(summary = "Update house data")
    public ResponseEntity<ApiResponseDTO<CompanyResponseDTO>> update(
        @Valid @RequestBody CompanyRequestDTO body,
        HttpServletRequest request) {
        var tenantId = SecurityUtils.getCurrentTenantId(jwtTokenProvider, request);
        return ResponseEntity.ok(ApiResponseDTO.ok(companyService.update(tenantId, body)));
    }

    @GetMapping("/members")
    @Operation(summary = "List the house members")
    public ResponseEntity<ApiResponseDTO<List<MemberResponseDTO>>> listMembers(HttpServletRequest request) {
        var tenantId = SecurityUtils.getCurrentTenantId(jwtTokenProvider, request);
        return ResponseEntity.ok(ApiResponseDTO.ok(membershipService.listMembers(tenantId)));
    }

    @DeleteMapping("/members/{userId}")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Remove a member from the house (owner only)")
    public ResponseEntity<Void> removeMember(@PathVariable UUID userId, HttpServletRequest request) {
        var tenantId = SecurityUtils.getCurrentTenantId(jwtTokenProvider, request);
        var requesterId = SecurityUtils.getCurrentUserId(jwtTokenProvider, request);
        membershipService.removeMember(tenantId, requesterId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/invite-code/rotate")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Generate a new invite code for the house, invalidating the previous one (owner only)")
    public ResponseEntity<ApiResponseDTO<InviteCodeResponseDTO>> rotateInviteCode(HttpServletRequest request) {
        var tenantId = SecurityUtils.getCurrentTenantId(jwtTokenProvider, request);
        return ResponseEntity.ok(ApiResponseDTO.ok(membershipService.rotateInviteCode(tenantId)));
    }
}
