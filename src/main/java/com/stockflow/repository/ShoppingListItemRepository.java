package com.stockflow.repository;

import com.stockflow.domain.entity.ShoppingListItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShoppingListItemRepository extends JpaRepository<ShoppingListItem, UUID> {

    List<ShoppingListItem> findByTenantIdAndCheckedFalseAndDeletedAtIsNullOrderByCreatedAtAsc(UUID tenantId);

    Optional<ShoppingListItem> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    @Query("SELECT s FROM ShoppingListItem s WHERE s.tenantId = :tenantId AND s.product.id = :productId " +
        "AND s.checked = false AND s.deletedAt IS NULL")
    List<ShoppingListItem> findOpenByTenantAndProduct(UUID tenantId, UUID productId);
}
