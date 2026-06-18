package com.comparev.similarity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodeNormalizerTest {
    @Test
    void normalizesQualifiedTypesGenericsCastsAndSlashConstants() {
        String fieldLine = "java.util.zip.ZipEntry entry = (java.util.zip.ZipEntry) entries.nextElement();";
        String sourceLine = "ZipEntry entry = entries.nextElement();";

        assertEquals(CodeNormalizer.normalizeComparableLine(sourceLine), CodeNormalizer.normalizeComparableLine(fieldLine));
    }

    @Test
    void normalizesNestedGenerics() {
        String fieldLine = "Enumeration<? extends java.util.zip.ZipEntry> entries = zip.entries();";
        String sourceLine = "Enumeration entries = zip.entries();";

        assertEquals(CodeNormalizer.normalizeComparableLine(sourceLine), CodeNormalizer.normalizeComparableLine(fieldLine));
    }

    @Test
    void normalizesCastedReceiverCalls() {
        String fieldLine = "((OutputStream) out).write(buf1, 0, len);";
        String sourceLine = "out.write(buf1, 0, len);";

        assertEquals(CodeNormalizer.normalizeComparableLine(sourceLine), CodeNormalizer.normalizeComparableLine(fieldLine));
    }

    @Test
    void normalizesSlashAsciiConstant() {
        String fieldLine = "outPath.substring(0, outPath.lastIndexOf(47));";
        String sourceLine = "outPath.substring(0, outPath.lastIndexOf('/'));";

        assertEquals(CodeNormalizer.normalizeComparableLine(sourceLine), CodeNormalizer.normalizeComparableLine(fieldLine));
    }

    @Test
    void identifiesSimilarDeclarationLines() {
        String fieldLine = "FileOutputStream out = PubFunc.getFileOutputStream(outPath);";
        String sourceLine = "OutputStream out = PubFunc.getFileOutputStream(outPath);";

        assertTrue(CodeNormalizer.lineSimilarity(fieldLine, sourceLine) >= 72);
    }

    @Test
    void normalizesDotZeroNumbers() {
        assertEquals(
                CodeNormalizer.normalizeComparableLine("watermark.setWidth(100);"),
                CodeNormalizer.normalizeComparableLine("watermark.setWidth(100.0);"));
        assertEquals(
                CodeNormalizer.normalizeComparableLine("watermark.setRotation(-40);"),
                CodeNormalizer.normalizeComparableLine("watermark.setRotation(-40.0);"));
    }

    @Test
    void treatsNumberAndEnumArgumentAsProbablyEquivalent() {
        assertTrue(CodeNormalizer.isProbablyEquivalentCallLine(
                "watermark.setRelativeHorizontalPosition(1);",
                "watermark.setRelativeHorizontalPosition(RelativeHorizontalPosition.PAGE);"));
        assertTrue(CodeNormalizer.isProbablyEquivalentCallLine(
                "Shape watermark = new Shape((DocumentBase) doc, 136);",
                "Shape watermark = new Shape(doc, ShapeType.TEXT_PLAIN_TEXT);"));
    }

    @Test
    void doesNotTreatDifferentCallsAsProbablyEquivalent() {
        org.junit.jupiter.api.Assertions.assertFalse(CodeNormalizer.isProbablyEquivalentCallLine(
                "out.write(buf1, 0, len);",
                "in.read(buf1);"));
    }
}
