package com.stockflow.service;

import com.stockflow.domain.dto.shoppinglist.ShoppingListItemRequestDTO;
import com.stockflow.domain.dto.shoppinglist.ShoppingListItemResponseDTO;
import com.stockflow.domain.entity.Product;
import com.stockflow.domain.entity.ShoppingListItem;
import com.stockflow.exception.ResourceNotFoundException;
import com.stockflow.mapper.ShoppingListItemMapper;
import com.stockflow.repository.ProductRepository;
import com.stockflow.repository.ShoppingListItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShoppingListService {

    private final ShoppingListItemRepository shoppingListItemRepository;
    private final ProductRepository productRepository;
    private final ShoppingListItemMapper shoppingListItemMapper;

    @Transactional
    public List<ShoppingListItemResponseDTO> list(UUID tenantId) {
        syncFromLowStock(tenantId);
        return shoppingListItemRepository.findByTenantIdAndCheckedFalseAndDeletedAtIsNullOrderByCreatedAtAsc(tenantId)
            .stream()
            .map(shoppingListItemMapper::toResponse)
            .toList();
    }

    // Garante um item aberto para cada produto abaixo do minimo, sem duplicar
    // se ja existe um item nao riscado apontando para o mesmo produto.
    private void syncFromLowStock(UUID tenantId) {
        for (Product product : productRepository.findBelowMinimumByTenantId(tenantId)) {
            boolean alreadyOnList = !shoppingListItemRepository
                .findOpenByTenantAndProduct(tenantId, product.getId()).isEmpty();
            if (!alreadyOnList) {
                shoppingListItemRepository.save(ShoppingListItem.builder()
                    .tenantId(tenantId)
                    .product(product)
                    .name(product.getName())
                    .build());
            }
        }
    }

    @Transactional
    public ShoppingListItemResponseDTO addManualItem(UUID tenantId, UUID userId, ShoppingListItemRequestDTO request) {
        ShoppingListItem item = ShoppingListItem.builder()
            .tenantId(tenantId)
            .name(request.name())
            .quantity(request.quantity())
            .createdBy(userId)
            .build();
        return shoppingListItemMapper.toResponse(shoppingListItemRepository.save(item));
    }

    @Transactional
    public void checkItem(UUID tenantId, UUID itemId) {
        ShoppingListItem item = findByTenantAndId(tenantId, itemId);
        item.setChecked(true);
        shoppingListItemRepository.save(item);
    }

    @Transactional
    public void deleteItem(UUID tenantId, UUID itemId) {
        ShoppingListItem item = findByTenantAndId(tenantId, itemId);
        item.softDelete();
        shoppingListItemRepository.save(item);
    }

    // Chamado ao confirmar uma nota: risca qualquer item aberto que apontava para o produto comprado.
    @Transactional
    public void checkByProduct(UUID tenantId, UUID productId) {
        shoppingListItemRepository.findOpenByTenantAndProduct(tenantId, productId)
            .forEach(item -> {
                item.setChecked(true);
                shoppingListItemRepository.save(item);
            });
    }

    private ShoppingListItem findByTenantAndId(UUID tenantId, UUID itemId) {
        return shoppingListItemRepository.findByIdAndTenantIdAndDeletedAtIsNull(itemId, tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("ShoppingListItem", itemId));
    }
}
