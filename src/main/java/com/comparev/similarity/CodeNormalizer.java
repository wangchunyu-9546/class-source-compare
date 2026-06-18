package com.comparev.similarity;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CodeNormalizer {
    private static final Pattern CALL_PATTERN = Pattern.compile("\\b([a-zA-Z_$][a-zA-Z0-9_$]*)\\s*\\(");
    private static final Pattern QUALIFIED_TYPE_PATTERN = Pattern.compile("\\b(?:[a-zA-Z_$][a-zA-Z0-9_$]*\\.)+([A-Z][a-zA-Z0-9_$]*)");
    private static final Pattern NUMBER_DOT_ZERO_PATTERN = Pattern.compile("(?<![a-zA-Z0-9_])(-?\\d+)\\.0(?![a-zA-Z0-9_])");

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
        String normalized = removeGenerics(trimmed);
        normalized = QUALIFIED_TYPE_PATTERN.matcher(normalized).replaceAll("$1");
        normalized = normalized.replace("lastIndexOf(47)", "lastIndexOf('/')")
                .replace("indexOf(47)", "indexOf('/')")
                .replace("charAt(47)", "charAt('/')");
        normalized = NUMBER_DOT_ZERO_PATTERN.matcher(normalized).replaceAll("$1");
        return normalized
                .replaceAll("\\((?:[A-Z][a-zA-Z0-9_$.]*(?:\\[\\])?|byte|short|int|long|float|double|boolean|char)\\)\\s*(?=[a-zA-Z_$\\(])", "")
                .replaceAll("\\(([a-zA-Z_$][a-zA-Z0-9_$]*)\\)\\.", "$1.")
                .replaceAll("\\s+", "")
                .replace("this.", "")
                .replace("(Object)", "");
    }

    public static String normalizeLooseLine(String line) {
        String normalized = normalizeComparableLine(line);
        return normalized.replaceAll("^(?:public|private|protected)?(?:static)?(?:final)?[A-Z][a-zA-Z0-9_$\\[\\]]+([a-zA-Z_$][a-zA-Z0-9_$]*=)", "$1")
                .replaceAll("^(?:while|for)\\((.*)\\)\\{?$", "$1")
                .replace(";", "")
                .replace("{", "")
                .replace("}", "");
    }

    public static int lineSimilarity(String left, String right) {
        String normalizedLeft = normalizeLooseLine(left);
        String normalizedRight = normalizeLooseLine(right);
        if (normalizedLeft.isEmpty() && normalizedRight.isEmpty()) {
            return 100;
        }
        int maxLength = Math.max(normalizedLeft.length(), normalizedRight.length());
        if (maxLength == 0) {
            return 100;
        }
        int distance = levenshtein(normalizedLeft, normalizedRight);
        return Math.max(0, Math.round((1.0f - (float) distance / maxLength) * 100));
    }

    public static boolean isProbablyEquivalentCallLine(String left, String right) {
        CallShape leftCall = parsePrimaryCall(left);
        CallShape rightCall = parsePrimaryCall(right);
        if (leftCall == null || rightCall == null || !leftCall.name.equals(rightCall.name)) {
            return false;
        }
        if (Math.abs(leftCall.arguments.size() - rightCall.arguments.size()) > 1) {
            return false;
        }

        int max = Math.max(leftCall.arguments.size(), rightCall.arguments.size());
        int matched = 0;
        int probableConstants = 0;
        for (int index = 0; index < max; index++) {
            String leftArgument = index < leftCall.arguments.size() ? normalizeArgument(leftCall.arguments.get(index)) : "";
            String rightArgument = index < rightCall.arguments.size() ? normalizeArgument(rightCall.arguments.get(index)) : "";
            if (leftArgument.equals(rightArgument)) {
                matched++;
            } else if (isNumberVsConstant(leftArgument, rightArgument)) {
                probableConstants++;
            }
        }
        return probableConstants > 0 && matched + probableConstants >= Math.max(1, max - 1);
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

    private static CallShape parsePrimaryCall(String line) {
        String normalized = normalizeComparableLine(line);
        int equalsIndex = normalized.indexOf('=');
        if (equalsIndex >= 0 && equalsIndex + 1 < normalized.length()) {
            normalized = normalized.substring(equalsIndex + 1);
        }
        if (normalized.startsWith("new")) {
            normalized = normalized.substring(3);
        }
        int openParen = normalized.indexOf('(');
        if (openParen <= 0) {
            return null;
        }
        int closeParen = findMatchingParen(normalized, openParen);
        if (closeParen <= openParen) {
            return null;
        }
        String beforeParen = normalized.substring(0, openParen);
        int dotIndex = beforeParen.lastIndexOf('.');
        String callName = dotIndex >= 0 ? beforeParen.substring(dotIndex + 1) : beforeParen;
        if (callName.isEmpty()) {
            return null;
        }
        return new CallShape(callName, splitArguments(normalized.substring(openParen + 1, closeParen)));
    }

    private static int findMatchingParen(String value, int openParen) {
        int depth = 0;
        for (int index = openParen; index < value.length(); index++) {
            char current = value.charAt(index);
            if (current == '(') {
                depth++;
            } else if (current == ')') {
                depth--;
                if (depth == 0) {
                    return index;
                }
            }
        }
        return -1;
    }

    private static List<String> splitArguments(String arguments) {
        List<String> result = new ArrayList<>();
        if (arguments.trim().isEmpty()) {
            return result;
        }
        int start = 0;
        int depth = 0;
        for (int index = 0; index < arguments.length(); index++) {
            char current = arguments.charAt(index);
            if (current == '(') {
                depth++;
            } else if (current == ')') {
                depth = Math.max(0, depth - 1);
            } else if (current == ',' && depth == 0) {
                result.add(arguments.substring(start, index));
                start = index + 1;
            }
        }
        result.add(arguments.substring(start));
        return result;
    }

    private static String normalizeArgument(String argument) {
        return normalizeComparableLine(argument)
                .replace(";", "")
                .replace("{", "")
                .replace("}", "");
    }

    private static boolean isNumberVsConstant(String left, String right) {
        return (isNumber(left) && isConstantExpression(right)) || (isNumber(right) && isConstantExpression(left));
    }

    private static boolean isNumber(String value) {
        return value.matches("-?\\d+(?:\\.\\d+)?");
    }

    private static boolean isConstantExpression(String value) {
        return value.matches("[A-Z][A-Za-z0-9_$]*(?:\\.[A-Z][A-Za-z0-9_$]*)+")
                || value.matches("[A-Z][A-Z0-9_]+")
                || value.indexOf('.') >= 0 && value.matches(".*[A-Z][A-Z0-9_]+.*");
    }

    private static boolean isIgnoredLine(String trimmed) {
        return trimmed.isEmpty()
                || trimmed.startsWith("//")
                || trimmed.startsWith("/*")
                || trimmed.startsWith("*")
                || trimmed.endsWith("*/");
    }

    private static String removeGenerics(String value) {
        String previous;
        String current = value;
        do {
            previous = current;
            current = current.replaceAll("<[^<>]*>", "");
        } while (!previous.equals(current));
        return current;
    }

    private static int levenshtein(String left, String right) {
        int[] costs = new int[right.length() + 1];
        for (int index = 0; index <= right.length(); index++) {
            costs[index] = index;
        }
        for (int leftIndex = 1; leftIndex <= left.length(); leftIndex++) {
            costs[0] = leftIndex;
            int previous = leftIndex - 1;
            for (int rightIndex = 1; rightIndex <= right.length(); rightIndex++) {
                int current = costs[rightIndex];
                int candidate = left.charAt(leftIndex - 1) == right.charAt(rightIndex - 1)
                        ? previous
                        : Math.min(Math.min(costs[rightIndex - 1], costs[rightIndex]), previous) + 1;
                costs[rightIndex] = candidate;
                previous = current;
            }
        }
        return costs[right.length()];
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

    private static class CallShape {
        private final String name;
        private final List<String> arguments;

        private CallShape(String name, List<String> arguments) {
            this.name = name;
            this.arguments = arguments;
        }
    }
}
