package com.comparev.report;

import com.comparev.model.CompatibilityIssue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class HtmlReportWriter {
    public void write(Path outputFile, List<CompatibilityIssue> issues) throws IOException {
        StringBuilder html = new StringBuilder();
        html.append("<!doctype html><html lang=\"zh-CN\"><head><meta charset=\"utf-8\">");
        html.append("<title>CompareV 兼容性报告</title>");
        html.append("<style>body{font-family:Arial,'Microsoft YaHei',sans-serif;margin:24px;background:#f7f8fa;color:#1f2937}");
        html.append("table{border-collapse:collapse;width:100%;background:#fff}th,td{border:1px solid #d1d5db;padding:8px;text-align:left;vertical-align:top}");
        html.append("th{background:#111827;color:#fff}.ERROR{color:#b91c1c;font-weight:bold}.WARNING{color:#b45309;font-weight:bold}.INFO{color:#2563eb}</style>");
        html.append("</head><body>");
        html.append("<h1>CompareV 兼容性报告</h1>");
        html.append("<p>生成时间：").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("</p>");
        html.append("<p>问题数量：").append(issues.size()).append("</p>");
        html.append("<table><thead><tr><th>等级</th><th>类型</th><th>类名</th><th>现场方法</th><th>本地方法</th><th>相似度</th><th>实现风险</th><th>说明</th></tr></thead><tbody>");
        for (CompatibilityIssue issue : issues) {
            html.append("<tr>")
                    .append(cell(issue.severity().displayName(), issue.severity().name()))
                    .append(cell(issue.issueType().displayName(), ""))
                    .append(cell(issue.className(), ""))
                    .append(cell(issue.classMethod(), ""))
                    .append(cell(issue.sourceMethod(), ""))
                    .append(cell(issue.similarityScore() == null ? "" : issue.similarityScore() + "%", ""))
                    .append(cell(issue.implementationRisk(), ""))
                    .append(cell(issue.message(), ""))
                    .append("</tr>");
        }
        html.append("</tbody></table></body></html>");
        Files.write(outputFile, html.toString().getBytes(StandardCharsets.UTF_8));
    }

    private String cell(String value, String cssClass) {
        String classAttribute = cssClass.trim().isEmpty() ? "" : " class=\"" + cssClass + "\"";
        return "<td" + classAttribute + ">" + escape(value) + "</td>";
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
