package com.comparev.scanner;

import com.comparev.model.ClassInfo;
import com.comparev.parser.ClassMetadataParser;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ClassFileScanner {
    private final ClassMetadataParser parser = new ClassMetadataParser();

    public Map<String, ClassInfo> scan(Path path) throws IOException {
        return scan(Collections.singletonList(path), false);
    }

    public Map<String, ClassInfo> scan(List<Path> inputPaths) throws IOException {
        return scan(inputPaths, false);
    }

    public Map<String, ClassInfo> scan(List<Path> inputPaths, boolean includeAnonymousInnerClasses) throws IOException {
        Map<String, ClassInfo> classes = new LinkedHashMap<>();
        for (Path inputPath : inputPaths) {
            scanPath(inputPath, classes, includeAnonymousInnerClasses);
        }
        return classes;
    }

    private void scanPath(Path inputPath, Map<String, ClassInfo> classes, boolean includeAnonymousInnerClasses) throws IOException {
        if (Files.isDirectory(inputPath)) {
            try (Stream<Path> paths = Files.walk(inputPath)) {
                List<Path> classFiles = paths.filter(Files::isRegularFile)
                        .filter(this::isClassFile)
                        .collect(Collectors.toList());
                for (Path classFile : classFiles) {
                    putClass(classFile, classes, includeAnonymousInnerClasses);
                }
            }
            return;
        }

        if (Files.isRegularFile(inputPath) && isClassFile(inputPath)) {
            putClass(inputPath, classes, includeAnonymousInnerClasses);
            return;
        }

        if (Files.isRegularFile(inputPath) && isJarFile(inputPath)) {
            scanJar(inputPath, classes, includeAnonymousInnerClasses);
        }
    }

    private void scanJar(Path jarFile, Map<String, ClassInfo> classes, boolean includeAnonymousInnerClasses) throws IOException {
        Path outputDirectory = Files.createTempDirectory("comparev-jar-classes-");
        try (JarFile jar = new JarFile(jarFile.toFile())) {
            for (JarEntry entry : Collections.list(jar.entries())) {
                if (entry.isDirectory() || !entry.getName().toLowerCase().endsWith(".class")) {
                    continue;
                }
                Path classFile = outputDirectory.resolve(entry.getName()).normalize();
                if (!classFile.startsWith(outputDirectory)) {
                    continue;
                }
                Files.createDirectories(classFile.getParent());
                try (InputStream inputStream = jar.getInputStream(entry)) {
                    Files.copy(inputStream, classFile, StandardCopyOption.REPLACE_EXISTING);
                }
                putClass(classFile, classes, includeAnonymousInnerClasses);
            }
        }
    }

    private void putClass(Path classFile, Map<String, ClassInfo> classes, boolean includeAnonymousInnerClasses) throws IOException {
        ClassInfo classInfo = parser.parse(classFile);
        if (!includeAnonymousInnerClasses && isAnonymousInnerClassName(classInfo.className())) {
            return;
        }
        classes.put(classInfo.className(), classInfo);
    }

    static boolean isAnonymousInnerClassName(String className) {
        int dollarIndex = className.lastIndexOf('$');
        if (dollarIndex < 0 || dollarIndex == className.length() - 1) {
            return false;
        }
        String innerName = className.substring(dollarIndex + 1);
        for (int index = 0; index < innerName.length(); index++) {
            if (!Character.isDigit(innerName.charAt(index))) {
                return false;
            }
        }
        return true;
    }

    private boolean isClassFile(Path path) {
        return path.toString().toLowerCase().endsWith(".class");
    }

    private boolean isJarFile(Path path) {
        return path.toString().toLowerCase().endsWith(".jar");
    }
}
