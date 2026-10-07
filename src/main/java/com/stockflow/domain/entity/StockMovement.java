package com.stockflow.domain.entity;

import com.stockflow.domain.enums.MovementType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "stock_movements",
    indexes = {
        @Index(name = "idx_stock_movements_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_stock_movements_product_id", columnList = "product_id"),
        @Index(name = "idx_stock_movements_created_at", columnList = "created_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class StockMovement extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MovementType type;

    @Column(nullable = false, precision = 15, scale = 4)
    private BigDecimal quantity;

    @Column(name = "stock_before", precision = 15, scale = 4)
    private BigDecimal stockBefore;

    @Column(name = "stock_after", precision = 15, scale = 4)
    private BigDecimal stockAfter;

    @Column(length = 255)
    private String reference;

    @Column(length = 500)
    private String notes;

    @Column(name = "created_by")
    private UUID createdBy;
}
