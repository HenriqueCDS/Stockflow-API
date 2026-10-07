package com.stockflow.domain.dto.invoice;

import java.math.BigDecimal;
import java.util.UUID;

public record InvoiceItemDTO(
    UUID id,
    String productName,
    String productEan,
    UUID productId,
    BigDecimal quantity,
    BigDecimal unitValue,
    BigDecimal totalValue,
    String unit,
    boolean ignored
) {}
