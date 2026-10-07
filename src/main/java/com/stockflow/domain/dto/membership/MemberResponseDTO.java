package com.stockflow.domain.dto.membership;

import com.stockflow.domain.enums.UserRole;

import java.time.LocalDateTime;
import java.util.UUID;

public record MemberResponseDTO(
    UUID id,
    String name,
    String email,
    UserRole role,
    LocalDateTime createdAt
) {}
