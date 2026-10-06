package com.stockflow.controller;

import com.stockflow.domain.dto.common.ApiResponseDTO;
import com.stockflow.domain.dto.invoice.InvoiceResponseDTO;
import com.stockflow.domain.dto.invoice.NfceProcessRequestDTO;
import com.stockflow.exception.FiscalException;
import com.stockflow.fiscal.service.QrCodeImageDecoder;
import com.stockflow.security.JwtTokenProvider;
import com.stockflow.service.InvoiceService;
import com.stockflow.usecase.ConfirmInvoiceUseCase;
import com.stockflow.usecase.ProcessNfceUseCase;
import com.stockflow.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/nfce")
@RequiredArgsConstructor
@Tag(name = "NFC-e", description = "NFC-e processing and invoice confirmation")
@SecurityRequirement(name = "bearerAuth")
public class NfceController {

    private final ProcessNfceUseCase processNfceUseCase;
    private final ConfirmInvoiceUseCase confirmInvoiceUseCase;
    private final InvoiceService invoiceService;
    private final QrCodeImageDecoder qrCodeImageDecoder;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/process")
    @Operation(
        summary = "Process QR Code from NFC-e",
        description = "Receives QR Code content (URL or full content), fetches invoice data from SEFAZ/provider and saves as FETCHED invoice awaiting confirmation"
    )
    public ResponseEntity<ApiResponseDTO<InvoiceResponseDTO>> process(
        @Valid @RequestBody NfceProcessRequestDTO body,
        HttpServletRequest request) {
        var tenantId = SecurityUtils.getCurrentTenantId(jwtTokenProvider, request);
        var userId = SecurityUtils.getCurrentUserId(jwtTokenProvider, request);
        InvoiceResponseDTO invoice = processNfceUseCase.execute(tenantId, userId, body.qrCode());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseDTO.ok("Invoice fetched successfully", invoice));
    }

    @PostMapping(value = "/process/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Process NFC-e from a QR Code image",
        description = "Decodes the QR Code from an uploaded image (PNG, JPEG, GIF or BMP, max 5MB) and processes it like /process"
    )
    public ResponseEntity<ApiResponseDTO<InvoiceResponseDTO>> processImage(
        @RequestPart("file") MultipartFile file,
        HttpServletRequest request) throws IOException {
        if (file.isEmpty()) {
            throw new FiscalException("Image file is empty", HttpStatus.BAD_REQUEST);
        }
        var tenantId = SecurityUtils.getCurrentTenantId(jwtTokenProvider, request);
        var userId = SecurityUtils.getCurrentUserId(jwtTokenProvider, request);
        String qrContent = qrCodeImageDecoder.decode(file.getBytes());
        InvoiceResponseDTO invoice = processNfceUseCase.execute(tenantId, userId, qrContent);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseDTO.ok("Invoice fetched successfully", invoice));
    }

    @PostMapping("/{invoiceId}/confirm")
    @Operation(
        summary = "Confirm invoice and update stock",
        description = "Confirms a FETCHED invoice. Creates stock movement entries and updates product quantities and average costs"
    )
    public ResponseEntity<ApiResponseDTO<InvoiceResponseDTO>> confirm(
        @PathVariable UUID invoiceId,
        HttpServletRequest request) {
        var tenantId = SecurityUtils.getCurrentTenantId(jwtTokenProvider, request);
        InvoiceResponseDTO invoice = confirmInvoiceUseCase.execute(tenantId, invoiceId);
        return ResponseEntity.ok(ApiResponseDTO.ok("Invoice confirmed and stock updated", invoice));
    }

    @PostMapping("/{invoiceId}/reject")
    @Operation(summary = "Reject fetched invoice")
    public ResponseEntity<ApiResponseDTO<Void>> reject(
        @PathVariable UUID invoiceId,
        HttpServletRequest request) {
        var tenantId = SecurityUtils.getCurrentTenantId(jwtTokenProvider, request);
        invoiceService.reject(tenantId, invoiceId);
        return ResponseEntity.ok(ApiResponseDTO.ok("Invoice rejected", null));
    }
}
