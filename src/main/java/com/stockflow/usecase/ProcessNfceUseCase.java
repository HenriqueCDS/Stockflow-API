package com.stockflow.usecase;

import com.stockflow.domain.dto.invoice.InvoiceResponseDTO;
import com.stockflow.domain.entity.Invoice;
import com.stockflow.domain.entity.InvoiceItem;
import com.stockflow.domain.entity.Product;
import com.stockflow.domain.enums.InvoiceStatus;
import com.stockflow.exception.DuplicateResourceException;
import com.stockflow.exception.FiscalException;
import com.stockflow.fiscal.dto.NfceDTO;
import com.stockflow.fiscal.dto.NfceItemDTO;
import com.stockflow.fiscal.dto.NfceKey;
import com.stockflow.fiscal.service.FiscalService;
import com.stockflow.mapper.InvoiceMapper;
import com.stockflow.repository.InvoiceRepository;
import com.stockflow.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProcessNfceUseCase {

    private final FiscalService fiscalService;
    private final InvoiceRepository invoiceRepository;
    private final ProductService productService;
    private final InvoiceMapper invoiceMapper;

    @Transactional
    public InvoiceResponseDTO execute(UUID tenantId, String qrCodeContent) {
        // The key embedded in the QR is authoritative: validate it and dedupe before any external call.
        NfceKey qrKey = NfceKey.extractRaw(qrCodeContent)
            .map(raw -> NfceKey.parse(raw).orElseThrow(() ->
                new FiscalException("Invalid NFC-e access key in QR Code", HttpStatus.BAD_REQUEST)))
            .orElse(null);

        if (qrKey != null && invoiceRepository.existsByInvoiceKeyAndTenantId(qrKey.value(), tenantId)) {
            throw new DuplicateResourceException("Invoice", "key", qrKey.value());
        }

        NfceDTO nfceData = fiscalService.processQrCode(qrCodeContent);

        String invoiceKey = qrKey != null ? qrKey.value() : nfceData.getInvoiceKey();
        if (qrKey == null && invoiceKey != null &&
            invoiceRepository.existsByInvoiceKeyAndTenantId(invoiceKey, tenantId)) {
            throw new DuplicateResourceException("Invoice", "key", invoiceKey);
        }

        String supplierCnpj = nfceData.getSupplierCnpj() != null
            ? nfceData.getSupplierCnpj()
            : (qrKey != null ? qrKey.cnpj() : null);

        Invoice invoice = Invoice.builder()
            .tenantId(tenantId)
            .invoiceKey(invoiceKey)
            .supplierName(nfceData.getSupplierName())
            .supplierCnpj(supplierCnpj)
            .purchaseDate(nfceData.getPurchaseDate())
            .totalValue(nfceData.getTotalValue())
            .qrCodeUrl(qrCodeContent.startsWith("http") ? qrCodeContent : null)
            .status(InvoiceStatus.FETCHED)
            .build();

        List<InvoiceItem> items = buildItems(tenantId, invoice, nfceData.getItems());
        invoice.setItems(items);

        invoice = invoiceRepository.save(invoice);
        log.info("NFC-e processed: tenantId={}, invoiceKey={}, items={}", tenantId, invoiceKey, items.size());

        return invoiceMapper.toResponse(invoice);
    }

    private List<InvoiceItem> buildItems(UUID tenantId, Invoice invoice, List<NfceItemDTO> nfceItems) {
        List<InvoiceItem> items = new ArrayList<>();
        if (nfceItems == null) return items;

        for (NfceItemDTO nfceItem : nfceItems) {
            Product product = productService.findOrCreateByEan(
                tenantId, nfceItem.getEan(), nfceItem.getName(), nfceItem.getUnit());

            InvoiceItem item = InvoiceItem.builder()
                .invoice(invoice)
                .product(product)
                .productName(nfceItem.getName())
                .productEan(nfceItem.getEan())
                .quantity(nfceItem.getQuantity())
                .unitValue(nfceItem.getUnitValue())
                .totalValue(nfceItem.getTotalValue())
                .unit(nfceItem.getUnit())
                .build();
            items.add(item);
        }
        return items;
    }
}
