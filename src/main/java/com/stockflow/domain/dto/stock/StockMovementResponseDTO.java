package com.stockflow.domain.dto.stock;

import com.stockflow.domain.enums.MovementType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record StockMovementResponseDTO(
    UUID id,
    UUID productId,
    String productName,
    MovementType type,
    BigDecimal quantity,
    BigDecimal stockBefore,
    BigDecimal stockAfter,
    String reference,
    String notes,
    UUID createdBy,
    LocalDateTime createdAt
) {}
