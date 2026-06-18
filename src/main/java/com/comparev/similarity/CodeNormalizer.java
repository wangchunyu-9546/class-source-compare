package com.comparev.similarity;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CodeNormalizer {
    private static final Pattern CALL_PATTERN = Pattern.compile("\\b([a-zA-Z_$][a-zA-Z0-9_$]*)\\s*\\(");

    private CodeNormalizer() {
    }

    public static String normalizeText(String body) {
        if (body == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (String line : splitLines(body)) {
            builder.append(normalizeComparableLine(line));
        }
        return builder.toString();
    }

    public static String normalizeComparableLine(String line) {
        String trimmed = line == null ? "" : line.trim();
        if (isIgnoredLine(trimmed)) {
            return "";
        }
        int commentIndex = trimmed.indexOf("//");
        if (commentIndex >= 0) {
            trimmed = trimmed.substring(0, commentIndex).trim();
        }
        return trimmed.replaceAll("<[^<>]*>", "")
                .replaceAll("\\((?:[A-Z][a-zA-Z0-9_$.]*(?:\\[\\])?|byte|short|int|long|float|double|boolean|char)\\)\\s*", "")
                .replaceAll("\\s+", "")
                .replace("this.", "")
                .replace("(Object)", "");
    }

    public static List<String> extractCallSequence(String body) {
        List<String> calls = new ArrayList<>();
        Matcher matcher = CALL_PATTERN.matcher(body == null ? "" : body);
        while (matcher.find()) {
            String call = matcher.group(1);
            if (!isNonBusinessCall(call)) {
                calls.add(call);
            }
        }
        return calls;
    }

    public static List<String> splitLines(String text) {
        String normalized = text == null ? "" : text.replace("\r\n", "\n").replace('\r', '\n');
        String[] parts = normalized.split("\n", -1);
        List<String> lines = new ArrayList<>();
        for (String part : parts) {
            lines.add(part);
        }
        return lines;
    }

    private static boolean isIgnoredLine(String trimmed) {
        return trimmed.isEmpty()
                || trimmed.startsWith("//")
                || trimmed.startsWith("/*")
                || trimmed.startsWith("*")
                || trimmed.endsWith("*/");
    }

    private static boolean isNonBusinessCall(String call) {
        return "if".equals(call)
                || "for".equals(call)
                || "while".equals(call)
                || "switch".equals(call)
                || "catch".equals(call)
                || "return".equals(call)
                || "throw".equals(call)
                || "new".equals(call)
                || "super".equals(call)
                || "this".equals(call)
                || "synchronized".equals(call);
    }
}
