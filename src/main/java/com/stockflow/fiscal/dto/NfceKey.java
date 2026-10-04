package com.stockflow.fiscal.dto;

import java.time.YearMonth;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * NF-e / NFC-e access key (44 digits). Layout:
 * UF(2) AAMM(4) CNPJ(14) modelo(2) serie(3) numero(9) tpEmis(1) codigo(8) DV(1).
 */
public record NfceKey(
    String value,
    String ufCode,
    YearMonth issueMonth,
    String cnpj,
    String model,
    String series,
    String number
) {

    private static final Pattern KEY_PATTERN = Pattern.compile("(?<!\\d)\\d{44}(?!\\d)");
    private static final Pattern PARAM_P_PATTERN = Pattern.compile("[?&]p=([^&#\\s]+)", Pattern.CASE_INSENSITIVE);

    /**
     * Extracts a key from QR content: the first field of the {@code p=} query param
     * (chave|versao|ambiente|idToken|hash), or any standalone 44-digit sequence.
     * Returns empty when no 44-digit candidate exists; does not validate the check digit.
     */
    public static Optional<String> extractRaw(String content) {
        if (content == null || content.isBlank()) return Optional.empty();

        Matcher p = PARAM_P_PATTERN.matcher(content);
        if (p.find()) {
            String first = p.group(1).split("(\\||%7C|%7c)", 2)[0];
            if (first.matches("\\d{44}")) return Optional.of(first);
        }

        Matcher m = KEY_PATTERN.matcher(content);
        return m.find() ? Optional.of(m.group()) : Optional.empty();
    }

    /** Parses and validates (length + mod 11 check digit). Empty if invalid. */
    public static Optional<NfceKey> parse(String key) {
        if (key == null || !key.matches("\\d{44}") || !hasValidCheckDigit(key)) {
            return Optional.empty();
        }
        int year = 2000 + Integer.parseInt(key.substring(2, 4));
        int month = Integer.parseInt(key.substring(4, 6));
        if (month < 1 || month > 12) return Optional.empty();

        return Optional.of(new NfceKey(
            key,
            key.substring(0, 2),
            YearMonth.of(year, month),
            key.substring(6, 20),
            key.substring(20, 22),
            key.substring(22, 25),
            key.substring(25, 34)
        ));
    }

    static boolean hasValidCheckDigit(String key) {
        int sum = 0;
        int weight = 2;
        for (int i = 42; i >= 0; i--) {
            sum += (key.charAt(i) - '0') * weight;
            weight = weight == 9 ? 2 : weight + 1;
        }
        int remainder = sum % 11;
        int expected = remainder < 2 ? 0 : 11 - remainder;
        return expected == key.charAt(43) - '0';
    }
}
