package com.stockflow.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "shopping_list_items",
    indexes = {
        @Index(name = "idx_shopping_list_items_tenant_open", columnList = "tenant_id, checked"),
        @Index(name = "idx_shopping_list_items_product", columnList = "product_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ShoppingListItem extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    // Nulo para item manual sem produto do catalogo (ex.: "sacos de lixo").
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false)
    private String name;

    @Column(precision = 15, scale = 4)
    private BigDecimal quantity;

    // Riscado: comprado manualmente ou automaticamente ao confirmar a proxima nota com o mesmo produto.
    @Column(nullable = false)
    @Builder.Default
    private boolean checked = false;

    // Nulo para item gerado automaticamente (produto abaixo do minimo).
    @Column(name = "created_by")
    private UUID createdBy;
}
