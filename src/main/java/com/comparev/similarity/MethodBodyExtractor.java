package com.comparev.similarity;

import com.comparev.model.MethodInfo;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class MethodBodyExtractor {
    public Map<String, MethodBodyInfo> extract(Path sourceFile) throws IOException {
        return extract(StaticJavaParser.parse(sourceFile));
    }

    public Map<String, MethodBodyInfo> extract(String sourceText) {
        return extract(StaticJavaParser.parse(sourceText));
    }

    private Map<String, MethodBodyInfo> extract(CompilationUnit compilationUnit) {
        String packageName = compilationUnit.getPackageDeclaration()
                .map(packageDeclaration -> packageDeclaration.getName().asString())
                .orElse("");
        Map<String, MethodBodyInfo> methods = new LinkedHashMap<>();
        for (ClassOrInterfaceDeclaration declaration : compilationUnit.findAll(ClassOrInterfaceDeclaration.class)) {
            String className = qualifiedClassName(packageName, declaration);
            for (MethodDeclaration methodDeclaration : declaration.getMethods()) {
                MethodInfo methodInfo = toMethodInfo(methodDeclaration);
                String body = methodDeclaration.getBody()
                        .map(Object::toString)
                        .orElse("");
                methods.put(methodKey(className, methodInfo), new MethodBodyInfo(className, methodInfo, body));
            }
        }
        return methods;
    }

    public static String methodKey(String className, MethodInfo methodInfo) {
        return className + "#" + methodInfo.parameterSignatureKey();
    }

    private String qualifiedClassName(String packageName, ClassOrInterfaceDeclaration declaration) {
        List<String> names = new ArrayList<>();
        Optional<ClassOrInterfaceDeclaration> current = Optional.of(declaration);
        while (current.isPresent()) {
            names.add(0, current.get().getNameAsString());
            current = current.get().findAncestor(ClassOrInterfaceDeclaration.class);
        }
        String localName = String.join("$", names);
        return packageName.trim().isEmpty() ? localName : packageName + "." + localName;
    }

    private MethodInfo toMethodInfo(MethodDeclaration declaration) {
        List<String> parameterTypes = declaration.getParameters().stream()
                .map(parameter -> parameter.getType().asString())
                .collect(Collectors.toList());
        return new MethodInfo(
                declaration.getNameAsString(),
                parameterTypes,
                declaration.getType().asString(),
                declaration.getAccessSpecifier().asString().trim().isEmpty()
                        ? "package-private"
                        : declaration.getAccessSpecifier().asString(),
                declaration.isStatic());
    }
}
