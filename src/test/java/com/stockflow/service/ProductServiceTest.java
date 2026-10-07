package com.stockflow.service;

import com.stockflow.domain.dto.product.ProductRequestDTO;
import com.stockflow.domain.dto.product.ProductResponseDTO;
import com.stockflow.domain.entity.Product;
import com.stockflow.exception.DuplicateResourceException;
import com.stockflow.exception.ResourceNotFoundException;
import com.stockflow.mapper.ProductMapper;
import com.stockflow.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock ProductRepository productRepository;
    @Mock ProductMapper productMapper;

    @InjectMocks ProductService productService;

    private UUID tenantId;
    private UUID userId;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();
        testProduct = Product.builder()
            .tenantId(tenantId)
            .name("Test Product")
            .ean("7891234567890")
            .currentStock(BigDecimal.TEN)
            .build();
        try {
            var idField = testProduct.getClass().getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(testProduct, UUID.randomUUID());
        } catch (Exception ignored) {}
    }

    @Test
    void create_shouldThrowWhenEanAlreadyExists() {
        when(productRepository.findByTenantIdAndEanAndDeletedAtIsNull(any(), anyString()))
            .thenReturn(Optional.of(testProduct));

        ProductRequestDTO request = new ProductRequestDTO("Product", "7891234567890", null, "UN", BigDecimal.ZERO);

        assertThatThrownBy(() -> productService.create(tenantId, userId, request))
            .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void create_shouldCreateProductSuccessfully() {
        ProductRequestDTO request = new ProductRequestDTO("New Product", "1234567890123", null, "UN", BigDecimal.ZERO);
        ProductResponseDTO expectedResponse = new ProductResponseDTO(
            UUID.randomUUID(), "New Product", "1234567890123", null, "UN",
            BigDecimal.ZERO, BigDecimal.ZERO,
            true, false, null, userId);

        when(productRepository.findByTenantIdAndEanAndDeletedAtIsNull(any(), anyString()))
            .thenReturn(Optional.empty());
        when(productMapper.toEntity(any())).thenReturn(testProduct);
        when(productRepository.save(any())).thenReturn(testProduct);
        when(productMapper.toResponse(any())).thenReturn(expectedResponse);

        ProductResponseDTO result = productService.create(tenantId, userId, request);
        assertThat(result.name()).isEqualTo("New Product");
    }

    @Test
    void create_shouldLinkProductToCreatingUser() {
        ProductRequestDTO request = new ProductRequestDTO("New Product", null, null, "UN", BigDecimal.ZERO);
        when(productMapper.toEntity(any())).thenReturn(testProduct);
        when(productRepository.save(any())).thenReturn(testProduct);

        productService.create(tenantId, userId, request);

        verify(productRepository).save(argThat(p -> userId.equals(p.getCreatedBy()) && tenantId.equals(p.getTenantId())));
    }

    @Test
    void findOrCreateByEan_shouldLinkAutoCreatedProductToUser() {
        when(productRepository.findByTenantIdAndEanAndDeletedAtIsNull(any(), anyString()))
            .thenReturn(Optional.empty());
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Product result = productService.findOrCreateByEan(tenantId, userId, "7891234567890", "Arroz", "KG");

        assertThat(result.getCreatedBy()).isEqualTo(userId);
        assertThat(result.getTenantId()).isEqualTo(tenantId);
    }

    @Test
    void getById_shouldThrowWhenNotFound() {
        when(productRepository.findByIdAndTenantIdAndDeletedAtIsNull(any(), any()))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getById(tenantId, UUID.randomUUID()))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_shouldSoftDeleteProduct() {
        when(productRepository.findByIdAndTenantIdAndDeletedAtIsNull(any(), any()))
            .thenReturn(Optional.of(testProduct));
        when(productRepository.save(any())).thenReturn(testProduct);

        productService.delete(tenantId, UUID.randomUUID());

        verify(productRepository).save(argThat(p -> p.isDeleted() && !p.isActive()));
    }

    @Test
    void addStock_shouldIncreaseCurrentStock() {
        Product product = Product.builder()
            .currentStock(new BigDecimal("10"))
            .build();

        product.addStock(new BigDecimal("5"));

        assertThat(product.getCurrentStock()).isEqualByComparingTo("15");
    }
}
