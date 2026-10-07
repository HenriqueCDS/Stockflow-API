package com.stockflow.domain.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserProfileRequestDTO(
    @NotBlank @Size(max = 100) String name
) {}
