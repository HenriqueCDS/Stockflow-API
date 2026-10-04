package com.stockflow.fiscal.service;

import com.stockflow.exception.FiscalException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Guards against SSRF: only HTTPS URLs on allowlisted fiscal domains that resolve to public IPs.
 * Redirects are not followed by the WebClient, so the validated host is the one contacted.
 */
@Component
public class FiscalUrlValidator {

    private final List<String> allowedSuffixes;

    public FiscalUrlValidator(
        @Value("${fiscal.allowed-host-suffixes:.gov.br,.focusnfe.com.br}") String[] allowedSuffixes) {
        this.allowedSuffixes = Arrays.stream(allowedSuffixes)
            .map(s -> s.trim().toLowerCase(Locale.ROOT))
            .filter(s -> !s.isEmpty())
            .map(s -> s.startsWith(".") ? s : "." + s)
            .toList();
    }

    public void validate(String url) {
        URI uri;
        try {
            uri = URI.create(url.trim());
        } catch (IllegalArgumentException e) {
            throw reject("Malformed URL in QR Code");
        }

        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw reject("Only HTTPS URLs are accepted");
        }
        if (uri.getUserInfo() != null) {
            throw reject("URL must not contain credentials");
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw reject("URL has no host");
        }
        String normalized = host.toLowerCase(Locale.ROOT);
        if (allowedSuffixes.stream().noneMatch(normalized::endsWith)) {
            throw reject("Host not allowed: " + host);
        }

        try {
            for (InetAddress address : InetAddress.getAllByName(host)) {
                if (isNonPublic(address)) {
                    throw reject("Host resolves to a non-public address");
                }
            }
        } catch (UnknownHostException e) {
            throw new FiscalException("Could not resolve host: " + host, HttpStatus.BAD_GATEWAY);
        }
    }

    private boolean isNonPublic(InetAddress a) {
        return a.isAnyLocalAddress() || a.isLoopbackAddress() || a.isLinkLocalAddress()
            || a.isSiteLocalAddress() || a.isMulticastAddress();
    }

    private FiscalException reject(String message) {
        return new FiscalException(message, HttpStatus.BAD_REQUEST);
    }
}
