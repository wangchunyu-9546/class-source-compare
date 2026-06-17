package com.comparev.similarity;

import com.comparev.model.Severity;

public class MethodSimilarityResult {
    private final int score;
    private final String risk;
    private final Severity severity;
    private final String summary;

    public MethodSimilarityResult(int score, String risk, Severity severity, String summary) {
        this.score = score;
        this.risk = risk;
        this.severity = severity;
        this.summary = summary;
    }

    public int score() {
        return score;
    }

    public String risk() {
        return risk;
    }

    public Severity severity() {
        return severity;
    }

    public String summary() {
        return summary;
    }
}
