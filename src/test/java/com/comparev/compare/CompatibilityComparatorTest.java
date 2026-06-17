package com.comparev.compare;

import com.comparev.model.ClassInfo;
import com.comparev.model.IssueType;
import com.comparev.model.MethodInfo;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompatibilityComparatorTest {
    @Test
    void reportsMissingSourceMethod() {
        ClassInfo fieldClass = new ClassInfo("demo.UserService", "demo", Paths.get("UserService.class"), Collections.singletonList(
                new MethodInfo("findName", Collections.singletonList("java.lang.String"), "java.lang.String", "public", false)
        ));
        ClassInfo sourceClass = new ClassInfo("demo.UserService", "demo", Paths.get("UserService.java"), Collections.emptyList());

        List<com.comparev.model.CompatibilityIssue> issues = new CompatibilityComparator().compare(singleton(fieldClass), singleton(sourceClass));

        assertEquals(1, issues.size());
        assertEquals(IssueType.METHOD_MISSING, issues.get(0).issueType());
    }

    @Test
    void matchesFullyQualifiedClassTypesWithSimpleSourceTypes() {
        ClassInfo fieldClass = new ClassInfo("demo.UserService", "demo", Paths.get("UserService.class"), Collections.singletonList(
                new MethodInfo("findName", Collections.singletonList("java.lang.String"), "java.lang.String", "public", false)
        ));
        ClassInfo sourceClass = new ClassInfo("demo.UserService", "demo", Paths.get("UserService.java"), Collections.singletonList(
                new MethodInfo("findName", Collections.singletonList("String"), "String", "public", false)
        ));

        List<com.comparev.model.CompatibilityIssue> issues = new CompatibilityComparator().compare(singleton(fieldClass), singleton(sourceClass));

        assertTrue(issues.isEmpty());
    }

    @Test
    void reportsReturnTypeMismatch() {
        ClassInfo fieldClass = new ClassInfo("demo.UserService", "demo", Paths.get("UserService.class"), Collections.singletonList(
                new MethodInfo("count", Collections.emptyList(), "int", "public", false)
        ));
        ClassInfo sourceClass = new ClassInfo("demo.UserService", "demo", Paths.get("UserService.java"), Collections.singletonList(
                new MethodInfo("count", Collections.emptyList(), "long", "public", false)
        ));

        List<com.comparev.model.CompatibilityIssue> issues = new CompatibilityComparator().compare(singleton(fieldClass), singleton(sourceClass));

        assertEquals(IssueType.RETURN_TYPE_MISMATCH, issues.get(0).issueType());
    }

    private Map<String, ClassInfo> singleton(ClassInfo classInfo) {
        Map<String, ClassInfo> classes = new HashMap<>();
        classes.put(classInfo.className(), classInfo);
        return classes;
    }
}
