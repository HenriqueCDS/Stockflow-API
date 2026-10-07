package com.stockflow.domain.dto.product;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ProductResponseDTO(
    UUID id,
    String name,
    String ean,
    String category,
    String unit,
    BigDecimal currentStock,
    BigDecimal averageCost,
    BigDecimal minimumStock,
    BigDecimal totalValue,
    boolean active,
    boolean belowMinimum,
    LocalDateTime createdAt,
    UUID createdBy
) {}
