package com.comparev.similarity;

import com.comparev.model.Severity;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MethodSimilarityAnalyzer {
    private static final Set<String> CONTROL_WORDS = new HashSet<>(Arrays.asList(
            "if", "else", "for", "while", "do", "switch", "case", "try", "catch", "finally", "return", "throw", "throws"));
    private static final Set<String> NON_CALL_WORDS = new HashSet<>(Arrays.asList(
            "if", "for", "while", "switch", "catch", "return", "throw", "new", "super", "this", "synchronized"));

    public MethodSimilarityResult analyze(String fieldBody, String sourceBody) {
        int textScore = textSimilarity(normalizeText(fieldBody), normalizeText(sourceBody));
        int callScore = setSimilarity(extractCalls(fieldBody), extractCalls(sourceBody));
        int constantScore = setSimilarity(extractConstants(fieldBody), extractConstants(sourceBody));
        int controlScore = setSimilarity(extractControls(fieldBody), extractControls(sourceBody));
        int score = Math.round(textScore * 0.40f + callScore * 0.25f + constantScore * 0.20f + controlScore * 0.15f);
        return new MethodSimilarityResult(score, risk(score), severity(score), summary(textScore, callScore, constantScore, controlScore));
    }

    private String risk(int score) {
        if (score >= 90) {
            return "高度一致";
        }
        if (score >= 70) {
            return "基本一致，需人工确认";
        }
        if (score >= 40) {
            return "差异较大";
        }
        return "疑似不一致";
    }

    private Severity severity(int score) {
        if (score >= 90) {
            return Severity.INFO;
        }
        if (score >= 40) {
            return Severity.WARNING;
        }
        return Severity.ERROR;
    }

    private String summary(int textScore, int callScore, int constantScore, int controlScore) {
        return "文本 " + textScore + "，调用 " + callScore + "，常量 " + constantScore + "，控制结构 " + controlScore;
    }

    private String normalizeText(String body) {
        if (body == null) {
            return "";
        }
        return body.replaceAll("/\\*.*?\\*/", "")
                .replaceAll("//.*", "")
                .replaceAll("\\s+", "")
                .trim();
    }

    private int textSimilarity(String left, String right) {
        if (left.isEmpty() && right.isEmpty()) {
            return 100;
        }
        int maxLength = Math.max(left.length(), right.length());
        if (maxLength == 0) {
            return 100;
        }
        int distance = levenshtein(left, right);
        return Math.max(0, Math.round((1.0f - (float) distance / maxLength) * 100));
    }

    private int levenshtein(String left, String right) {
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

    private Set<String> extractCalls(String body) {
        Set<String> calls = new HashSet<>();
        Matcher matcher = Pattern.compile("\\b([a-zA-Z_$][a-zA-Z0-9_$]*)\\s*\\(").matcher(nullToEmpty(body));
        while (matcher.find()) {
            String token = matcher.group(1);
            if (!NON_CALL_WORDS.contains(token)) {
                calls.add(token);
            }
        }
        return calls;
    }

    private Set<String> extractConstants(String body) {
        Set<String> constants = new HashSet<>();
        Matcher matcher = Pattern.compile("\"([^\"\\\\]|\\\\.)*\"|'([^'\\\\]|\\\\.)*'|\\b\\d+(?:\\.\\d+)?\\b").matcher(nullToEmpty(body));
        while (matcher.find()) {
            constants.add(matcher.group());
        }
        return constants;
    }

    private Set<String> extractControls(String body) {
        Set<String> controls = new HashSet<>();
        Matcher matcher = Pattern.compile("\\b[a-zA-Z_$][a-zA-Z0-9_$]*\\b").matcher(nullToEmpty(body));
        while (matcher.find()) {
            String token = matcher.group();
            if (CONTROL_WORDS.contains(token)) {
                controls.add(token);
            }
        }
        return controls;
    }

    private int setSimilarity(Set<String> left, Set<String> right) {
        if (left.isEmpty() && right.isEmpty()) {
            return 100;
        }
        Set<String> union = new HashSet<>(left);
        union.addAll(right);
        Set<String> intersection = new HashSet<>(left);
        intersection.retainAll(right);
        return Math.round((float) intersection.size() / union.size() * 100);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
