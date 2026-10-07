package com.stockflow.domain.dto.shoppinglist;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ShoppingListItemResponseDTO(
    UUID id,
    UUID productId,
    String name,
    BigDecimal quantity,
    boolean checked,
    LocalDateTime createdAt
) {}
