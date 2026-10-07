package com.stockflow.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "products",
    indexes = {
        @Index(name = "idx_products_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_products_ean", columnList = "ean"),
        @Index(name = "idx_products_tenant_ean", columnList = "tenant_id, ean")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Product extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private String name;

    @Column(length = 14)
    private String ean;

    @Column(length = 100)
    private String category;

    @Column(length = 20)
    private String unit;

    @Column(name = "current_stock", nullable = false, precision = 15, scale = 4)
    @Builder.Default
    private BigDecimal currentStock = BigDecimal.ZERO;

    @Column(name = "minimum_stock", precision = 15, scale = 4)
    @Builder.Default
    private BigDecimal minimumStock = BigDecimal.ZERO;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_by")
    private UUID createdBy;

    public void addStock(BigDecimal quantity) {
        this.currentStock = this.currentStock.add(quantity);
    }
}
