package com.comparev.compare;

import com.comparev.decompile.ClassDecompiler;
import com.comparev.decompile.DecompiledSource;
import com.comparev.model.ClassInfo;
import com.comparev.model.CompatibilityIssue;
import com.comparev.model.IssueType;
import com.comparev.model.MethodInfo;
import com.comparev.model.Severity;
import com.comparev.similarity.MethodBodyExtractor;
import com.comparev.similarity.MethodBodyInfo;
import com.comparev.similarity.MethodSimilarityAnalyzer;
import com.comparev.similarity.MethodSimilarityResult;
import com.comparev.similarity.JavaUnicodeEscapeDecoder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ImplementationComparator {
    private final ClassDecompiler decompiler;
    private final MethodBodyExtractor extractor;
    private final MethodSimilarityAnalyzer analyzer;

    public ImplementationComparator(ClassDecompiler decompiler) {
        this.decompiler = decompiler;
        this.extractor = new MethodBodyExtractor();
        this.analyzer = new MethodSimilarityAnalyzer();
    }

    public List<CompatibilityIssue> compare(Map<String, ClassInfo> fieldClasses, Map<String, ClassInfo> sourceClasses) throws IOException {
        List<CompatibilityIssue> issues = new ArrayList<>();
        for (Map.Entry<String, ClassInfo> entry : fieldClasses.entrySet()) {
            ClassInfo sourceClass = sourceClasses.get(entry.getKey());
            if (sourceClass == null) {
                continue;
            }
            try {
                compareClass(entry.getValue(), sourceClass, issues);
            } catch (Exception exception) {
                issues.add(new CompatibilityIssue(
                        entry.getKey(),
                        IssueType.IMPLEMENTATION_COMPARE,
                        "",
                        "",
                        "方法实现分析失败：" + exception.getMessage(),
                        Severity.WARNING));
            }
        }
        return issues;
    }

    private void compareClass(ClassInfo fieldClass, ClassInfo sourceClass, List<CompatibilityIssue> issues) throws IOException {
        DecompiledSource decompiledSource = decompiler.decompile(fieldClass);
        Map<String, MethodBodyInfo> fieldMethods = extractor.extract(decompiledSource.sourceText());
        Map<String, MethodBodyInfo> sourceMethods = extractor.extract(sourceClass.sourcePath());

        for (MethodInfo fieldMethod : fieldClass.methods()) {
            String methodKey = MethodBodyExtractor.methodKey(fieldClass.className(), fieldMethod);
            MethodBodyInfo fieldBody = fieldMethods.get(methodKey);
            MethodBodyInfo sourceBody = sourceMethods.get(methodKey);
            if (fieldBody == null || sourceBody == null) {
                continue;
            }
            String fieldBodyText = JavaUnicodeEscapeDecoder.decode(fieldBody.body());
            String sourceBodyText = JavaUnicodeEscapeDecoder.decode(sourceBody.body());
            MethodSimilarityResult result = analyzer.analyze(fieldBodyText, sourceBodyText);
            issues.add(new CompatibilityIssue(
                    fieldClass.className(),
                    IssueType.IMPLEMENTATION_COMPARE,
                    fieldMethod.displaySignature(),
                    sourceBody.methodInfo().displaySignature(),
                    "方法实现相似度：" + result.summary(),
                    result.severity(),
                    result.score(),
                    result.risk(),
                    fieldBodyText,
                    sourceBodyText));
        }
    }
}
