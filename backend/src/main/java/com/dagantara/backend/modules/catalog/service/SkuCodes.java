package com.dagantara.backend.modules.catalog.service;

import com.dagantara.backend.common.exception.ApiException;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;

public final class SkuCodes {
    private static final int MAX_SKU_LENGTH = 255;
    private static final Pattern SKU = Pattern.compile("[A-Za-z0-9._-]+");
    private static final Pattern SHORT_CODE = Pattern.compile("[A-Za-z0-9]+");

    private SkuCodes() {
    }

    public static String normalizeSku(String input) {
        String sku = input == null ? "" : input.strip();
        if (sku.length() > MAX_SKU_LENGTH || !SKU.matcher(sku).matches()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_SKU",
                    "SKU must contain 1 to 255 ASCII letters, digits, dots, underscores or hyphens");
        }
        return sku.toUpperCase(Locale.ROOT);
    }

    static String normalizeShortCode(String input) {
        if (input == null) {
            return null;
        }
        String code = input.strip();
        if (!SHORT_CODE.matcher(code).matches()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_SHORT_CODE",
                    "Short code must contain only ASCII letters and digits");
        }
        return code.toUpperCase(Locale.ROOT);
    }
}
