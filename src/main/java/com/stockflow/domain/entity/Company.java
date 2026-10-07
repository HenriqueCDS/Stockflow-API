package com.stockflow.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Table(name = "companies",
    indexes = {
        @Index(name = "idx_companies_tenant_id", columnList = "tenant_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Company extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(name = "tenant_id", nullable = false, unique = true)
    private UUID tenantId;

    @Column(length = 100)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(length = 255)
    private String address;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;
}
