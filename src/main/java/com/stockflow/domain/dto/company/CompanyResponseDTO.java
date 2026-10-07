package com.stockflow.domain.dto.company;

import java.time.LocalDateTime;
import java.util.UUID;

public record CompanyResponseDTO(
    UUID id,
    String name,
    UUID tenantId,
    String inviteCode,
    String email,
    String phone,
    String address,
    boolean active,
    LocalDateTime createdAt
) {}
