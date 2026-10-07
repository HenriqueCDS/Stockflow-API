package com.stockflow.domain.dto.dashboard;

import java.math.BigDecimal;
import java.util.List;

public record DashboardDTO(
    long totalProducts,
    long activeProducts,
    long lowStockProducts,
    BigDecimal totalStockValue,
    long totalInvoices,
    long pendingInvoices,
    BigDecimal monthlySpend,
    List<RecentMovementDTO> recentMovements,
    List<TopProductDTO> topProducts
) {}
