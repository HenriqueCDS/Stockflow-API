package com.stockflow.service;

import com.stockflow.domain.dto.common.PageResponseDTO;
import com.stockflow.domain.dto.stock.StockAdjustmentRequestDTO;
import com.stockflow.domain.dto.stock.StockMovementResponseDTO;
import com.stockflow.domain.entity.Product;
import com.stockflow.domain.entity.StockMovement;
import com.stockflow.domain.enums.MovementType;
import com.stockflow.exception.BusinessException;
import com.stockflow.mapper.StockMovementMapper;
import com.stockflow.repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockMovementService {

    private final StockMovementRepository stockMovementRepository;
    private final ProductService productService;
    private final StockMovementMapper stockMovementMapper;

    @Transactional
    public StockMovement recordEntry(UUID tenantId, UUID userId, Product product, BigDecimal quantity,
                                     BigDecimal unitCost, String reference) {
        BigDecimal stockBefore = product.getCurrentStock();
        product.updateAverageCost(quantity, unitCost);

        StockMovement movement = StockMovement.builder()
            .tenantId(tenantId)
            .product(product)
            .type(MovementType.ENTRY)
            .quantity(quantity)
            .unitCost(unitCost)
            .stockBefore(stockBefore)
            .stockAfter(product.getCurrentStock())
            .reference(reference)
            .createdBy(userId)
            .build();

        log.info("Stock entry: product={}, qty={}, ref={}, createdBy={}", product.getId(), quantity, reference, userId);
        return stockMovementRepository.save(movement);
    }

    @Transactional
    public StockMovementResponseDTO adjust(UUID tenantId, UUID userId, StockAdjustmentRequestDTO request) {
        Product product = productService.findByTenantAndId(tenantId, request.productId());
        BigDecimal stockBefore = product.getCurrentStock();

        if (request.type() == MovementType.EXIT || request.type() == MovementType.RETURN) {
            if (product.getCurrentStock().compareTo(request.quantity()) < 0) {
                throw new BusinessException("Insufficient stock for this movement");
            }
            product.setCurrentStock(product.getCurrentStock().subtract(request.quantity()));
        } else {
            BigDecimal cost = request.unitCost() != null ? request.unitCost() : BigDecimal.ZERO;
            product.updateAverageCost(request.quantity(), cost);
        }

        StockMovement movement = StockMovement.builder()
            .tenantId(tenantId)
            .product(product)
            .type(request.type())
            .quantity(request.quantity())
            .unitCost(request.unitCost())
            .stockBefore(stockBefore)
            .stockAfter(product.getCurrentStock())
            .notes(request.notes())
            .createdBy(userId)
            .build();

        return stockMovementMapper.toResponse(stockMovementRepository.save(movement));
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<StockMovementResponseDTO> listByTenant(UUID tenantId, Pageable pageable) {
        return PageResponseDTO.from(
            stockMovementRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable)
                .map(stockMovementMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<StockMovementResponseDTO> listByProduct(UUID tenantId, UUID productId, Pageable pageable) {
        productService.findByTenantAndId(tenantId, productId);
        return PageResponseDTO.from(
            stockMovementRepository.findByTenantIdAndProductIdOrderByCreatedAtDesc(tenantId, productId, pageable)
                .map(stockMovementMapper::toResponse));
    }
}
