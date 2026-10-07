package com.stockflow.domain.dto.shoppinglist;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ShoppingListItemRequestDTO(
    @NotBlank @Size(max = 255) String name,
    @Positive BigDecimal quantity
) {}
