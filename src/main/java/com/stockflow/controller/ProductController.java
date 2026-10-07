package com.stockflow.controller;

import com.stockflow.domain.dto.common.ApiResponseDTO;
import com.stockflow.domain.dto.common.PageResponseDTO;
import com.stockflow.domain.dto.product.ProductFilterDTO;
import com.stockflow.domain.dto.product.ProductRequestDTO;
import com.stockflow.domain.dto.product.ProductResponseDTO;
import com.stockflow.domain.dto.stock.StockMovementResponseDTO;
import com.stockflow.domain.enums.MovementType;
import com.stockflow.security.JwtTokenProvider;
import com.stockflow.service.ProductService;
import com.stockflow.service.StockMovementService;
import com.stockflow.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product catalog and stock management")
@SecurityRequirement(name = "bearerAuth")
public class ProductController {

    private final ProductService productService;
    private final StockMovementService stockMovementService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping
    @Operation(summary = "Create new product")
    public ResponseEntity<ApiResponseDTO<ProductResponseDTO>> create(
        @Valid @RequestBody ProductRequestDTO body,
        HttpServletRequest request) {
        var tenantId = SecurityUtils.getCurrentTenantId(jwtTokenProvider, request);
        var userId = SecurityUtils.getCurrentUserId(jwtTokenProvider, request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponseDTO.ok(productService.create(tenantId, userId, body)));
    }

    @GetMapping
    @Operation(summary = "List products with filters and pagination")
    public ResponseEntity<ApiResponseDTO<PageResponseDTO<ProductResponseDTO>>> list(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "name") String sort,
        @RequestParam(required = false) String name,
        @RequestParam(required = false) String ean,
        @RequestParam(required = false) String category,
        @RequestParam(required = false) Boolean active,
        @RequestParam(required = false) Boolean belowMinimum,
        @RequestParam(required = false) Boolean mine,
        HttpServletRequest request) {
        var tenantId = SecurityUtils.getCurrentTenantId(jwtTokenProvider, request);
        UUID createdBy = Boolean.TRUE.equals(mine)
            ? SecurityUtils.getCurrentUserId(jwtTokenProvider, request)
            : null;
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(sort));
        ProductFilterDTO filter = new ProductFilterDTO(name, ean, category, active, belowMinimum, createdBy);
        return ResponseEntity.ok(ApiResponseDTO.ok(productService.list(tenantId, filter, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID")
    public ResponseEntity<ApiResponseDTO<ProductResponseDTO>> getById(
        @PathVariable UUID id,
        HttpServletRequest request) {
        var tenantId = SecurityUtils.getCurrentTenantId(jwtTokenProvider, request);
        return ResponseEntity.ok(ApiResponseDTO.ok(productService.getById(tenantId, id)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update product")
    public ResponseEntity<ApiResponseDTO<ProductResponseDTO>> update(
        @PathVariable UUID id,
        @Valid @RequestBody ProductRequestDTO body,
        HttpServletRequest request) {
        var tenantId = SecurityUtils.getCurrentTenantId(jwtTokenProvider, request);
        return ResponseEntity.ok(ApiResponseDTO.ok(productService.update(tenantId, id, body)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete product")
    public ResponseEntity<Void> delete(@PathVariable UUID id, HttpServletRequest request) {
        var tenantId = SecurityUtils.getCurrentTenantId(jwtTokenProvider, request);
        productService.delete(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/use")
    @Operation(summary = "\"Usei\" — 1-tap stock exit for consumption (default quantity: 1)")
    public ResponseEntity<ApiResponseDTO<StockMovementResponseDTO>> use(
        @PathVariable UUID id,
        @RequestParam(defaultValue = "1") BigDecimal quantity,
        HttpServletRequest request) {
        var tenantId = SecurityUtils.getCurrentTenantId(jwtTokenProvider, request);
        var userId = SecurityUtils.getCurrentUserId(jwtTokenProvider, request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponseDTO.ok(stockMovementService.quickExit(tenantId, userId, id, MovementType.USED, quantity)));
    }

    @PostMapping("/{id}/discard")
    @Operation(summary = "\"Descartei\" — 1-tap stock exit for waste/expired items (default quantity: 1)")
    public ResponseEntity<ApiResponseDTO<StockMovementResponseDTO>> discard(
        @PathVariable UUID id,
        @RequestParam(defaultValue = "1") BigDecimal quantity,
        HttpServletRequest request) {
        var tenantId = SecurityUtils.getCurrentTenantId(jwtTokenProvider, request);
        var userId = SecurityUtils.getCurrentUserId(jwtTokenProvider, request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponseDTO.ok(stockMovementService.quickExit(tenantId, userId, id, MovementType.DISCARDED, quantity)));
    }
}
