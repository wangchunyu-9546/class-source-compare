package com.comparev.model;

import java.nio.file.Path;
import java.util.List;

public class ClassInfo {
    private final String className;
    private final String packageName;
    private final Path sourcePath;
    private final List<MethodInfo> methods;

    public ClassInfo(String className, String packageName, Path sourcePath, List<MethodInfo> methods) {
        this.className = className;
        this.packageName = packageName;
        this.sourcePath = sourcePath;
        this.methods = methods;
    }

    public String className() {
        return className;
    }

    public String packageName() {
        return packageName;
    }

    public Path sourcePath() {
        return sourcePath;
    }

    public List<MethodInfo> methods() {
        return methods;
    }
}
