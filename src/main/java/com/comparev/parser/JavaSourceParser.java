package com.comparev.parser;

import com.comparev.model.ClassInfo;
import com.comparev.model.MethodInfo;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class JavaSourceParser {
    public List<ClassInfo> parse(Path sourceFile) throws IOException {
        CompilationUnit compilationUnit = StaticJavaParser.parse(sourceFile);
        String packageName = compilationUnit.getPackageDeclaration()
                .map(packageDeclaration -> packageDeclaration.getName().asString())
                .orElse("");
        List<ClassInfo> classes = new ArrayList<>();

        for (ClassOrInterfaceDeclaration declaration : compilationUnit.findAll(ClassOrInterfaceDeclaration.class)) {
            String className = qualifiedClassName(packageName, declaration);
            List<MethodInfo> methods = declaration.getMethods().stream()
                    .map(this::toMethodInfo)
                    .collect(Collectors.toList());
            classes.add(new ClassInfo(className, packageName, sourceFile, methods));
        }

        return classes;
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
