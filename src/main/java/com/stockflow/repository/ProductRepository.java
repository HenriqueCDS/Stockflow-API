package com.stockflow.repository;

import com.stockflow.domain.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

    Optional<Product> findByTenantIdAndEanAndDeletedAtIsNull(UUID tenantId, String ean);

    Optional<Product> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    List<Product> findByTenantIdAndDeletedAtIsNull(UUID tenantId);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.tenantId = :tenantId AND p.deletedAt IS NULL AND p.active = true")
    long countActiveByTenantId(UUID tenantId);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.tenantId = :tenantId AND p.deletedAt IS NULL AND p.currentStock <= p.minimumStock AND p.minimumStock > 0")
    long countLowStockByTenantId(UUID tenantId);

    long countByTenantIdAndDeletedAtIsNull(UUID tenantId);

    @Query("SELECT p FROM Product p WHERE p.tenantId = :tenantId AND p.deletedAt IS NULL AND p.active = true " +
        "AND p.minimumStock > 0 AND p.currentStock <= p.minimumStock")
    List<Product> findBelowMinimumByTenantId(UUID tenantId);
}
