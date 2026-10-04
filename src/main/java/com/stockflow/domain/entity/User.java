package com.stockflow.domain.entity;

import com.stockflow.domain.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Table(name = "users",
    indexes = {
        @Index(name = "idx_users_email", columnList = "email"),
        @Index(name = "idx_users_tenant_id", columnList = "tenant_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class User extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private UserRole role = UserRole.USER;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "refresh_token")
    private String refreshToken;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;
}
