package com.stockflow.service;

import com.stockflow.domain.dto.common.PageResponseDTO;
import com.stockflow.domain.dto.product.ProductFilterDTO;
import com.stockflow.domain.dto.product.ProductRequestDTO;
import com.stockflow.domain.dto.product.ProductResponseDTO;
import com.stockflow.domain.entity.Product;
import com.stockflow.exception.DuplicateResourceException;
import com.stockflow.exception.ResourceNotFoundException;
import com.stockflow.mapper.ProductMapper;
import com.stockflow.repository.ProductRepository;
import com.stockflow.repository.specification.ProductSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Transactional
    public ProductResponseDTO create(UUID tenantId, UUID userId, ProductRequestDTO request) {
        if (request.ean() != null && !request.ean().isBlank()) {
            productRepository.findByTenantIdAndEanAndDeletedAtIsNull(tenantId, request.ean())
                .ifPresent(p -> { throw new DuplicateResourceException("Product", "ean", request.ean()); });
        }

        Product product = productMapper.toEntity(request);
        product.setTenantId(tenantId);
        product.setCreatedBy(userId);

        return productMapper.toResponse(productRepository.save(product));
    }

    @Transactional(readOnly = true)
    public ProductResponseDTO getById(UUID tenantId, UUID productId) {
        Product product = findByTenantAndId(tenantId, productId);
        return productMapper.toResponse(product);
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<ProductResponseDTO> list(UUID tenantId, ProductFilterDTO filter, Pageable pageable) {
        Page<Product> page = productRepository.findAll(
            ProductSpecification.withFilter(tenantId, filter), pageable);
        return PageResponseDTO.from(page.map(productMapper::toResponse));
    }

    @Transactional
    public ProductResponseDTO update(UUID tenantId, UUID productId, ProductRequestDTO request) {
        Product product = findByTenantAndId(tenantId, productId);

        if (request.ean() != null && !request.ean().isBlank()
            && !request.ean().equals(product.getEan())) {
            productRepository.findByTenantIdAndEanAndDeletedAtIsNull(tenantId, request.ean())
                .ifPresent(p -> { throw new DuplicateResourceException("Product", "ean", request.ean()); });
        }

        productMapper.updateEntity(request, product);
        return productMapper.toResponse(productRepository.save(product));
    }

    @Transactional
    public void delete(UUID tenantId, UUID productId) {
        Product product = findByTenantAndId(tenantId, productId);
        product.softDelete();
        product.setActive(false);
        productRepository.save(product);
    }

    public Product findByTenantAndId(UUID tenantId, UUID productId) {
        return productRepository.findByIdAndTenantIdAndDeletedAtIsNull(productId, tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
    }

    public Product findOrCreateByEan(UUID tenantId, UUID userId, String ean, String name, String unit) {
        if (ean != null && !ean.isBlank()) {
            return productRepository.findByTenantIdAndEanAndDeletedAtIsNull(tenantId, ean)
                .orElseGet(() -> createAutoProduct(tenantId, userId, name, ean, unit));
        }
        return createAutoProduct(tenantId, userId, name, ean, unit);
    }

    private Product createAutoProduct(UUID tenantId, UUID userId, String name, String ean, String unit) {
        Product product = Product.builder()
            .tenantId(tenantId)
            .createdBy(userId)
            .name(name)
            .ean(ean)
            .unit(unit)
            .build();
        return productRepository.save(product);
    }
}
