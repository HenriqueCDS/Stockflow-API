package com.stockflow.fiscal.service;

import com.stockflow.exception.FiscalException;
import com.stockflow.fiscal.dto.NfceDTO;
import com.stockflow.fiscal.provider.FiscalProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class FiscalService {

    private static final Pattern URL_PATTERN = Pattern.compile("https?://[^\\s\"'<>]+");

    private final List<FiscalProvider> providers;
    private final FiscalUrlValidator urlValidator;

    public FiscalService(List<FiscalProvider> providers, FiscalUrlValidator urlValidator) {
        this.providers = providers;
        this.urlValidator = urlValidator;
    }

    public NfceDTO processQrCode(String qrCodeContent) {
        String url = extractUrl(qrCodeContent);
        log.info("Processing NFC-e URL: {}", url);
        return fetchFromProvider(url);
    }

    public NfceDTO fetchFromUrl(String url) {
        return fetchFromProvider(url);
    }

    private NfceDTO fetchFromProvider(String url) {
        urlValidator.validate(url);
        FiscalProvider provider = providers.stream()
            .filter(p -> p.supports(url))
            .findFirst()
            .orElseGet(() -> getDefaultProvider());

        log.info("Using fiscal provider: {} for URL: {}", provider.getName(), url);
        return provider.fetchInvoice(url);
    }

    private FiscalProvider getDefaultProvider() {
        // SEFAZ is the default fallback provider
        return providers.stream()
            .filter(p -> "SEFAZ".equals(p.getName()))
            .findFirst()
            .orElseThrow(() -> new FiscalException("No fiscal provider available", HttpStatus.SERVICE_UNAVAILABLE));
    }

    private String extractUrl(String content) {
        if (content == null || content.isBlank()) {
            throw new FiscalException("QR Code content is empty", HttpStatus.BAD_REQUEST);
        }

        content = content.trim();

        // Direct URL
        if (content.startsWith("http")) {
            return content;
        }

        // URL embedded in content
        Matcher m = URL_PATTERN.matcher(content);
        if (m.find()) {
            return m.group();
        }

        throw new FiscalException("Could not extract URL from QR Code: " + content, HttpStatus.BAD_REQUEST);
    }
}
