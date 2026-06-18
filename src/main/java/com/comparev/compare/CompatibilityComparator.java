package com.comparev.compare;

import com.comparev.model.ClassInfo;
import com.comparev.model.CompatibilityIssue;
import com.comparev.model.IssueType;
import com.comparev.model.MethodInfo;
import com.comparev.model.Severity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CompatibilityComparator {
    public List<CompatibilityIssue> compare(Map<String, ClassInfo> fieldClasses, Map<String, ClassInfo> sourceClasses) {
        List<CompatibilityIssue> issues = new ArrayList<>();

        fieldClasses.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> compareClass(entry.getValue(), sourceClasses.get(entry.getKey()), issues));

        return issues.stream()
                .sorted(Comparator.comparing(CompatibilityIssue::severity)
                        .thenComparing(CompatibilityIssue::className)
                        .thenComparing(issue -> issue.issueType().name())
                        .thenComparing(CompatibilityIssue::classMethod))
                .collect(Collectors.toList());
    }

    private void compareClass(ClassInfo fieldClass, ClassInfo sourceClass, List<CompatibilityIssue> issues) {
        if (sourceClass == null) {
            issues.add(new CompatibilityIssue(
                    fieldClass.className(),
                    IssueType.CLASS_MISSING,
                    "",
                    "",
                    "现场 class 存在，但本地源码中未找到该类",
                    Severity.ERROR));
            return;
        }

        Map<String, MethodInfo> sourceMethods = sourceClass.methods().stream()
                .collect(Collectors.toMap(MethodInfo::parameterSignatureKey, Function.identity(), (left, right) -> left));
        Map<String, MethodInfo> fieldMethods = fieldClass.methods().stream()
                .collect(Collectors.toMap(MethodInfo::parameterSignatureKey, Function.identity(), (left, right) -> left));

        for (MethodInfo fieldMethod : fieldClass.methods()) {
            MethodInfo sourceMethod = sourceMethods.get(fieldMethod.parameterSignatureKey());
            if (sourceMethod == null) {
                issues.add(new CompatibilityIssue(
                        fieldClass.className(),
                        IssueType.METHOD_MISSING,
                        fieldMethod.displaySignature(),
                        "",
                        "现场 class 方法存在，但本地源码中未找到同名同参方法",
                        Severity.ERROR));
                continue;
            }
            if (!fieldMethod.sameReturnType(sourceMethod)) {
                issues.add(new CompatibilityIssue(
                        fieldClass.className(),
                        IssueType.RETURN_TYPE_MISMATCH,
                        fieldMethod.displaySignature(),
                        sourceMethod.displaySignature(),
                        "方法参数一致，但返回类型不同",
                        Severity.ERROR));
            }
            if (!fieldMethod.accessModifier().equals(sourceMethod.accessModifier())) {
                issues.add(new CompatibilityIssue(
                        fieldClass.className(),
                        IssueType.ACCESS_MISMATCH,
                        fieldMethod.displaySignature(),
                        sourceMethod.displaySignature(),
                        "方法访问修饰符不同：现场 " + fieldMethod.accessModifier() + "，本地 " + sourceMethod.accessModifier(),
                        Severity.WARNING));
            }
        }

        for (MethodInfo sourceMethod : sourceClass.methods()) {
            if (!fieldMethods.containsKey(sourceMethod.parameterSignatureKey())) {
                issues.add(new CompatibilityIssue(
                        fieldClass.className(),
                        IssueType.LOCAL_EXTRA_METHOD,
                        "",
                        sourceMethod.displaySignature(),
                        "本地源码存在额外方法，现场 class 中未找到；不影响以现场为准的兼容性",
                        Severity.INFO));
            }
        }
    }
}
