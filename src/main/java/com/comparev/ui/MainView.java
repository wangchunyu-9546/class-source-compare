package com.comparev.ui;

import com.comparev.compare.CompatibilityComparator;
import com.comparev.compare.ImplementationComparator;
import com.comparev.decompile.CfrClassDecompiler;
import com.comparev.model.CompatibilityIssue;
import com.comparev.model.IssueType;
import com.comparev.report.ExcelReportWriter;
import com.comparev.report.HtmlReportWriter;
import com.comparev.scanner.ClassFileScanner;
import com.comparev.scanner.JavaSourceScanner;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

public class MainView extends BorderPane {
    private static final String PREF_CLASS_INPUTS = "classInputs";
    private static final String PREF_SOURCE_INPUTS = "sourceInputs";
    private static final String PREF_INCLUDE_ANONYMOUS = "includeAnonymousInnerClasses";
    private static final String PREF_ANALYZE_IMPLEMENTATION = "analyzeImplementation";
    private static final String PREF_HIDE_HIGH_CONSISTENCY = "hideHighConsistency";

    private final Stage stage;
    private final Preferences preferences = Preferences.userNodeForPackage(MainView.class);
    private final TextField classDirectoryField = new TextField();
    private final TextField sourceDirectoryField = new TextField();
    private final List<Path> classInputPaths = new ArrayList<>();
    private final List<Path> sourceInputPaths = new ArrayList<>();
    private final CheckBox includeAnonymousInnerClasses = new CheckBox("包含匿名内部类($1/$2)");
    private final CheckBox analyzeImplementation = new CheckBox("分析方法实现");
    private final CheckBox hideHighConsistency = new CheckBox("隐藏高度一致");
    private final ComboBox<String> issueFilter = new ComboBox<>();
    private final ComboBox<String> classFilter = new ComboBox<>();
    private final Label summaryLabel = new Label("尚未扫描");
    private final ObservableList<CompatibilityIssue> issues = FXCollections.observableArrayList();
    private final FilteredList<CompatibilityIssue> filteredIssues = new FilteredList<>(issues);
    private final TableView<CompatibilityIssue> table = new TableView<>(filteredIssues);
    private final Label statusLabel = new Label("请选择目录后开始扫描");
    private final ProgressIndicator progressIndicator = new ProgressIndicator();
    private int lastFieldClassCount;
    private int lastSourceClassCount;

    public MainView(Stage stage) {
        this.stage = stage;
        setPadding(new Insets(14));
        setTop(createControls());
        setCenter(createTable());
        setBottom(createStatusBar());
        configureFilter();
        loadPreferences();
    }

    private VBox createControls() {
        classDirectoryField.setEditable(false);
        sourceDirectoryField.setEditable(false);
        classDirectoryField.setPromptText("可点击选择目录，也可拖入 .class/.jar 文件或目录");
        sourceDirectoryField.setPromptText("可点击选择目录，也可拖入 .java 文件或源码目录");

        Button chooseClassButton = new Button("选择现场 class 目录");
        chooseClassButton.setOnAction(event -> chooseDirectory(classInputPaths, classDirectoryField, "选择现场 class 目录"));

        Button chooseSourceButton = new Button("选择本地源码目录");
        chooseSourceButton.setOnAction(event -> chooseDirectory(sourceInputPaths, sourceDirectoryField, "选择本地源码目录"));

        Button scanButton = new Button("开始扫描");
        scanButton.setOnAction(event -> scan());

        Button exportButton = new Button("导出 HTML");
        exportButton.setOnAction(event -> exportReport());

        Button exportExcelButton = new Button("导出 Excel");
        exportExcelButton.setOnAction(event -> exportExcelReport());

        HBox classRow = new HBox(8, chooseClassButton, classDirectoryField);
        HBox sourceRow = new HBox(8, chooseSourceButton, sourceDirectoryField);
        configureDropTarget(classRow, classInputPaths, classDirectoryField, ".class", ".jar");
        configureDropTarget(classDirectoryField, classInputPaths, classDirectoryField, ".class", ".jar");
        configureDropTarget(sourceRow, sourceInputPaths, sourceDirectoryField, ".java");
        configureDropTarget(sourceDirectoryField, sourceInputPaths, sourceDirectoryField, ".java");
        classFilter.setPrefWidth(260);
        HBox actionRow = new HBox(8, scanButton, exportButton, exportExcelButton, includeAnonymousInnerClasses, analyzeImplementation, hideHighConsistency, new Label("问题类型"), issueFilter, new Label("类"), classFilter);
        HBox summaryRow = new HBox(8, summaryLabel);
        HBox.setHgrow(classDirectoryField, Priority.ALWAYS);
        HBox.setHgrow(sourceDirectoryField, Priority.ALWAYS);

        VBox controls = new VBox(10, classRow, sourceRow, actionRow, summaryRow);
        controls.setPadding(new Insets(0, 0, 12, 0));
        return controls;
    }

    private TableView<CompatibilityIssue> createTable() {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getColumns().add(column("等级", issue -> issue.severity().displayName(), 80));
        table.getColumns().add(column("类型", issue -> issue.issueType().displayName(), 150));
        table.getColumns().add(column("类名", CompatibilityIssue::className, 260));
        table.getColumns().add(column("现场方法", CompatibilityIssue::classMethod, 280));
        table.getColumns().add(column("本地方法", CompatibilityIssue::sourceMethod, 280));
        table.getColumns().add(column("相似度", issue -> issue.similarityScore() == null ? "" : issue.similarityScore() + "%", 80));
        table.getColumns().add(column("实现风险", CompatibilityIssue::implementationRisk, 150));
        table.getColumns().add(column("说明", CompatibilityIssue::message, 320));
        table.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && table.getSelectionModel().getSelectedItem() != null) {
                showImplementationDetail(table.getSelectionModel().getSelectedItem());
            }
        });
        table.setRowFactory(tableView -> createIssueRow());
        return table;
    }

    private TableRow<CompatibilityIssue> createIssueRow() {
        TableRow<CompatibilityIssue> row = new TableRow<>();
        ContextMenu contextMenu = new ContextMenu();

        MenuItem copyClassName = new MenuItem("复制类名");
        copyClassName.setOnAction(event -> copyText(row.getItem().className()));
        MenuItem copyFieldMethod = new MenuItem("复制现场方法");
        copyFieldMethod.setOnAction(event -> copyText(row.getItem().classMethod()));
        MenuItem copySourceMethod = new MenuItem("复制本地方法");
        copySourceMethod.setOnAction(event -> copyText(row.getItem().sourceMethod()));
        MenuItem copyMessage = new MenuItem("复制说明");
        copyMessage.setOnAction(event -> copyText(row.getItem().message()));
        MenuItem openDetail = new MenuItem("打开方法详情");
        openDetail.setOnAction(event -> showImplementationDetail(row.getItem()));

        contextMenu.getItems().addAll(copyClassName, copyFieldMethod, copySourceMethod, copyMessage, openDetail);
        row.contextMenuProperty().bind(javafx.beans.binding.Bindings.when(row.emptyProperty()).then((ContextMenu) null).otherwise(contextMenu));
        return row;
    }

    private void copyText(String text) {
        ClipboardContent content = new ClipboardContent();
        content.putString(text == null ? "" : text);
        Clipboard.getSystemClipboard().setContent(content);
        statusLabel.setText("已复制到剪贴板");
    }

    private TableColumn<CompatibilityIssue, String> column(String title, ValueProvider provider, int width) {
        TableColumn<CompatibilityIssue, String> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new ReadOnlyStringWrapper(provider.value(data.getValue())));
        column.setCellFactory(tableColumn -> new TableCell<CompatibilityIssue, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.trim().isEmpty()) {
                    setText(null);
                    setTooltip(null);
                    return;
                }
                setText(item);
                setTooltip(new Tooltip(item));
            }
        });
        column.setPrefWidth(width);
        return column;
    }

    private HBox createStatusBar() {
        progressIndicator.setVisible(false);
        progressIndicator.setPrefSize(22, 22);
        HBox statusBar = new HBox(10, progressIndicator, statusLabel);
        statusBar.setPadding(new Insets(12, 0, 0, 0));
        return statusBar;
    }

    private void configureFilter() {
        issueFilter.getItems().add("全部");
        for (IssueType issueType : IssueType.values()) {
            issueFilter.getItems().add(issueType.displayName());
        }
        issueFilter.getSelectionModel().selectFirst();
        issueFilter.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        classFilter.getItems().add("全部类");
        classFilter.getSelectionModel().selectFirst();
        classFilter.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        hideHighConsistency.selectedProperty().addListener((observable, oldValue, newValue) -> {
            savePreferences();
            applyFilters();
        });
        includeAnonymousInnerClasses.selectedProperty().addListener((observable, oldValue, newValue) -> savePreferences());
        analyzeImplementation.selectedProperty().addListener((observable, oldValue, newValue) -> savePreferences());
    }

    private void applyFilters() {
        String selectedIssueType = issueFilter.getValue();
        String selectedClass = classFilter.getValue();
        filteredIssues.setPredicate(issue -> {
            boolean issueTypeMatches = selectedIssueType == null
                    || "全部".equals(selectedIssueType)
                    || issue.issueType() == IssueType.fromDisplayName(selectedIssueType);
            if (!issueTypeMatches) {
                return false;
            }
            boolean classMatches = selectedClass == null
                    || "全部类".equals(selectedClass)
                    || issue.className().equals(selectedClass);
            if (!classMatches) {
                return false;
            }
            return !hideHighConsistency.isSelected() || !isHighConsistencyImplementation(issue);
        });
    }

    private boolean isHighConsistencyImplementation(CompatibilityIssue issue) {
        return issue.issueType() == IssueType.IMPLEMENTATION_COMPARE
                && issue.similarityScore() != null
                && issue.similarityScore() >= 90;
    }

    private void refreshClassFilter() {
        String previousSelection = classFilter.getValue();
        TreeSet<String> classNames = new TreeSet<>();
        for (CompatibilityIssue issue : issues) {
            if (issue.className() != null && !issue.className().trim().isEmpty()) {
                classNames.add(issue.className());
            }
        }

        classFilter.getItems().setAll("全部类");
        classFilter.getItems().addAll(classNames);
        if (previousSelection != null && classFilter.getItems().contains(previousSelection)) {
            classFilter.getSelectionModel().select(previousSelection);
        } else {
            classFilter.getSelectionModel().selectFirst();
        }
    }

    private void updateSummary() {
        int incompatibleCount = 0;
        int warningCount = 0;
        int infoCount = 0;
        int implementationCount = 0;
        int similarityTotal = 0;
        int similarityCount = 0;

        for (CompatibilityIssue issue : issues) {
            switch (issue.severity()) {
                case ERROR:
                    incompatibleCount++;
                    break;
                case WARNING:
                    warningCount++;
                    break;
                case INFO:
                    infoCount++;
                    break;
                default:
                    break;
            }
            if (issue.issueType() == IssueType.IMPLEMENTATION_COMPARE) {
                implementationCount++;
            }
            if (issue.similarityScore() != null) {
                similarityTotal += issue.similarityScore();
                similarityCount++;
            }
        }

        String averageSimilarity = similarityCount == 0 ? "-" : Math.round((float) similarityTotal / similarityCount) + "%";
        summaryLabel.setText("现场类 " + lastFieldClassCount
                + " | 源码类 " + lastSourceClassCount
                + " | 问题 " + issues.size()
                + " | 不兼容 " + incompatibleCount
                + " | 需确认 " + warningCount
                + " | 提示 " + infoCount
                + " | 方法实现比对 " + implementationCount
                + " | 平均相似度 " + averageSimilarity);
    }

    private void chooseDirectory(List<Path> inputPaths, TextField targetField, String title) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(title);
        File selected = chooser.showDialog(stage);
        if (selected != null) {
            inputPaths.clear();
            inputPaths.add(selected.toPath());
            updateInputField(targetField, inputPaths);
            savePreferences();
        }
    }

    private void configureDropTarget(javafx.scene.Node target, List<Path> inputPaths, TextField targetField, String... fileSuffixes) {
        target.setOnDragOver(event -> {
            Dragboard dragboard = event.getDragboard();
            if (dragboard.hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });
        target.setOnDragDropped(event -> {
            Dragboard dragboard = event.getDragboard();
            boolean success = false;
            if (dragboard.hasFiles()) {
                List<Path> acceptedPaths = dragboard.getFiles().stream()
                        .map(File::toPath)
                        .filter(path -> acceptsPath(path, fileSuffixes))
                        .collect(Collectors.toList());
                if (!acceptedPaths.isEmpty()) {
                    inputPaths.clear();
                    inputPaths.addAll(acceptedPaths);
                    updateInputField(targetField, inputPaths);
                    savePreferences();
                    statusLabel.setText("已拖入 " + acceptedPaths.size() + " 个输入项");
                    success = true;
                } else {
                    showError("拖入内容中没有可扫描的文件或目录");
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    private boolean acceptsPath(Path path, String... fileSuffixes) {
        File file = path.toFile();
        if (file.isDirectory()) {
            return true;
        }
        String fileName = file.getName().toLowerCase();
        for (String fileSuffix : fileSuffixes) {
            if (fileName.endsWith(fileSuffix)) {
                return true;
            }
        }
        return false;
    }

    private void updateInputField(TextField targetField, List<Path> inputPaths) {
        if (inputPaths.size() == 1) {
            targetField.setText(inputPaths.get(0).toString());
            return;
        }
        targetField.setText("已选择 " + inputPaths.size() + " 个输入项");
    }

    private void loadPreferences() {
        loadInputPaths(PREF_CLASS_INPUTS, classInputPaths, classDirectoryField);
        loadInputPaths(PREF_SOURCE_INPUTS, sourceInputPaths, sourceDirectoryField);
        includeAnonymousInnerClasses.setSelected(preferences.getBoolean(PREF_INCLUDE_ANONYMOUS, false));
        analyzeImplementation.setSelected(preferences.getBoolean(PREF_ANALYZE_IMPLEMENTATION, false));
        hideHighConsistency.setSelected(preferences.getBoolean(PREF_HIDE_HIGH_CONSISTENCY, false));
        applyFilters();
    }

    private void loadInputPaths(String key, List<Path> inputPaths, TextField targetField) {
        String storedValue = preferences.get(key, "");
        if (storedValue.trim().isEmpty()) {
            return;
        }
        List<Path> paths = new ArrayList<>();
        for (String line : storedValue.split("\\n")) {
            if (!line.trim().isEmpty()) {
                Path path = Paths.get(line);
                if (Files.exists(path)) {
                    paths.add(path);
                }
            }
        }
        if (!paths.isEmpty()) {
            inputPaths.clear();
            inputPaths.addAll(paths);
            updateInputField(targetField, inputPaths);
        }
    }

    private void savePreferences() {
        preferences.put(PREF_CLASS_INPUTS, serializePaths(classInputPaths));
        preferences.put(PREF_SOURCE_INPUTS, serializePaths(sourceInputPaths));
        preferences.putBoolean(PREF_INCLUDE_ANONYMOUS, includeAnonymousInnerClasses.isSelected());
        preferences.putBoolean(PREF_ANALYZE_IMPLEMENTATION, analyzeImplementation.isSelected());
        preferences.putBoolean(PREF_HIDE_HIGH_CONSISTENCY, hideHighConsistency.isSelected());
    }

    private String serializePaths(List<Path> inputPaths) {
        return inputPaths.stream()
                .map(Path::toString)
                .collect(Collectors.joining("\n"));
    }

    private void scan() {
        if (classInputPaths.isEmpty() || sourceInputPaths.isEmpty()) {
            showError("请先选择或拖入现场 class 和本地源码");
            return;
        }
        savePreferences();

        List<Path> classInputs = Collections.unmodifiableList(new ArrayList<>(classInputPaths));
        List<Path> sourceInputs = Collections.unmodifiableList(new ArrayList<>(sourceInputPaths));
        boolean includeAnonymous = includeAnonymousInnerClasses.isSelected();
        boolean analyzeBodies = analyzeImplementation.isSelected();
        Task<List<CompatibilityIssue>> task = new Task<List<CompatibilityIssue>>() {
            private int fieldClassCount;
            private int sourceClassCount;

            @Override
            protected List<CompatibilityIssue> call() throws Exception {
                updateMessage("正在扫描现场 class 文件...");
                Map<String, com.comparev.model.ClassInfo> fieldClasses = new ClassFileScanner().scan(classInputs, includeAnonymous);
                fieldClassCount = fieldClasses.size();
                updateMessage("正在扫描本地 Java 源码...");
                Map<String, com.comparev.model.ClassInfo> sourceClasses = new JavaSourceScanner().scan(sourceInputs);
                sourceClassCount = sourceClasses.size();
                updateMessage("正在比对兼容性...");
                List<CompatibilityIssue> results = new ArrayList<>(new CompatibilityComparator().compare(fieldClasses, sourceClasses));
                if (analyzeBodies) {
                    updateMessage("正在反编译并分析方法实现...");
                    results.addAll(new ImplementationComparator(new CfrClassDecompiler()).compare(fieldClasses, sourceClasses));
                }
                return results;
            }

            @Override
            protected void succeeded() {
                super.succeeded();
                lastFieldClassCount = fieldClassCount;
                lastSourceClassCount = sourceClassCount;
            }
        };

        statusLabel.textProperty().bind(task.messageProperty());
        progressIndicator.setVisible(true);
        task.setOnSucceeded(event -> {
            statusLabel.textProperty().unbind();
            issues.setAll(task.getValue());
            refreshClassFilter();
            updateSummary();
            applyFilters();
            progressIndicator.setVisible(false);
            statusLabel.setText("扫描完成，发现问题 " + issues.size() + " 个");
        });
        task.setOnFailed(event -> {
            statusLabel.textProperty().unbind();
            progressIndicator.setVisible(false);
            statusLabel.setText("扫描失败");
            showError(task.getException().getMessage());
        });
        Thread worker = new Thread(task, "comparev-scan");
        worker.setDaemon(true);
        worker.start();
    }

    private void exportReport() {
        if (issues.isEmpty()) {
            showError("当前没有可导出的扫描结果");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("导出 HTML 报告");
        chooser.setInitialFileName("comparev-report.html");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("HTML", "*.html"));
        File selected = chooser.showSaveDialog(stage);
        if (selected == null) {
            return;
        }
        try {
            new HtmlReportWriter().write(selected.toPath(), new ArrayList<>(issues));
            statusLabel.setText("报告已导出：" + selected.getAbsolutePath());
        } catch (Exception exception) {
            showError(exception.getMessage());
        }
    }

    private void exportExcelReport() {
        if (filteredIssues.isEmpty()) {
            showError("当前没有可导出的筛选结果");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("导出 Excel 报告");
        chooser.setInitialFileName("comparev-report.xlsx");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel", "*.xlsx"));
        File selected = chooser.showSaveDialog(stage);
        if (selected == null) {
            return;
        }
        try {
            new ExcelReportWriter().write(selected.toPath(), new ArrayList<>(filteredIssues));
            statusLabel.setText("Excel 已导出：" + selected.getAbsolutePath());
        } catch (Exception exception) {
            showError(exception.getMessage());
        }
    }

    private void showImplementationDetail(CompatibilityIssue issue) {
        if (issue.fieldImplementation().trim().isEmpty() && issue.sourceImplementation().trim().isEmpty()) {
            return;
        }
        Stage detailStage = new Stage();
        detailStage.setTitle("方法实现详情 - " + issue.className());

        DiffResult diffResult = buildDiff(issue.fieldImplementation(), issue.sourceImplementation());
        ListView<DiffLine> fieldList = createDiffListView(diffResult.fieldLines);
        ListView<DiffLine> sourceList = createDiffListView(diffResult.sourceLines);
        CheckBox onlyDifferent = new CheckBox("只看差异");
        onlyDifferent.selectedProperty().addListener((observable, oldValue, newValue) -> {
            fieldList.setItems(FXCollections.observableArrayList(visibleDiffLines(diffResult.fieldLines, diffResult.sourceLines, newValue)));
            sourceList.setItems(FXCollections.observableArrayList(visibleDiffLines(diffResult.sourceLines, diffResult.fieldLines, newValue)));
        });

        Label tipLabel = new Label(summaryText(diffResult) + "。白色表示已对齐一致，浅红色表示差异，浅灰色表示注释/空行已忽略比较");
        HBox topBar = new HBox(12, onlyDifferent, tipLabel);
        VBox fieldPane = new VBox(6, new Label("现场反编译方法体"), fieldList);
        VBox sourcePane = new VBox(6, new Label("本地源码方法体"), sourceList);
        VBox.setVgrow(fieldList, Priority.ALWAYS);
        VBox.setVgrow(sourceList, Priority.ALWAYS);

        SplitPane splitPane = new SplitPane(fieldPane, sourcePane);
        splitPane.setDividerPositions(0.5);
        BorderPane root = new BorderPane(splitPane);
        root.setTop(topBar);
        root.setPadding(new Insets(10));
        detailStage.setScene(new javafx.scene.Scene(root, 980, 620));
        detailStage.show();
    }

    private List<DiffLine> visibleDiffLines(List<DiffLine> lines, List<DiffLine> otherLines, boolean onlyDifferent) {
        if (!onlyDifferent) {
            return lines;
        }
        List<DiffLine> visibleLines = new ArrayList<>();
        for (int index = 0; index < lines.size(); index++) {
            DiffStatus status = lines.get(index).status;
            DiffStatus otherStatus = index < otherLines.size() ? otherLines.get(index).status : DiffStatus.MISSING;
            if (isVisibleDifference(status) || isVisibleDifference(otherStatus)) {
                visibleLines.add(lines.get(index));
            }
        }
        return visibleLines;
    }

    private boolean isVisibleDifference(DiffStatus status) {
        return status == DiffStatus.DIFFERENT || status == DiffStatus.MISSING;
    }

    private String summaryText(DiffResult diffResult) {
        return "一致 " + diffResult.equalCount
                + " 行，差异 " + diffResult.differentCount
                + " 行，忽略 " + diffResult.ignoredCount
                + " 行，匹配率 " + diffResult.matchRate() + "%";
    }

    private ListView<DiffLine> createDiffListView(List<DiffLine> lines) {
        ListView<DiffLine> listView = new ListView<>(FXCollections.observableArrayList(lines));
        listView.setCellFactory(view -> new ListCell<DiffLine>() {
            @Override
            protected void updateItem(DiffLine item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(item.displayText());
                setStyle(styleFor(item.status));
            }
        });
        return listView;
    }

    private String styleFor(DiffStatus status) {
        String font = "-fx-font-family: 'Consolas', 'Courier New', monospace;";
        if (status == DiffStatus.IGNORED) {
            return "-fx-background-color: #f3f4f6; -fx-text-fill: #6b7280;" + font;
        }
        if (status == DiffStatus.DIFFERENT || status == DiffStatus.MISSING) {
            return "-fx-background-color: #ffe5e5;" + font;
        }
        return font;
    }

    private DiffResult buildDiff(String fieldText, String sourceText) {
        List<CodeLine> fieldLines = toCodeLines(fieldText);
        List<CodeLine> sourceLines = toCodeLines(sourceText);
        List<CodeLine> fieldComparable = comparableLines(fieldLines);
        List<CodeLine> sourceComparable = comparableLines(sourceLines);
        List<int[]> matches = lcsMatches(fieldComparable, sourceComparable);

        List<DiffLine> fieldDisplay = new ArrayList<>();
        List<DiffLine> sourceDisplay = new ArrayList<>();
        int fieldOriginalIndex = 0;
        int sourceOriginalIndex = 0;
        for (int[] match : matches) {
            CodeLine fieldMatch = fieldComparable.get(match[0]);
            CodeLine sourceMatch = sourceComparable.get(match[1]);
            appendUnmatchedSegment(fieldLines, sourceLines, fieldOriginalIndex, fieldMatch.originalIndex, sourceOriginalIndex, sourceMatch.originalIndex, fieldDisplay, sourceDisplay);
            appendPair(fieldMatch, sourceMatch, DiffStatus.EQUAL, fieldDisplay, sourceDisplay);
            fieldOriginalIndex = fieldMatch.originalIndex + 1;
            sourceOriginalIndex = sourceMatch.originalIndex + 1;
        }
        appendUnmatchedSegment(fieldLines, sourceLines, fieldOriginalIndex, fieldLines.size(), sourceOriginalIndex, sourceLines.size(), fieldDisplay, sourceDisplay);
        return new DiffResult(fieldDisplay, sourceDisplay);
    }

    private void appendUnmatchedSegment(
            List<CodeLine> fieldLines,
            List<CodeLine> sourceLines,
            int fieldStart,
            int fieldEnd,
            int sourceStart,
            int sourceEnd,
            List<DiffLine> fieldDisplay,
            List<DiffLine> sourceDisplay) {
        List<CodeLine> fieldSegment = fieldLines.subList(fieldStart, fieldEnd);
        List<CodeLine> sourceSegment = sourceLines.subList(sourceStart, sourceEnd);
        int max = Math.max(fieldSegment.size(), sourceSegment.size());
        for (int index = 0; index < max; index++) {
            CodeLine fieldLine = index < fieldSegment.size() ? fieldSegment.get(index) : null;
            CodeLine sourceLine = index < sourceSegment.size() ? sourceSegment.get(index) : null;
            DiffStatus status = segmentStatus(fieldLine, sourceLine);
            appendPair(fieldLine, sourceLine, status, fieldDisplay, sourceDisplay);
        }
    }

    private DiffStatus segmentStatus(CodeLine fieldLine, CodeLine sourceLine) {
        if ((fieldLine == null || fieldLine.ignored) && (sourceLine == null || sourceLine.ignored)) {
            return DiffStatus.IGNORED;
        }
        if (fieldLine == null || sourceLine == null) {
            return DiffStatus.MISSING;
        }
        if (fieldLine.ignored || sourceLine.ignored) {
            return DiffStatus.DIFFERENT;
        }
        return fieldLine.normalized.equals(sourceLine.normalized) ? DiffStatus.EQUAL : DiffStatus.DIFFERENT;
    }

    private void appendPair(CodeLine fieldLine, CodeLine sourceLine, DiffStatus status, List<DiffLine> fieldDisplay, List<DiffLine> sourceDisplay) {
        fieldDisplay.add(toDiffLine(fieldLine, status));
        sourceDisplay.add(toDiffLine(sourceLine, status));
    }

    private DiffLine toDiffLine(CodeLine line, DiffStatus status) {
        if (line == null) {
            return new DiffLine(0, "", DiffStatus.MISSING);
        }
        return new DiffLine(line.lineNumber, line.text, line.ignored ? DiffStatus.IGNORED : status);
    }

    private List<CodeLine> toCodeLines(String text) {
        List<String> lines = splitLines(text);
        List<CodeLine> codeLines = new ArrayList<>();
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            String normalized = normalizeComparableLine(line);
            boolean ignored = normalized.isEmpty();
            codeLines.add(new CodeLine(index, index + 1, line, normalized, ignored));
        }
        return codeLines;
    }

    private List<CodeLine> comparableLines(List<CodeLine> lines) {
        return lines.stream()
                .filter(line -> !line.ignored)
                .collect(Collectors.toList());
    }

    private String normalizeComparableLine(String line) {
        String trimmed = line == null ? "" : line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("//") || trimmed.startsWith("/*") || trimmed.startsWith("*") || trimmed.endsWith("*/")) {
            return "";
        }
        int commentIndex = trimmed.indexOf("//");
        if (commentIndex >= 0) {
            trimmed = trimmed.substring(0, commentIndex).trim();
        }
        return trimmed.replaceAll("<[^<>]*>", "")
                .replaceAll("\\((?:[A-Z][a-zA-Z0-9_$.]*(?:\\[\\])?|byte|short|int|long|float|double|boolean|char)\\)\\s*", "")
                .replaceAll("\\s+", "")
                .replace("this.", "")
                .replace("(Object)", "");
    }

    private List<int[]> lcsMatches(List<CodeLine> left, List<CodeLine> right) {
        int[][] lengths = new int[left.size() + 1][right.size() + 1];
        for (int leftIndex = left.size() - 1; leftIndex >= 0; leftIndex--) {
            for (int rightIndex = right.size() - 1; rightIndex >= 0; rightIndex--) {
                if (left.get(leftIndex).normalized.equals(right.get(rightIndex).normalized)) {
                    lengths[leftIndex][rightIndex] = lengths[leftIndex + 1][rightIndex + 1] + 1;
                } else {
                    lengths[leftIndex][rightIndex] = Math.max(lengths[leftIndex + 1][rightIndex], lengths[leftIndex][rightIndex + 1]);
                }
            }
        }

        List<int[]> matches = new ArrayList<>();
        int leftIndex = 0;
        int rightIndex = 0;
        while (leftIndex < left.size() && rightIndex < right.size()) {
            if (left.get(leftIndex).normalized.equals(right.get(rightIndex).normalized)) {
                matches.add(new int[]{leftIndex, rightIndex});
                leftIndex++;
                rightIndex++;
            } else if (lengths[leftIndex + 1][rightIndex] >= lengths[leftIndex][rightIndex + 1]) {
                leftIndex++;
            } else {
                rightIndex++;
            }
        }
        return matches;
    }

    private List<String> splitLines(String text) {
        String normalized = text == null ? "" : text.replace("\r\n", "\n").replace('\r', '\n');
        String[] parts = normalized.split("\n", -1);
        List<String> lines = new ArrayList<>();
        Collections.addAll(lines, parts);
        return lines;
    }

    private enum DiffStatus {
        EQUAL,
        DIFFERENT,
        IGNORED,
        MISSING
    }

    private static class CodeLine {
        private final int originalIndex;
        private final int lineNumber;
        private final String text;
        private final String normalized;
        private final boolean ignored;

        private CodeLine(int originalIndex, int lineNumber, String text, String normalized, boolean ignored) {
            this.originalIndex = originalIndex;
            this.lineNumber = lineNumber;
            this.text = text;
            this.normalized = normalized;
            this.ignored = ignored;
        }
    }

    private static class DiffLine {
        private final int lineNumber;
        private final String text;
        private final DiffStatus status;

        private DiffLine(int lineNumber, String text, DiffStatus status) {
            this.lineNumber = lineNumber;
            this.text = text;
            this.status = status;
        }

        private String displayText() {
            return lineNumber == 0 ? "      " : String.format("%4d  %s", lineNumber, text);
        }
    }

    private static class DiffResult {
        private final List<DiffLine> fieldLines;
        private final List<DiffLine> sourceLines;
        private final int equalCount;
        private final int differentCount;
        private final int ignoredCount;

        private DiffResult(List<DiffLine> fieldLines, List<DiffLine> sourceLines) {
            this.fieldLines = fieldLines;
            this.sourceLines = sourceLines;
            int equal = 0;
            int different = 0;
            int ignored = 0;
            int max = Math.max(fieldLines.size(), sourceLines.size());
            for (int index = 0; index < max; index++) {
                DiffStatus fieldStatus = index < fieldLines.size() ? fieldLines.get(index).status : DiffStatus.MISSING;
                DiffStatus sourceStatus = index < sourceLines.size() ? sourceLines.get(index).status : DiffStatus.MISSING;
                if (fieldStatus == DiffStatus.IGNORED && sourceStatus == DiffStatus.IGNORED) {
                    ignored++;
                } else if (fieldStatus == DiffStatus.EQUAL && sourceStatus == DiffStatus.EQUAL) {
                    equal++;
                } else {
                    different++;
                }
            }
            this.equalCount = equal;
            this.differentCount = different;
            this.ignoredCount = ignored;
        }

        private int matchRate() {
            int comparableTotal = equalCount + differentCount;
            if (comparableTotal == 0) {
                return 100;
            }
            return Math.round((float) equalCount / comparableTotal * 100);
        }
    }

    private void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("CompareV");
            alert.setHeaderText("操作失败");
            alert.setContentText(message == null ? "未知错误" : message);
            alert.showAndWait();
        });
    }

    @FunctionalInterface
    private interface ValueProvider {
        String value(CompatibilityIssue issue);
    }
}
