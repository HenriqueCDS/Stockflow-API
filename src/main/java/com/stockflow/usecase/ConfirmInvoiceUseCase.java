package com.stockflow.usecase;

import com.stockflow.domain.dto.invoice.InvoiceResponseDTO;
import com.stockflow.domain.entity.Invoice;
import com.stockflow.domain.entity.InvoiceItem;
import com.stockflow.domain.enums.InvoiceStatus;
import com.stockflow.exception.BusinessException;
import com.stockflow.mapper.InvoiceMapper;
import com.stockflow.repository.InvoiceRepository;
import com.stockflow.service.InvoiceService;
import com.stockflow.service.ShoppingListService;
import com.stockflow.service.StockMovementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConfirmInvoiceUseCase {

    private final InvoiceService invoiceService;
    private final InvoiceRepository invoiceRepository;
    private final StockMovementService stockMovementService;
    private final ShoppingListService shoppingListService;
    private final InvoiceMapper invoiceMapper;

    @Transactional
    public InvoiceResponseDTO execute(UUID tenantId, UUID userId, UUID invoiceId) {
        Invoice invoice = invoiceService.findByTenantAndId(tenantId, invoiceId);

        if (invoice.getStatus() != InvoiceStatus.FETCHED) {
            throw new BusinessException(
                "Invoice must be in FETCHED status to be confirmed. Current: " + invoice.getStatus(),
                HttpStatus.UNPROCESSABLE_ENTITY);
        }

        String reference = "INVOICE#" + invoiceId;

        for (InvoiceItem item : invoice.getItems()) {
            if (item.getProduct() == null || item.isIgnored()) continue;
            stockMovementService.recordEntry(
                tenantId,
                userId,
                item.getProduct(),
                item.getQuantity(),
                item.getUnitValue(),
                reference
            );
            shoppingListService.checkByProduct(tenantId, item.getProduct().getId());
        }

        invoice.setStatus(InvoiceStatus.CONFIRMED);
        invoice = invoiceRepository.save(invoice);

        log.info("Invoice confirmed: invoiceId={}, tenantId={}, items={}",
            invoiceId, tenantId, invoice.getItems().size());

        return invoiceMapper.toResponse(invoice);
    }
}
