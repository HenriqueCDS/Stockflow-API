package com.stockflow.domain.dto.user;

import java.util.UUID;

public record UserProfileResponseDTO(
    UUID id,
    String name,
    String email,
    String role
) {}
