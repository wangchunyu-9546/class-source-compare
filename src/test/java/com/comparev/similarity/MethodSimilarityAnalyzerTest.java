package com.comparev.similarity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MethodSimilarityAnalyzerTest {
    private final MethodSimilarityAnalyzer analyzer = new MethodSimilarityAnalyzer();

    @Test
    void scoresIdenticalBodiesAsHighlyConsistent() {
        String body = "{ if (name == null) { return \"\"; } return name.trim(); }";

        MethodSimilarityResult result = analyzer.analyze(body, body);

        assertTrue(result.score() >= 90);
    }

    @Test
    void scoresSmallFormattingDifferencesAsConsistent() {
        String fieldBody = "{ return userService.findName(id); }";
        String sourceBody = "{\n    return userService.findName(id);\n}";

        MethodSimilarityResult result = analyzer.analyze(fieldBody, sourceBody);

        assertTrue(result.score() >= 90);
    }

    @Test
    void scoresDifferentLogicAsRisky() {
        String fieldBody = "{ if (enabled) { return userService.findName(id); } return \"\"; }";
        String sourceBody = "{ logger.info(\"skip\"); return cache.get(id); }";

        MethodSimilarityResult result = analyzer.analyze(fieldBody, sourceBody);

        assertTrue(result.score() < 70);
    }
}
