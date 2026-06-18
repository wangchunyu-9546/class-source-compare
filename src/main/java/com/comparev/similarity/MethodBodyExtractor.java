package com.comparev.similarity;

import com.comparev.model.ClassInfo;
import com.comparev.model.MethodInfo;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MethodBodyExtractor {
    public Map<String, MethodBodyInfo> extract(Path sourceFile) throws IOException {
        return extract(StaticJavaParser.parse(sourceFile));
    }

    public Map<String, MethodBodyInfo> extract(Path sourceFile, ClassInfo classInfo) throws IOException {
        String sourceText = new String(Files.readAllBytes(sourceFile), StandardCharsets.UTF_8);
        return extract(sourceText, classInfo);
    }

    public Map<String, MethodBodyInfo> extract(String sourceText) {
        return extract(StaticJavaParser.parse(sourceText));
    }

    public Map<String, MethodBodyInfo> extract(String sourceText, ClassInfo classInfo) {
        try {
            return extract(sourceText);
        } catch (RuntimeException exception) {
            return fallbackExtract(sourceText, classInfo);
        }
    }

    private Map<String, MethodBodyInfo> extract(CompilationUnit compilationUnit) {
        String packageName = compilationUnit.getPackageDeclaration()
                .map(packageDeclaration -> packageDeclaration.getName().asString())
                .orElse("");
        Map<String, MethodBodyInfo> methods = new LinkedHashMap<>();
        for (ClassOrInterfaceDeclaration declaration : compilationUnit.findAll(ClassOrInterfaceDeclaration.class)) {
            String className = qualifiedClassName(packageName, declaration);
            for (MethodDeclaration methodDeclaration : declaration.getMethods()) {
                MethodInfo methodInfo = toMethodInfo(methodDeclaration, declaration.isInterface());
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

    private Map<String, MethodBodyInfo> fallbackExtract(String sourceText, ClassInfo classInfo) {
        Map<String, MethodBodyInfo> methods = new LinkedHashMap<>();
        String text = sourceText == null ? "" : sourceText;
        for (MethodInfo methodInfo : classInfo.methods()) {
            String body = findMethodBody(text, methodInfo);
            if (!body.isEmpty()) {
                methods.put(methodKey(classInfo.className(), methodInfo), new MethodBodyInfo(classInfo.className(), methodInfo, body));
            }
        }
        return methods;
    }

    private String findMethodBody(String sourceText, MethodInfo methodInfo) {
        Pattern pattern = Pattern.compile("\\b" + Pattern.quote(methodInfo.methodName()) + "\\s*\\(");
        Matcher matcher = pattern.matcher(sourceText);
        while (matcher.find()) {
            if (matcher.start() > 0 && sourceText.charAt(matcher.start() - 1) == '.') {
                continue;
            }
            int openParen = sourceText.indexOf('(', matcher.start());
            int closeParen = findMatching(sourceText, openParen, '(', ')');
            if (closeParen < 0 || parameterCount(sourceText.substring(openParen + 1, closeParen)) != methodInfo.parameterTypes().size()) {
                continue;
            }
            int bodyStart = findBodyStart(sourceText, closeParen + 1);
            if (bodyStart < 0) {
                continue;
            }
            int bodyEnd = findMatching(sourceText, bodyStart, '{', '}');
            if (bodyEnd > bodyStart) {
                return sourceText.substring(bodyStart, bodyEnd + 1);
            }
        }
        return "";
    }

    private int findBodyStart(String sourceText, int startIndex) {
        for (int index = startIndex; index < sourceText.length(); index++) {
            char current = sourceText.charAt(index);
            if (current == '{') {
                return index;
            }
            if (current == ';') {
                return -1;
            }
        }
        return -1;
    }

    private int findMatching(String sourceText, int startIndex, char open, char close) {
        if (startIndex < 0 || startIndex >= sourceText.length() || sourceText.charAt(startIndex) != open) {
            return -1;
        }
        int depth = 0;
        boolean inString = false;
        boolean inChar = false;
        boolean escaped = false;
        for (int index = startIndex; index < sourceText.length(); index++) {
            char current = sourceText.charAt(index);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (current == '\\') {
                escaped = true;
                continue;
            }
            if (current == '"' && !inChar) {
                inString = !inString;
                continue;
            }
            if (current == '\'' && !inString) {
                inChar = !inChar;
                continue;
            }
            if (inString || inChar) {
                continue;
            }
            if (current == open) {
                depth++;
            } else if (current == close) {
                depth--;
                if (depth == 0) {
                    return index;
                }
            }
        }
        return -1;
    }

    private int parameterCount(String parameters) {
        String trimmed = parameters.trim();
        if (trimmed.isEmpty()) {
            return 0;
        }
        int count = 1;
        int genericDepth = 0;
        for (int index = 0; index < trimmed.length(); index++) {
            char current = trimmed.charAt(index);
            if (current == '<') {
                genericDepth++;
            } else if (current == '>') {
                genericDepth = Math.max(0, genericDepth - 1);
            } else if (current == ',' && genericDepth == 0) {
                count++;
            }
        }
        return count;
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

    private MethodInfo toMethodInfo(MethodDeclaration declaration, boolean interfaceMethod) {
        List<String> parameterTypes = declaration.getParameters().stream()
                .map(parameter -> parameter.getType().asString())
                .collect(Collectors.toList());
        String accessModifier = declaration.getAccessSpecifier().asString().trim();
        if (accessModifier.isEmpty()) {
            accessModifier = interfaceMethod ? "public" : "package-private";
        }
        return new MethodInfo(
                declaration.getNameAsString(),
                parameterTypes,
                declaration.getType().asString(),
                accessModifier,
                declaration.isStatic());
    }
}
