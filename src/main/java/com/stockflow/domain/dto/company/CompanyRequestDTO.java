package com.stockflow.domain.dto.company;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompanyRequestDTO(
    @NotBlank @Size(min = 2, max = 100) String name,
    String email,
    String phone,
    String address
) {}
