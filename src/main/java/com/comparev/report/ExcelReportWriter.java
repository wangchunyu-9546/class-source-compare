package com.comparev.report;

import com.comparev.model.CompatibilityIssue;
import com.comparev.model.Severity;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ExcelReportWriter {
    private static final String[] HEADERS = {
            "等级", "类型", "类名", "现场方法", "本地方法", "相似度", "实现风险", "说明"
    };

    public void write(Path outputFile, List<CompatibilityIssue> issues) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("CompareV结果");
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle errorStyle = createSeverityStyle(workbook, IndexedColors.ROSE);
            CellStyle warningStyle = createSeverityStyle(workbook, IndexedColors.LIGHT_ORANGE);
            CellStyle infoStyle = createSeverityStyle(workbook, IndexedColors.LIGHT_CORNFLOWER_BLUE);

            writeHeader(sheet, headerStyle);
            for (int index = 0; index < issues.size(); index++) {
                writeIssue(sheet.createRow(index + 1), issues.get(index), styleFor(issues.get(index).severity(), errorStyle, warningStyle, infoStyle));
            }
            for (int index = 0; index < HEADERS.length; index++) {
                sheet.autoSizeColumn(index);
                int width = Math.min(sheet.getColumnWidth(index) + 512, 16000);
                sheet.setColumnWidth(index, width);
            }

            try (OutputStream outputStream = Files.newOutputStream(outputFile)) {
                workbook.write(outputStream);
            }
        }
    }

    private void writeHeader(Sheet sheet, CellStyle headerStyle) {
        Row row = sheet.createRow(0);
        for (int index = 0; index < HEADERS.length; index++) {
            Cell cell = row.createCell(index);
            cell.setCellValue(HEADERS[index]);
            cell.setCellStyle(headerStyle);
        }
    }

    private void writeIssue(Row row, CompatibilityIssue issue, CellStyle rowStyle) {
        writeCell(row, 0, issue.severity().displayName(), rowStyle);
        writeCell(row, 1, issue.issueType().displayName(), rowStyle);
        writeCell(row, 2, issue.className(), rowStyle);
        writeCell(row, 3, issue.classMethod(), rowStyle);
        writeCell(row, 4, issue.sourceMethod(), rowStyle);
        writeCell(row, 5, issue.similarityScore() == null ? "" : issue.similarityScore() + "%", rowStyle);
        writeCell(row, 6, issue.implementationRisk(), rowStyle);
        writeCell(row, 7, issue.message(), rowStyle);
    }

    private void writeCell(Row row, int columnIndex, String value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        cell.setCellValue(value == null ? "" : value);
        cell.setCellStyle(style);
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private CellStyle createSeverityStyle(Workbook workbook, IndexedColors color) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(color.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setWrapText(true);
        return style;
    }

    private CellStyle styleFor(Severity severity, CellStyle errorStyle, CellStyle warningStyle, CellStyle infoStyle) {
        if (severity == Severity.ERROR) {
            return errorStyle;
        }
        if (severity == Severity.WARNING) {
            return warningStyle;
        }
        return infoStyle;
    }
}
