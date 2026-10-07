package com.stockflow.service;

import com.stockflow.domain.dto.shoppinglist.ShoppingListItemRequestDTO;
import com.stockflow.domain.dto.shoppinglist.ShoppingListItemResponseDTO;
import com.stockflow.domain.entity.Product;
import com.stockflow.domain.entity.ShoppingListItem;
import com.stockflow.exception.ResourceNotFoundException;
import com.stockflow.mapper.ShoppingListItemMapper;
import com.stockflow.repository.ProductRepository;
import com.stockflow.repository.ShoppingListItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShoppingListServiceTest {

    @Mock ShoppingListItemRepository shoppingListItemRepository;
    @Mock ProductRepository productRepository;
    @Mock ShoppingListItemMapper shoppingListItemMapper;

    @InjectMocks ShoppingListService shoppingListService;

    private UUID tenantId;
    private UUID userId;
    private UUID itemId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();
        itemId = UUID.randomUUID();
    }

    @Test
    void list_shouldCreateItemForBelowMinimumProductNotAlreadyOnList() {
        Product product = Product.builder().tenantId(tenantId).name("Arroz").build();
        when(productRepository.findBelowMinimumByTenantId(tenantId)).thenReturn(List.of(product));
        when(shoppingListItemRepository.findOpenByTenantAndProduct(tenantId, product.getId()))
            .thenReturn(List.of());
        when(shoppingListItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(shoppingListItemRepository.findByTenantIdAndCheckedFalseAndDeletedAtIsNullOrderByCreatedAtAsc(tenantId))
            .thenReturn(List.of());

        shoppingListService.list(tenantId);

        verify(shoppingListItemRepository).save(argThat(item ->
            item.getProduct() == product && item.getName().equals("Arroz") && !item.isChecked()));
    }

    @Test
    void list_shouldNotDuplicateWhenProductAlreadyOnOpenList() {
        Product product = Product.builder().tenantId(tenantId).name("Arroz").build();
        when(productRepository.findBelowMinimumByTenantId(tenantId)).thenReturn(List.of(product));
        when(shoppingListItemRepository.findOpenByTenantAndProduct(tenantId, product.getId()))
            .thenReturn(List.of(mock(ShoppingListItem.class)));
        when(shoppingListItemRepository.findByTenantIdAndCheckedFalseAndDeletedAtIsNullOrderByCreatedAtAsc(tenantId))
            .thenReturn(List.of());

        shoppingListService.list(tenantId);

        verify(shoppingListItemRepository, never()).save(any());
    }

    @Test
    void addManualItem_shouldPersistWithCreatedBy() {
        when(shoppingListItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(shoppingListItemMapper.toResponse(any())).thenReturn(
            new ShoppingListItemResponseDTO(null, null, "Sacos de lixo", null, false, null));

        ShoppingListItemResponseDTO response = shoppingListService.addManualItem(
            tenantId, userId, new ShoppingListItemRequestDTO("Sacos de lixo", null));

        assertThat(response.name()).isEqualTo("Sacos de lixo");
        verify(shoppingListItemRepository).save(argThat(item -> userId.equals(item.getCreatedBy())));
    }

    @Test
    void checkItem_shouldThrowWhenNotFound() {
        when(shoppingListItemRepository.findByIdAndTenantIdAndDeletedAtIsNull(itemId, tenantId))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> shoppingListService.checkItem(tenantId, itemId))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void checkItem_shouldMarkAsChecked() {
        ShoppingListItem item = ShoppingListItem.builder().tenantId(tenantId).name("Arroz").build();
        when(shoppingListItemRepository.findByIdAndTenantIdAndDeletedAtIsNull(itemId, tenantId))
            .thenReturn(Optional.of(item));
        when(shoppingListItemRepository.save(any())).thenReturn(item);

        shoppingListService.checkItem(tenantId, itemId);

        assertThat(item.isChecked()).isTrue();
    }

    @Test
    void checkByProduct_shouldCheckAllOpenItemsForThatProduct() {
        UUID productId = UUID.randomUUID();
        ShoppingListItem item1 = ShoppingListItem.builder().tenantId(tenantId).name("Arroz").build();
        when(shoppingListItemRepository.findOpenByTenantAndProduct(tenantId, productId))
            .thenReturn(List.of(item1));
        when(shoppingListItemRepository.save(any())).thenReturn(item1);

        shoppingListService.checkByProduct(tenantId, productId);

        assertThat(item1.isChecked()).isTrue();
    }
}
