package com.comparev.parser;

import com.comparev.model.ClassInfo;
import com.comparev.model.MethodInfo;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ClassMetadataParser {
    public ClassInfo parse(Path classFile) throws IOException {
        try (InputStream inputStream = Files.newInputStream(classFile)) {
            ClassReader reader = new ClassReader(inputStream);
            ClassNode classNode = new ClassNode();
            reader.accept(classNode, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

            String className = classNode.name.replace('/', '.');
            int packageEnd = className.lastIndexOf('.');
            String packageName = packageEnd >= 0 ? className.substring(0, packageEnd) : "";
            List<MethodInfo> methods = classNode.methods.stream()
                    .map(MethodNode.class::cast)
                    .filter(method -> !method.name.startsWith("<"))
                    .map(this::toMethodInfo)
                    .collect(Collectors.toList());

            return new ClassInfo(className, packageName, classFile, methods);
        }
    }

    private MethodInfo toMethodInfo(MethodNode methodNode) {
        Type methodType = Type.getMethodType(methodNode.desc);
        List<String> parameterTypes = Arrays.stream(methodType.getArgumentTypes())
                .map(Type::getClassName)
                .collect(Collectors.toList());
        String returnType = methodType.getReturnType().getClassName();
        return new MethodInfo(
                methodNode.name,
                parameterTypes,
                returnType,
                accessModifier(methodNode.access),
                (methodNode.access & Opcodes.ACC_STATIC) != 0);
    }

    private String accessModifier(int access) {
        if ((access & Opcodes.ACC_PUBLIC) != 0) {
            return "public";
        }
        if ((access & Opcodes.ACC_PROTECTED) != 0) {
            return "protected";
        }
        if ((access & Opcodes.ACC_PRIVATE) != 0) {
            return "private";
        }
        return "package-private";
    }
}
