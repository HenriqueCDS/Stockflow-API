package com.stockflow.repository.specification;

import com.stockflow.domain.dto.product.ProductFilterDTO;
import com.stockflow.domain.entity.Product;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProductSpecification {

    private ProductSpecification() {}

    public static Specification<Product> withFilter(UUID tenantId, ProductFilterDTO filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("tenantId"), tenantId));
            predicates.add(cb.isNull(root.get("deletedAt")));

            if (filter != null) {
                if (filter.name() != null && !filter.name().isBlank()) {
                    predicates.add(cb.like(cb.lower(root.get("name")), "%" + filter.name().toLowerCase() + "%"));
                }
                if (filter.ean() != null && !filter.ean().isBlank()) {
                    predicates.add(cb.equal(root.get("ean"), filter.ean()));
                }
                if (filter.category() != null && !filter.category().isBlank()) {
                    predicates.add(cb.equal(cb.lower(root.get("category")), filter.category().toLowerCase()));
                }
                if (filter.active() != null) {
                    predicates.add(cb.equal(root.get("active"), filter.active()));
                }
                if (filter.createdBy() != null) {
                    predicates.add(cb.equal(root.get("createdBy"), filter.createdBy()));
                }
                if (Boolean.TRUE.equals(filter.belowMinimum())) {
                    predicates.add(cb.and(
                        cb.greaterThan(root.get("minimumStock"), java.math.BigDecimal.ZERO),
                        cb.lessThanOrEqualTo(root.get("currentStock"), root.get("minimumStock"))
                    ));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
