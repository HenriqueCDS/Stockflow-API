package com.stockflow.controller;

import com.stockflow.domain.dto.common.ApiResponseDTO;
import com.stockflow.domain.dto.shoppinglist.ShoppingListItemRequestDTO;
import com.stockflow.domain.dto.shoppinglist.ShoppingListItemResponseDTO;
import com.stockflow.security.JwtTokenProvider;
import com.stockflow.service.ShoppingListService;
import com.stockflow.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shopping-list")
@RequiredArgsConstructor
@Tag(name = "Shopping List", description = "Shared shopping list: below-minimum products + manual items")
@SecurityRequirement(name = "bearerAuth")
public class ShoppingListController {

    private final ShoppingListService shoppingListService;
    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping
    @Operation(summary = "List open shopping list items (below-minimum products + manual items)")
    public ResponseEntity<ApiResponseDTO<List<ShoppingListItemResponseDTO>>> list(HttpServletRequest request) {
        var tenantId = SecurityUtils.getCurrentTenantId(jwtTokenProvider, request);
        return ResponseEntity.ok(ApiResponseDTO.ok(shoppingListService.list(tenantId)));
    }

    @PostMapping
    @Operation(summary = "Add a manual item to the shopping list")
    public ResponseEntity<ApiResponseDTO<ShoppingListItemResponseDTO>> addManualItem(
        @Valid @RequestBody ShoppingListItemRequestDTO body,
        HttpServletRequest request) {
        var tenantId = SecurityUtils.getCurrentTenantId(jwtTokenProvider, request);
        var userId = SecurityUtils.getCurrentUserId(jwtTokenProvider, request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponseDTO.ok(shoppingListService.addManualItem(tenantId, userId, body)));
    }

    @PostMapping("/{id}/check")
    @Operation(summary = "Mark an item as bought (strike it off the list)")
    public ResponseEntity<Void> check(@PathVariable UUID id, HttpServletRequest request) {
        var tenantId = SecurityUtils.getCurrentTenantId(jwtTokenProvider, request);
        shoppingListService.checkItem(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove an item from the shopping list")
    public ResponseEntity<Void> delete(@PathVariable UUID id, HttpServletRequest request) {
        var tenantId = SecurityUtils.getCurrentTenantId(jwtTokenProvider, request);
        shoppingListService.deleteItem(tenantId, id);
        return ResponseEntity.noContent().build();
    }
}
