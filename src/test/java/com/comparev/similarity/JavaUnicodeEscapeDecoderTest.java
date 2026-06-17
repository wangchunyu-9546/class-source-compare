package com.comparev.similarity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JavaUnicodeEscapeDecoderTest {
    @Test
    void decodesJavaUnicodeEscapes() {
        String value = "\"\\u67e5\\u770b\\u4e86\"";

        assertEquals("\"查看了\"", JavaUnicodeEscapeDecoder.decode(value));
    }

    @Test
    void keepsInvalidEscapesUnchanged() {
        String value = "\"\\uZZZZ\"";

        assertEquals(value, JavaUnicodeEscapeDecoder.decode(value));
    }
}
