package com.stockflow.usecase;

import com.stockflow.domain.entity.Invoice;
import com.stockflow.domain.entity.InvoiceItem;
import com.stockflow.domain.entity.Product;
import com.stockflow.domain.enums.InvoiceStatus;
import com.stockflow.exception.BusinessException;
import com.stockflow.mapper.InvoiceMapper;
import com.stockflow.repository.InvoiceRepository;
import com.stockflow.service.InvoiceService;
import com.stockflow.service.ShoppingListService;
import com.stockflow.service.StockMovementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfirmInvoiceUseCaseTest {

    @Mock InvoiceService invoiceService;
    @Mock InvoiceRepository invoiceRepository;
    @Mock StockMovementService stockMovementService;
    @Mock ShoppingListService shoppingListService;
    @Mock InvoiceMapper invoiceMapper;

    @InjectMocks ConfirmInvoiceUseCase confirmInvoiceUseCase;

    private UUID tenantId;
    private UUID userId;
    private UUID invoiceId;
    private Invoice invoice;
    private Product product;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();
        invoiceId = UUID.randomUUID();

        product = Product.builder()
            .tenantId(tenantId)
            .name("Test Product")
            .currentStock(BigDecimal.ZERO)
            .averageCost(BigDecimal.ZERO)
            .build();

        InvoiceItem item = InvoiceItem.builder()
            .product(product)
            .productName("Test Product")
            .quantity(new BigDecimal("5"))
            .unitValue(new BigDecimal("10.00"))
            .totalValue(new BigDecimal("50.00"))
            .build();

        invoice = Invoice.builder()
            .tenantId(tenantId)
            .status(InvoiceStatus.FETCHED)
            .items(List.of(item))
            .build();
    }

    @Test
    void execute_shouldThrowWhenInvoiceNotFetched() {
        invoice.setStatus(InvoiceStatus.CONFIRMED);
        when(invoiceService.findByTenantAndId(any(), any())).thenReturn(invoice);

        assertThatThrownBy(() -> confirmInvoiceUseCase.execute(tenantId, userId, invoiceId))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("FETCHED");
    }

    @Test
    void execute_shouldCreateStockMovementsForEachItem() {
        when(invoiceService.findByTenantAndId(any(), any())).thenReturn(invoice);
        when(invoiceRepository.save(any())).thenReturn(invoice);
        when(invoiceMapper.toResponse(any())).thenReturn(null);

        confirmInvoiceUseCase.execute(tenantId, userId, invoiceId);

        verify(stockMovementService, times(1))
            .recordEntry(eq(tenantId), eq(userId), eq(product), eq(new BigDecimal("5")),
                eq(new BigDecimal("10.00")), anyString());
        verify(shoppingListService, times(1)).checkByProduct(eq(tenantId), eq(product.getId()));
    }

    @Test
    void execute_shouldSetInvoiceStatusToConfirmed() {
        when(invoiceService.findByTenantAndId(any(), any())).thenReturn(invoice);
        when(invoiceRepository.save(any())).thenReturn(invoice);

        confirmInvoiceUseCase.execute(tenantId, userId, invoiceId);

        verify(invoiceRepository).save(argThat(inv -> inv.getStatus() == InvoiceStatus.CONFIRMED));
    }
}
