package com.comparev.similarity;

public final class JavaUnicodeEscapeDecoder {
    private JavaUnicodeEscapeDecoder() {
    }

    public static String decode(String value) {
        if (value == null || value.indexOf("\\u") < 0) {
            return value;
        }

        StringBuilder decoded = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (current == '\\' && index + 5 < value.length() && value.charAt(index + 1) == 'u') {
                int unicodeStart = index + 2;
                int unicodeEnd = unicodeStart + 4;
                String hex = value.substring(unicodeStart, unicodeEnd);
                if (isHex(hex)) {
                    decoded.append((char) Integer.parseInt(hex, 16));
                    index = unicodeEnd - 1;
                    continue;
                }
            }
            decoded.append(current);
        }
        return decoded.toString();
    }

    private static boolean isHex(String value) {
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            boolean digit = current >= '0' && current <= '9';
            boolean lower = current >= 'a' && current <= 'f';
            boolean upper = current >= 'A' && current <= 'F';
            if (!digit && !lower && !upper) {
                return false;
            }
        }
        return true;
    }
}
