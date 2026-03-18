package org.app.mintonmatchapi.common.util;

public final class StringUtils {

    private StringUtils() {
    }

    /**
     * 문자열을 trim하고, null 또는 공백이면 null 반환
     */
    public static String trimOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
