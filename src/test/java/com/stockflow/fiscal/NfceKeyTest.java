package com.stockflow.fiscal;

import com.stockflow.fiscal.dto.NfceKey;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;

class NfceKeyTest {

    private static final String VALID = "35240911222333000181650010000001231000000454";

    @Test
    void parse_shouldDecodeFieldsOfValidKey() {
        NfceKey key = NfceKey.parse(VALID).orElseThrow();

        assertThat(key.ufCode()).isEqualTo("35");
        assertThat(key.issueMonth()).isEqualTo(YearMonth.of(2024, 9));
        assertThat(key.cnpj()).isEqualTo("11222333000181");
        assertThat(key.model()).isEqualTo("65");
        assertThat(key.series()).isEqualTo("001");
        assertThat(key.number()).isEqualTo("000000123");
    }

    @Test
    void parse_shouldRejectWrongCheckDigit() {
        assertThat(NfceKey.parse(VALID.substring(0, 43) + "0")).isEmpty();
    }

    @Test
    void parse_shouldRejectBadLengthOrNonDigits() {
        assertThat(NfceKey.parse(null)).isEmpty();
        assertThat(NfceKey.parse(VALID.substring(1))).isEmpty();
        assertThat(NfceKey.parse(VALID.substring(0, 43) + "x")).isEmpty();
    }

    @Test
    void extractRaw_shouldReadKeyFromPParamOfNfceUrl() {
        String url = "https://www.nfce.fazenda.sp.gov.br/qrcode?p=" + VALID + "|2|1|1|ABCDEF0123";
        assertThat(NfceKey.extractRaw(url)).contains(VALID);
    }

    @Test
    void extractRaw_shouldReadUrlEncodedSeparatorAndOtherParams() {
        assertThat(NfceKey.extractRaw("https://x.gov.br/q?p=" + VALID + "%7C2%7C1")).contains(VALID);
        assertThat(NfceKey.extractRaw("https://x.gov.br/q?chNFe=" + VALID + "&nVersao=100")).contains(VALID);
    }

    @Test
    void extractRaw_shouldAcceptBareKeyAndIgnoreLongerDigitRuns() {
        assertThat(NfceKey.extractRaw(" " + VALID + " ")).contains(VALID);
        assertThat(NfceKey.extractRaw(VALID + "9")).isEmpty();
        assertThat(NfceKey.extractRaw("https://x.gov.br/q?id=123")).isEmpty();
        assertThat(NfceKey.extractRaw(null)).isEmpty();
    }
}
