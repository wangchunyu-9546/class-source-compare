package com.comparev.decompile;

import java.nio.file.Path;

public class DecompiledSource {
    private final String className;
    private final Path sourcePath;
    private final String sourceText;

    public DecompiledSource(String className, Path sourcePath, String sourceText) {
        this.className = className;
        this.sourcePath = sourcePath;
        this.sourceText = sourceText;
    }

    public String className() {
        return className;
    }

    public Path sourcePath() {
        return sourcePath;
    }

    public String sourceText() {
        return sourceText;
    }
}
