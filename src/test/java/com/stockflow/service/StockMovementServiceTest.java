package com.stockflow.service;

import com.stockflow.domain.dto.stock.StockAdjustmentRequestDTO;
import com.stockflow.domain.entity.Product;
import com.stockflow.domain.entity.StockMovement;
import com.stockflow.domain.enums.MovementType;
import com.stockflow.exception.BusinessException;
import com.stockflow.mapper.StockMovementMapper;
import com.stockflow.repository.StockMovementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockMovementServiceTest {

    @Mock StockMovementRepository stockMovementRepository;
    @Mock ProductService productService;
    @Mock StockMovementMapper stockMovementMapper;

    @InjectMocks StockMovementService stockMovementService;

    private UUID tenantId;
    private UUID userId;
    private Product product;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();
        product = Product.builder()
            .tenantId(tenantId)
            .name("Test Product")
            .currentStock(BigDecimal.TEN)
            .averageCost(BigDecimal.ONE)
            .build();
    }

    @Test
    void recordEntry_shouldStampCreatedBy() {
        when(stockMovementRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        StockMovement movement = stockMovementService.recordEntry(
            tenantId, userId, product, new BigDecimal("5"), new BigDecimal("10.00"), "ref");

        assertThat(movement.getCreatedBy()).isEqualTo(userId);
        assertThat(movement.getType()).isEqualTo(MovementType.ENTRY);
    }

    @Test
    void adjust_shouldStampCreatedBy() {
        when(productService.findByTenantAndId(tenantId, null)).thenReturn(product);
        when(stockMovementRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        StockAdjustmentRequestDTO request = new StockAdjustmentRequestDTO(
            null, MovementType.ADJUSTMENT, new BigDecimal("3"), null, "ajuste");

        stockMovementService.adjust(tenantId, userId, request);

        verify(stockMovementRepository).save(argThat(m -> userId.equals(m.getCreatedBy())));
    }

    @Test
    void quickExit_shouldSubtractStockAndStampAuthorAndType() {
        UUID productId = UUID.randomUUID();
        when(productService.findByTenantAndId(tenantId, productId)).thenReturn(product);
        when(stockMovementRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        stockMovementService.quickExit(tenantId, userId, productId, MovementType.USED, BigDecimal.ONE);

        assertThat(product.getCurrentStock()).isEqualTo(new BigDecimal("9"));
        verify(stockMovementRepository).save(argThat(m ->
            userId.equals(m.getCreatedBy()) && m.getType() == MovementType.USED));
    }

    @Test
    void quickExit_shouldThrowWhenInsufficientStock() {
        UUID productId = UUID.randomUUID();
        product.setCurrentStock(BigDecimal.ZERO);
        when(productService.findByTenantAndId(tenantId, productId)).thenReturn(product);

        assertThatThrownBy(() -> stockMovementService.quickExit(
            tenantId, userId, productId, MovementType.DISCARDED, BigDecimal.ONE))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("Insufficient stock");
    }
}
