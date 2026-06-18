package com.comparev.similarity;

import com.comparev.model.ClassInfo;
import com.comparev.model.MethodInfo;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MethodBodyExtractorTest {
    @Test
    void fallsBackWhenSourceCannotBeParsedAsCompilationUnit() {
        MethodInfo methodInfo = new MethodInfo("save", Collections.emptyList(), "void", "public", false);
        ClassInfo classInfo = new ClassInfo("demo.BadSource", "demo", Paths.get("BadSource.java"), Collections.singletonList(methodInfo));
        String brokenSource = "package demo; public class BadSource { public void save() { String value = \"ok\"; } void";

        Map<String, MethodBodyInfo> methods = new MethodBodyExtractor().extract(brokenSource, classInfo);

        assertFalse(methods.isEmpty());
        assertTrue(methods.values().iterator().next().body().contains("String value"));
    }
}
