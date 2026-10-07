package com.stockflow.service;

import com.stockflow.domain.dto.invoice.InvoiceItemReviewRequestDTO;
import com.stockflow.domain.entity.Invoice;
import com.stockflow.domain.entity.InvoiceItem;
import com.stockflow.domain.entity.Product;
import com.stockflow.domain.enums.InvoiceStatus;
import com.stockflow.exception.BusinessException;
import com.stockflow.exception.ResourceNotFoundException;
import com.stockflow.mapper.InvoiceMapper;
import com.stockflow.repository.InvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock InvoiceRepository invoiceRepository;
    @Mock InvoiceMapper invoiceMapper;
    @Mock ProductService productService;

    @InjectMocks InvoiceService invoiceService;

    private UUID tenantId;
    private UUID invoiceId;
    private UUID itemId;
    private Invoice invoice;
    private InvoiceItem item;

    private static void setId(Object entity, UUID id) throws Exception {
        Field idField = entity.getClass().getSuperclass().getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(entity, id);
    }

    @BeforeEach
    void setUp() throws Exception {
        tenantId = UUID.randomUUID();
        invoiceId = UUID.randomUUID();
        itemId = UUID.randomUUID();

        item = InvoiceItem.builder()
            .productName("Arroz")
            .quantity(new BigDecimal("2"))
            .unitValue(new BigDecimal("10.00"))
            .totalValue(new BigDecimal("20.00"))
            .build();
        setId(item, itemId);

        invoice = Invoice.builder()
            .tenantId(tenantId)
            .status(InvoiceStatus.FETCHED)
            .items(List.of(item))
            .build();
        setId(invoice, invoiceId);

        when(invoiceRepository.findByIdAndTenantIdAndDeletedAtIsNull(invoiceId, tenantId))
            .thenReturn(java.util.Optional.of(invoice));
    }

    private void stubSave() {
        when(invoiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void reviewItem_shouldThrowWhenInvoiceNotFetched() {
        invoice.setStatus(InvoiceStatus.CONFIRMED);

        assertThatThrownBy(() -> invoiceService.reviewItem(
            tenantId, invoiceId, itemId, new InvoiceItemReviewRequestDTO(null, null, null, null)))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("FETCHED");
    }

    @Test
    void reviewItem_shouldThrowWhenItemNotFound() {
        UUID otherItemId = UUID.randomUUID();

        assertThatThrownBy(() -> invoiceService.reviewItem(
            tenantId, invoiceId, otherItemId, new InvoiceItemReviewRequestDTO(null, null, null, null)))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void reviewItem_shouldRenameItem() {
        stubSave();
        invoiceService.reviewItem(tenantId, invoiceId, itemId,
            new InvoiceItemReviewRequestDTO("Arroz Tipo 1", null, null, null));

        assertThat(item.getProductName()).isEqualTo("Arroz Tipo 1");
    }

    @Test
    void reviewItem_shouldAdjustQuantityAndRecalculateTotal() {
        stubSave();
        invoiceService.reviewItem(tenantId, invoiceId, itemId,
            new InvoiceItemReviewRequestDTO(null, null, new BigDecimal("5"), null));

        assertThat(item.getQuantity()).isEqualTo(new BigDecimal("5"));
        assertThat(item.getTotalValue()).isEqualTo(new BigDecimal("50.00"));
    }

    @Test
    void reviewItem_shouldMergeIntoExistingProduct() {
        UUID targetProductId = UUID.randomUUID();
        Product target = Product.builder().tenantId(tenantId).name("Arroz 5kg").build();
        when(productService.findByTenantAndId(tenantId, targetProductId)).thenReturn(target);
        stubSave();

        invoiceService.reviewItem(tenantId, invoiceId, itemId,
            new InvoiceItemReviewRequestDTO(null, targetProductId, null, null));

        assertThat(item.getProduct()).isSameAs(target);
    }

    @Test
    void reviewItem_shouldMarkItemAsIgnored() {
        stubSave();
        invoiceService.reviewItem(tenantId, invoiceId, itemId,
            new InvoiceItemReviewRequestDTO(null, null, null, true));

        assertThat(item.isIgnored()).isTrue();
    }
}
