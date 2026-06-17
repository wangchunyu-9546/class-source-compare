package com.comparev.model;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class MethodInfo {
    private final String methodName;
    private final List<String> parameterTypes;
    private final String returnType;
    private final String accessModifier;
    private final boolean staticMethod;

    public MethodInfo(String methodName, List<String> parameterTypes, String returnType, String accessModifier, boolean staticMethod) {
        this.methodName = methodName;
        this.parameterTypes = parameterTypes;
        this.returnType = returnType;
        this.accessModifier = accessModifier;
        this.staticMethod = staticMethod;
    }

    public String methodName() {
        return methodName;
    }

    public List<String> parameterTypes() {
        return parameterTypes;
    }

    public String returnType() {
        return returnType;
    }

    public String accessModifier() {
        return accessModifier;
    }

    public boolean staticMethod() {
        return staticMethod;
    }

    public String parameterSignatureKey() {
        return methodName + "(" + parameterTypes.stream()
                .map(MethodInfo::normalizeType)
                .collect(Collectors.joining(",")) + ")";
    }

    public String displaySignature() {
        String parameters = String.join(", ", parameterTypes);
        String staticText = staticMethod ? " static" : "";
        return accessModifier + staticText + " " + returnType + " " + methodName + "(" + parameters + ")";
    }

    public boolean sameReturnType(MethodInfo other) {
        return Objects.equals(normalizeType(returnType), normalizeType(other.returnType));
    }

    public static String normalizeType(String type) {
        if (type == null || type.trim().isEmpty()) {
            return "";
        }
        String normalized = type.replace("...", "[]").replaceAll("<.*>", "").trim();
        int arrayDepth = 0;
        while (normalized.endsWith("[]")) {
            arrayDepth++;
            normalized = normalized.substring(0, normalized.length() - 2);
        }
        int dotIndex = normalized.lastIndexOf('.');
        if (dotIndex >= 0) {
            normalized = normalized.substring(dotIndex + 1);
        }
        StringBuilder builder = new StringBuilder(normalized);
        for (int index = 0; index < arrayDepth; index++) {
            builder.append("[]");
        }
        return builder.toString();
    }
}
