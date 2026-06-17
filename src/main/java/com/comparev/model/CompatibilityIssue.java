package com.comparev.model;

public class CompatibilityIssue {
    private final String className;
    private final IssueType issueType;
    private final String classMethod;
    private final String sourceMethod;
    private final String message;
    private final Severity severity;
    private final Integer similarityScore;
    private final String implementationRisk;
    private final String fieldImplementation;
    private final String sourceImplementation;

    public CompatibilityIssue(String className, IssueType issueType, String classMethod, String sourceMethod, String message, Severity severity) {
        this(className, issueType, classMethod, sourceMethod, message, severity, null, "", "", "");
    }

    public CompatibilityIssue(
            String className,
            IssueType issueType,
            String classMethod,
            String sourceMethod,
            String message,
            Severity severity,
            Integer similarityScore,
            String implementationRisk,
            String fieldImplementation,
            String sourceImplementation) {
        this.className = className;
        this.issueType = issueType;
        this.classMethod = classMethod;
        this.sourceMethod = sourceMethod;
        this.message = message;
        this.severity = severity;
        this.similarityScore = similarityScore;
        this.implementationRisk = implementationRisk;
        this.fieldImplementation = fieldImplementation;
        this.sourceImplementation = sourceImplementation;
    }

    public String className() {
        return className;
    }

    public IssueType issueType() {
        return issueType;
    }

    public String classMethod() {
        return classMethod;
    }

    public String sourceMethod() {
        return sourceMethod;
    }

    public String message() {
        return message;
    }

    public Severity severity() {
        return severity;
    }

    public Integer similarityScore() {
        return similarityScore;
    }

    public String implementationRisk() {
        return implementationRisk;
    }

    public String fieldImplementation() {
        return fieldImplementation;
    }

    public String sourceImplementation() {
        return sourceImplementation;
    }
}
