package com.stockflow.service;

import com.stockflow.domain.dto.dashboard.DashboardDTO;
import com.stockflow.domain.dto.dashboard.RecentMovementDTO;
import com.stockflow.domain.entity.StockMovement;
import com.stockflow.repository.InvoiceRepository;
import com.stockflow.repository.ProductRepository;
import com.stockflow.repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ProductRepository productRepository;
    private final InvoiceRepository invoiceRepository;
    private final StockMovementRepository stockMovementRepository;

    @Transactional(readOnly = true)
    public DashboardDTO getDashboard(UUID tenantId) {
        long totalProducts = productRepository.countByTenantIdAndDeletedAtIsNull(tenantId);
        long activeProducts = productRepository.countActiveByTenantId(tenantId);
        long lowStockProducts = productRepository.countLowStockByTenantId(tenantId);

        long totalInvoices = invoiceRepository.countByTenantId(tenantId);
        long pendingInvoices = invoiceRepository.countAwaitingReviewByTenantId(tenantId);

        LocalDate monthStart = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth());
        LocalDate nextMonthStart = monthStart.plusMonths(1);
        BigDecimal monthlySpend = invoiceRepository.sumConfirmedTotalValueBetween(tenantId, monthStart, nextMonthStart);

        List<StockMovement> recentMovements = stockMovementRepository
            .findRecentByTenantId(tenantId, PageRequest.of(0, 10));
        List<RecentMovementDTO> recentDTOs = recentMovements.stream()
            .map(m -> new RecentMovementDTO(
                m.getId(), m.getProduct().getName(), m.getType(), m.getQuantity(), m.getCreatedAt()))
            .toList();

        return new DashboardDTO(
            totalProducts, activeProducts, lowStockProducts,
            totalInvoices, pendingInvoices,
            monthlySpend != null ? monthlySpend : BigDecimal.ZERO,
            recentDTOs
        );
    }
}
