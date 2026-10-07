package com.stockflow.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "invoice_items",
    indexes = {
        @Index(name = "idx_invoice_items_invoice_id", columnList = "invoice_id"),
        @Index(name = "idx_invoice_items_product_id", columnList = "product_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class InvoiceItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "product_ean", length = 14)
    private String productEan;

    @Column(nullable = false, precision = 15, scale = 4)
    private BigDecimal quantity;

    @Column(name = "unit_value", nullable = false, precision = 15, scale = 4)
    private BigDecimal unitValue;

    @Column(name = "total_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalValue;

    @Column(length = 20)
    private String unit;

    // Marcado na revisao (antes de confirmar): item excluido, nao gera movimento de estoque.
    @Column(nullable = false)
    @Builder.Default
    private boolean ignored = false;
}
