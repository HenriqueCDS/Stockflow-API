package com.stockflow.domain.dto.stock;

import com.stockflow.domain.enums.MovementType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record StockAdjustmentRequestDTO(
    @NotNull UUID productId,
    @NotNull MovementType type,
    @NotNull @Positive BigDecimal quantity,
    String notes
) {}
