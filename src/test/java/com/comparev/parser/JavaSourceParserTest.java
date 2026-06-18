package com.comparev.parser;

import com.comparev.model.ClassInfo;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JavaSourceParserTest {
    @Test
    void treatsImplicitInterfaceMethodsAsPublic() throws Exception {
        Path sourceFile = Files.createTempFile("IExample", ".java");
        Files.write(sourceFile, "package demo; public interface IExample { void save(String id); }".getBytes(StandardCharsets.UTF_8));

        ClassInfo classInfo = new JavaSourceParser().parse(sourceFile).get(0);

        assertEquals("public", classInfo.methods().get(0).accessModifier());
    }

    @Test
    void keepsImplicitClassMethodsAsPackagePrivate() throws Exception {
        Path sourceFile = Files.createTempFile("Example", ".java");
        Files.write(sourceFile, "package demo; public class Example { void save(String id) {} }".getBytes(StandardCharsets.UTF_8));

        ClassInfo classInfo = new JavaSourceParser().parse(sourceFile).get(0);

        assertEquals("package-private", classInfo.methods().get(0).accessModifier());
    }
}
