package com.stockflow.fiscal;

import com.stockflow.exception.FiscalException;
import com.stockflow.fiscal.service.FiscalUrlValidator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FiscalUrlValidatorTest {

    private final FiscalUrlValidator validator =
        new FiscalUrlValidator(new String[]{".gov.br", ".focusnfe.com.br"});

    @Test
    void shouldRejectNonHttps() {
        assertThatThrownBy(() -> validator.validate("http://www.nfce.fazenda.sp.gov.br/qrcode?p=1"))
            .isInstanceOf(FiscalException.class).hasMessageContaining("HTTPS");
        assertThatThrownBy(() -> validator.validate("file:///etc/passwd"))
            .isInstanceOf(FiscalException.class);
    }

    @Test
    void shouldRejectHostsOutsideAllowlist() {
        assertThatThrownBy(() -> validator.validate("https://evil.com/qrcode"))
            .isInstanceOf(FiscalException.class).hasMessageContaining("not allowed");
        // suffix trick: must not match "evilgov.br"
        assertThatThrownBy(() -> validator.validate("https://evilgov.br/qrcode"))
            .isInstanceOf(FiscalException.class);
        assertThatThrownBy(() -> validator.validate("https://sefaz.gov.br.evil.com/qrcode"))
            .isInstanceOf(FiscalException.class);
    }

    @Test
    void shouldRejectInternalTargetsAndCredentials() {
        assertThatThrownBy(() -> validator.validate("https://169.254.169.254/latest/meta-data"))
            .isInstanceOf(FiscalException.class);
        assertThatThrownBy(() -> validator.validate("https://localhost/admin"))
            .isInstanceOf(FiscalException.class);
        assertThatThrownBy(() -> validator.validate("https://user:pw@www.sefaz.sp.gov.br/x"))
            .isInstanceOf(FiscalException.class).hasMessageContaining("credentials");
    }

    @Test
    void shouldAcceptIpLiteralOnlyIfHostAllowed() {
        // IP literals never match a domain suffix
        assertThatThrownBy(() -> validator.validate("https://8.8.8.8/x")).isInstanceOf(FiscalException.class);
    }

    @Test
    void shouldAcceptAllowedHostWithoutDnsWhenLiteralUnnecessary() {
        // Host check passes; DNS resolution may fail offline (BAD_GATEWAY) but never "not allowed".
        assertThatCode(() -> {
            try {
                validator.validate("https://www.nfce.fazenda.sp.gov.br/qrcode?p=1");
            } catch (FiscalException e) {
                if (e.getMessage().contains("not allowed") || e.getMessage().contains("HTTPS")) throw e;
            }
        }).doesNotThrowAnyException();
    }
}
