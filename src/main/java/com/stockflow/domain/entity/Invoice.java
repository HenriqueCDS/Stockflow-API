package com.stockflow.domain.entity;

import com.stockflow.domain.enums.InvoiceStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "invoices",
    indexes = {
        @Index(name = "idx_invoices_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_invoices_key", columnList = "invoice_key"),
        @Index(name = "idx_invoices_tenant_key", columnList = "tenant_id, invoice_key")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Invoice extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "invoice_key", length = 44)
    private String invoiceKey;

    @Column(name = "supplier_name")
    private String supplierName;

    @Column(name = "supplier_cnpj", length = 14)
    private String supplierCnpj;

    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    @Column(name = "total_value", precision = 15, scale = 2)
    private BigDecimal totalValue;

    @Column(name = "qr_code_url", length = 2048)
    private String qrCodeUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private InvoiceStatus status = InvoiceStatus.PENDING;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<InvoiceItem> items = new ArrayList<>();
}
