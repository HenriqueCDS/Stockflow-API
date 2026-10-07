package com.stockflow.domain.dto.invoice;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Revisao de um item da nota antes de confirmar. Todos os campos sao
 * opcionais; so o que vier preenchido e aplicado (ver InvoiceService.reviewItem).
 */
public record InvoiceItemReviewRequestDTO(
    @Size(max = 255) String productName,
    UUID mergeIntoProductId,
    @Positive BigDecimal quantity,
    Boolean ignored
) {}
