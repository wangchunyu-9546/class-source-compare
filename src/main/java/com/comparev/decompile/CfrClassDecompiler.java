package com.comparev.decompile;

import com.comparev.model.ClassInfo;
import org.benf.cfr.reader.Main;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CfrClassDecompiler implements ClassDecompiler {
    private final Path outputDirectory;

    public CfrClassDecompiler() throws IOException {
        this(Files.createTempDirectory("comparev-cfr-"));
    }

    public CfrClassDecompiler(Path outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    @Override
    public DecompiledSource decompile(ClassInfo classInfo) throws IOException {
        Path classOutputDirectory = Files.createTempDirectory(outputDirectory, safeName(classInfo.className()) + "-");
        Main.main(new String[]{
                classInfo.sourcePath().toString(),
                "--outputdir", classOutputDirectory.toString(),
                "--silent", "true"
        });
        Path javaFile = findJavaFile(classOutputDirectory);
        String sourceText = new String(Files.readAllBytes(javaFile), StandardCharsets.UTF_8);
        return new DecompiledSource(classInfo.className(), javaFile, sourceText);
    }

    private Path findJavaFile(Path directory) throws IOException {
        try (Stream<Path> paths = Files.walk(directory)) {
            List<Path> javaFiles = paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().toLowerCase().endsWith(".java"))
                    .sorted(Comparator.comparing(Path::toString))
                    .collect(Collectors.toList());
            if (javaFiles.isEmpty()) {
                throw new IOException("CFR 未生成 Java 文件：" + directory);
            }
            return javaFiles.get(0);
        }
    }

    private String safeName(String className) {
        return className.replaceAll("[^a-zA-Z0-9_.-]", "_");
    }
}
