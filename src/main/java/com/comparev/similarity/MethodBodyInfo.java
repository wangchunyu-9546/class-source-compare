package com.comparev.similarity;

import com.comparev.model.MethodInfo;

public class MethodBodyInfo {
    private final String className;
    private final MethodInfo methodInfo;
    private final String body;

    public MethodBodyInfo(String className, MethodInfo methodInfo, String body) {
        this.className = className;
        this.methodInfo = methodInfo;
        this.body = body;
    }

    public String className() {
        return className;
    }

    public MethodInfo methodInfo() {
        return methodInfo;
    }

    public String body() {
        return body;
    }
}
