package com.stockflow.domain.dto.invoice;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NfceProcessRequestDTO(
    @NotBlank @Size(max = 2048) String qrCode
) {}
