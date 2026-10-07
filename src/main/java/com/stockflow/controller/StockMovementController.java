package com.stockflow.controller;

import com.stockflow.domain.dto.common.ApiResponseDTO;
import com.stockflow.domain.dto.common.PageResponseDTO;
import com.stockflow.domain.dto.stock.StockAdjustmentRequestDTO;
import com.stockflow.domain.dto.stock.StockMovementResponseDTO;
import com.stockflow.security.JwtTokenProvider;
import com.stockflow.service.StockMovementService;
import com.stockflow.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stock-movements")
@RequiredArgsConstructor
@Tag(name = "Stock Movements", description = "Stock movement history and manual adjustments")
@SecurityRequirement(name = "bearerAuth")
public class StockMovementController {

    private final StockMovementService stockMovementService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/adjust")
    @Operation(summary = "Manual stock adjustment (entry, exit, return)")
    public ResponseEntity<ApiResponseDTO<StockMovementResponseDTO>> adjust(
        @Valid @RequestBody StockAdjustmentRequestDTO body,
        HttpServletRequest request) {
        var tenantId = SecurityUtils.getCurrentTenantId(jwtTokenProvider, request);
        var userId = SecurityUtils.getCurrentUserId(jwtTokenProvider, request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponseDTO.ok(stockMovementService.adjust(tenantId, userId, body)));
    }

    @GetMapping
    @Operation(summary = "List all movements for tenant")
    public ResponseEntity<ApiResponseDTO<PageResponseDTO<StockMovementResponseDTO>>> list(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        HttpServletRequest request) {
        var tenantId = SecurityUtils.getCurrentTenantId(jwtTokenProvider, request);
        var pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(ApiResponseDTO.ok(stockMovementService.listByTenant(tenantId, pageable)));
    }

    @GetMapping("/product/{productId}")
    @Operation(summary = "List movements for a specific product")
    public ResponseEntity<ApiResponseDTO<PageResponseDTO<StockMovementResponseDTO>>> listByProduct(
        @PathVariable UUID productId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        HttpServletRequest request) {
        var tenantId = SecurityUtils.getCurrentTenantId(jwtTokenProvider, request);
        var pageable = PageRequest.of(page, Math.min(size, 100));
        return ResponseEntity.ok(ApiResponseDTO.ok(stockMovementService.listByProduct(tenantId, productId, pageable)));
    }
}
