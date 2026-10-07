package com.stockflow.mapper;

import com.stockflow.domain.dto.product.ProductRequestDTO;
import com.stockflow.domain.dto.product.ProductResponseDTO;
import com.stockflow.domain.entity.Product;
import org.mapstruct.*;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ProductMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "currentStock", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    Product toEntity(ProductRequestDTO dto);

    @Mapping(target = "belowMinimum", expression = "java(product.getMinimumStock() != null && product.getMinimumStock().compareTo(java.math.BigDecimal.ZERO) > 0 && product.getCurrentStock().compareTo(product.getMinimumStock()) <= 0)")
    ProductResponseDTO toResponse(Product product);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "currentStock", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void updateEntity(ProductRequestDTO dto, @MappingTarget Product product);
}
