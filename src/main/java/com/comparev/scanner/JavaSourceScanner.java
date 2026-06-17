package com.comparev.scanner;

import com.comparev.model.ClassInfo;
import com.comparev.parser.JavaSourceParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JavaSourceScanner {
    private final JavaSourceParser parser = new JavaSourceParser();

    public Map<String, ClassInfo> scan(Path path) throws IOException {
        return scan(Collections.singletonList(path));
    }

    public Map<String, ClassInfo> scan(List<Path> inputPaths) throws IOException {
        Map<String, ClassInfo> classes = new LinkedHashMap<>();
        for (Path inputPath : inputPaths) {
            scanPath(inputPath, classes);
        }
        return classes;
    }

    private void scanPath(Path inputPath, Map<String, ClassInfo> classes) throws IOException {
        if (Files.isDirectory(inputPath)) {
            try (Stream<Path> paths = Files.walk(inputPath)) {
                List<Path> sourceFiles = paths.filter(Files::isRegularFile)
                        .filter(this::isJavaFile)
                        .collect(Collectors.toList());
                for (Path sourceFile : sourceFiles) {
                    putClasses(sourceFile, classes);
                }
            }
            return;
        }

        if (Files.isRegularFile(inputPath) && isJavaFile(inputPath)) {
            putClasses(inputPath, classes);
        }
    }

    private void putClasses(Path sourceFile, Map<String, ClassInfo> classes) throws IOException {
        for (ClassInfo classInfo : parser.parse(sourceFile)) {
            classes.put(classInfo.className(), classInfo);
        }
    }

    private boolean isJavaFile(Path path) {
        return path.toString().toLowerCase().endsWith(".java");
    }
}
