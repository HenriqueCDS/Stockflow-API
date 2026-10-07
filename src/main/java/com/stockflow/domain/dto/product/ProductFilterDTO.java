package com.stockflow.domain.dto.product;

import java.util.UUID;

public record ProductFilterDTO(
    String name,
    String ean,
    String category,
    Boolean active,
    Boolean belowMinimum,
    UUID createdBy
) {}
