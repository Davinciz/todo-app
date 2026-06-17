package com.todoapp.controller;

import com.todoapp.dao.TaskDAO;
import com.todoapp.model.Task;
import com.todoapp.util.FileAttachmentManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MainController {

    // --- Dialog form fields ---
    private TextField titleField;
    private TextArea descriptionArea;
    private DatePicker deadlinePicker;
    private TextField categoryField;
    private ChoiceBox<Task.Priority> priorityChoiceBox;
    private ChoiceBox<Task.Status> statusChoiceBox;
    private Label attachmentLabel;

    // --- Buttons ---
    private Button attachFileButton;
    private Button removeAttachmentButton;
    @FXML private Button openAttachmentButton;
    @FXML private Button deleteButton;
    @FXML private Button refreshButton;
    @FXML private Button newTaskButton;
    @FXML private Button editTaskButton;

    // --- Search & filter ---
    @FXML private TextField searchField;
    @FXML private ChoiceBox<String> filterStatusChoiceBox;

    // --- Detail panel & calendar ---
    @FXML private Label detailSectionLabel;
    @FXML private VBox detailCard;
    @FXML private GridPane detailFactsGrid;
    @FXML private VBox detailDescriptionBox;
    @FXML private Label detailTitleLabel;
    @FXML private Label detailMetaLabel;
    @FXML private Label detailCategoryLabel;
    @FXML private Label detailDeadlineLabel;
    @FXML private Label detailPriorityLabel;
    @FXML private Label detailStatusLabel;
    @FXML private Label detailAttachmentLabel;
    @FXML private TextArea detailDescriptionArea;
    @FXML private Label calendarTitleLabel;
    @FXML private GridPane deadlineCalendarGrid;

    // --- Table ---
    @FXML private TableView<Task> taskTableView;
    @FXML private TableColumn<Task, String> titleColumn;
    @FXML private TableColumn<Task, String> categoryColumn;
    @FXML private TableColumn<Task, String> deadlineColumn;
    @FXML private TableColumn<Task, String> priorityColumn;
    @FXML private TableColumn<Task, String> statusColumn;
    @FXML private TableColumn<Task, String> attachmentColumn;

    @FXML private Label statusBarLabel;

    private final TaskDAO taskDAO = new TaskDAO();
    private final ObservableList<Task> taskList = FXCollections.observableArrayList();

    // Task yang sedang dipilih/diedit. Null = mode tambah baru.
    private Task selectedTask = null;

    // Path file lampiran yang baru dipilih tapi belum disimpan ke DB
    private String pendingAttachmentPath = null;
    private String pendingAttachmentName = null;
    private YearMonth visibleCalendarMonth = YearMonth.now();

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("MMMM yyyy");

    @FXML
    public void initialize() {
        setupFilterChoiceBox();
        setupTableColumns();
        setupTableSelectionListener();
        setupSearchListener();

        loadAllTasks();
        updateDetailPanel(null);
    }

    // =========================================================
    // SETUP
    // =========================================================

    private void setupFilterChoiceBox() {
        filterStatusChoiceBox.setItems(FXCollections.observableArrayList(
                "Semua", "PENDING", "IN_PROGRESS", "DONE"
        ));
        filterStatusChoiceBox.setValue("Semua");
        filterStatusChoiceBox.setOnAction(e -> applyFilter());
    }

    private void setupTableColumns() {
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));

        deadlineColumn.setCellValueFactory(data -> {
            LocalDate deadline = data.getValue().getDeadline();
            String text = deadline != null ? deadline.format(DATE_FORMAT) : "-";
            return new SimpleStringProperty(text);
        });

        priorityColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getPriority().toString()));
        priorityColumn.setCellFactory(column -> createBadgeCell("priority"));

        statusColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getStatus().toString()));
        statusColumn.setCellFactory(column -> createBadgeCell("status"));

        attachmentColumn.setCellValueFactory(data -> {
            Task task = data.getValue();
            String text = task.hasAttachment() ? "📎 " + task.getAttachmentName() : "-";
            return new SimpleStringProperty(text);
        });

        // Highlight baris hanya untuk kondisi khusus: overdue atau selesai.
        taskTableView.setRowFactory(tv -> new TableRow<Task>() {
            @Override
            protected void updateItem(Task task, boolean empty) {
                super.updateItem(task, empty);
                getStyleClass().removeAll("overdue-row", "done-row");
                if (task == null || empty) {
                    return;
                }

                if (task.isOverdue()) {
                    getStyleClass().add("overdue-row");
                } else if (task.getStatus() == Task.Status.DONE) {
                    getStyleClass().add("done-row");
                }
            }
        });

        taskTableView.setItems(taskList);
    }

    private void setupTableSelectionListener() {
        taskTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedTask = newVal;
            updateDetailPanel(newVal);
        });
    }

    private void setupSearchListener() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilter());
    }

    // =========================================================
    // DATA LOADING & FILTERING
    // =========================================================

    private void loadAllTasks() {
        try {
            List<Task> tasks = taskDAO.getAllTasks();
            taskList.setAll(tasks);
            renderDeadlineCalendar();
            setStatus("Berhasil memuat " + tasks.size() + " tugas.");
        } catch (Exception e) {
            showError("Gagal memuat data", e.getMessage());
        }
    }

    private void applyFilter() {
        String keyword = searchField.getText();
        String statusFilter = filterStatusChoiceBox.getValue();

        try {
            List<Task> result;
            if (keyword != null && !keyword.isBlank()) {
                result = taskDAO.searchTasks(keyword.trim());
            } else {
                result = taskDAO.getAllTasks();
            }

            if (statusFilter != null && !statusFilter.equals("Semua")) {
                result.removeIf(t -> !t.getStatus().name().equals(statusFilter));
            }

            taskList.setAll(result);
            renderDeadlineCalendar();
        } catch (Exception e) {
            showError("Gagal memfilter data", e.getMessage());
        }
    }

    // =========================================================
    // FORM HANDLING
    // =========================================================

    @FXML
    private void handleShowNewTaskDialog() {
        openTaskDialog(null);
    }

    @FXML
    private void handleShowEditTaskDialog() {
        Task task = taskTableView.getSelectionModel().getSelectedItem();
        if (task == null) {
            showError("Tidak ada yang dipilih", "Pilih tugas pada tabel yang ingin diedit.");
            return;
        }
        openTaskDialog(task);
    }

    private void openTaskDialog(Task taskToEdit) {
        selectedTask = taskToEdit;
        VBox form = createTaskForm(taskToEdit);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(taskToEdit == null ? "Buat Tugas Baru" : "Edit Tugas");
        dialog.setHeaderText(null);
        dialog.getDialogPane().getStyleClass().add("task-dialog");
        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/css/style.css").toExternalForm()
        );
        dialog.getDialogPane().setContent(form);

        ButtonType saveType = new ButtonType("Simpan", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        Node saveButton = dialog.getDialogPane().lookupButton(saveType);
        saveButton.getStyleClass().add("primary-button");
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (!saveTaskFromDialog()) {
                event.consume();
            } else {
                dialog.close();
            }
        });

        dialog.showAndWait();
    }

    private VBox createTaskForm(Task taskToEdit) {
        titleField = new TextField();
        titleField.setPromptText("Masukkan judul tugas...");

        descriptionArea = new TextArea();
        descriptionArea.setPromptText("Jelaskan detail tugas...");
        descriptionArea.setPrefRowCount(4);
        descriptionArea.setWrapText(true);

        deadlinePicker = new DatePicker();
        deadlinePicker.setEditable(false);
        deadlinePicker.setPromptText("Klik untuk pilih tanggal");
        deadlinePicker.setOnMouseClicked(e -> deadlinePicker.show());
        categoryField = new TextField();
        categoryField.setPromptText("Contoh: Kuliah, Kerja, Pribadi");

        priorityChoiceBox = new ChoiceBox<>(FXCollections.observableArrayList(Task.Priority.values()));
        statusChoiceBox = new ChoiceBox<>(FXCollections.observableArrayList(Task.Status.values()));

        attachFileButton = new Button("📎 Pilih File");
        attachFileButton.setMaxWidth(Double.MAX_VALUE);
        attachFileButton.setOnAction(e -> handleAttachFile());

        removeAttachmentButton = new Button("✕");
        removeAttachmentButton.setOnAction(e -> handleRemoveAttachment());

        attachmentLabel = new Label("Tidak ada file dilampirkan");
        attachmentLabel.getStyleClass().add("attachment-label");
        attachmentLabel.setWrapText(true);

        pendingAttachmentPath = null;
        pendingAttachmentName = null;

        if (taskToEdit != null) {
            titleField.setText(taskToEdit.getTitle());
            descriptionArea.setText(taskToEdit.getDescription());
            deadlinePicker.setValue(taskToEdit.getDeadline());
            categoryField.setText(taskToEdit.getCategory());
            priorityChoiceBox.setValue(taskToEdit.getPriority());
            statusChoiceBox.setValue(taskToEdit.getStatus());
            pendingAttachmentPath = taskToEdit.getAttachmentPath();
            pendingAttachmentName = taskToEdit.getAttachmentName();
        } else {
            priorityChoiceBox.setValue(Task.Priority.MEDIUM);
            statusChoiceBox.setValue(Task.Status.PENDING);
        }
        updateAttachmentLabel();

        VBox form = new VBox(12);
        form.getStyleClass().add("dialog-form");
        form.setPadding(new Insets(4));
        form.setPrefWidth(420);
        form.getChildren().addAll(
                createFormGroup("Judul Tugas", titleField),
                createFormGroup("Deskripsi", descriptionArea),
                createFormGroup("Tenggat Waktu", deadlinePicker),
                createFormGroup("Kategori", categoryField),
                createFormGroup("Tingkat Prioritas", priorityChoiceBox),
                createFormGroup("Status", statusChoiceBox),
                createAttachmentGroup()
        );
        return form;
    }

    private VBox createFormGroup(String labelText, Control control) {
        Label label = new Label(labelText);
        label.getStyleClass().add("field-label");
        control.setMaxWidth(Double.MAX_VALUE);
        return new VBox(6, label, control);
    }

    private VBox createAttachmentGroup() {
        Label label = new Label("Lampiran File");
        label.getStyleClass().add("field-label");

        HBox buttons = new HBox(8, attachFileButton, removeAttachmentButton);
        buttons.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(attachFileButton, Priority.ALWAYS);

        return new VBox(8, label, buttons, attachmentLabel);
    }

    private boolean saveTaskFromDialog() {
        String title = titleField.getText();
        if (title == null || title.isBlank()) {
            showError("Validasi gagal", "Judul tugas tidak boleh kosong.");
            return false;
        }

        Task task = (selectedTask != null) ? selectedTask : new Task();
        task.setTitle(title.trim());
        task.setDescription(descriptionArea.getText());
        task.setDeadline(deadlinePicker.getValue());
        task.setCategory(categoryField.getText());
        task.setPriority(priorityChoiceBox.getValue());
        task.setStatus(statusChoiceBox.getValue());
        task.setAttachmentPath(pendingAttachmentPath);
        task.setAttachmentName(pendingAttachmentName);

        try {
            if (selectedTask == null) {
                taskDAO.addTask(task);
                setStatus("Tugas \"" + task.getTitle() + "\" berhasil ditambahkan.");
            } else {
                taskDAO.updateTask(task);
                setStatus("Tugas \"" + task.getTitle() + "\" berhasil diperbarui.");
            }
            loadAllTasks();
            selectTaskById(task.getId());
            return true;
        } catch (Exception e) {
            showError("Gagal menyimpan tugas", e.getMessage());
            return false;
        }
    }

    @FXML
    private void handleDeleteTask() {
        Task task = taskTableView.getSelectionModel().getSelectedItem();
        if (task == null) {
            showError("Tidak ada yang dipilih", "Pilih tugas pada tabel yang ingin dihapus.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Konfirmasi Hapus");
        confirm.setHeaderText(null);
        confirm.setContentText("Yakin ingin menghapus tugas \"" + task.getTitle() + "\"?");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    if (task.hasAttachment()) {
                        FileAttachmentManager.deleteAttachment(task.getAttachmentPath());
                    }
                    taskDAO.deleteTask(task.getId());
                    setStatus("Tugas \"" + task.getTitle() + "\" berhasil dihapus.");
                    selectedTask = null;
                    updateDetailPanel(null);
                    loadAllTasks();
                } catch (Exception e) {
                    showError("Gagal menghapus tugas", e.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleRefresh() {
        searchField.clear();
        filterStatusChoiceBox.setValue("Semua");
        loadAllTasks();
    }

    // =========================================================
    // ATTACHMENT HANDLING
    // =========================================================

    @FXML
    private void handleAttachFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Pilih File Lampiran");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Semua File", "*.*"),
                new FileChooser.ExtensionFilter("Dokumen", "*.pdf", "*.doc", "*.docx", "*.txt"),
                new FileChooser.ExtensionFilter("Gambar", "*.png", "*.jpg", "*.jpeg"),
                new FileChooser.ExtensionFilter("Spreadsheet", "*.xls", "*.xlsx", "*.csv")
        );

        File chosenFile = fileChooser.showOpenDialog(attachFileButton.getScene().getWindow());
        if (chosenFile != null) {
            try {
                String savedPath = FileAttachmentManager.saveAttachment(chosenFile);
                pendingAttachmentPath = savedPath;
                pendingAttachmentName = chosenFile.getName();
                updateAttachmentLabel();
                setStatus("File \"" + chosenFile.getName() + "\" siap dilampirkan. Klik Simpan untuk konfirmasi.");
            } catch (IOException e) {
                showError("Gagal melampirkan file", e.getMessage());
            }
        }
    }

    @FXML
    private void handleRemoveAttachment() {
        pendingAttachmentPath = null;
        pendingAttachmentName = null;
        updateAttachmentLabel();
    }

    @FXML
    private void handleOpenAttachment() {
        Task task = taskTableView.getSelectionModel().getSelectedItem();
        if (task == null || !task.hasAttachment()) {
            showError("Tidak ada lampiran", "Tugas ini tidak memiliki file lampiran.");
            return;
        }

        try {
            FileAttachmentManager.openAttachment(task.getAttachmentPath());
        } catch (IOException e) {
            showError("Gagal membuka lampiran", e.getMessage());
        }
    }

    private void updateAttachmentLabel() {
        if (attachmentLabel == null) {
            return;
        }
        if (pendingAttachmentName != null && !pendingAttachmentName.isBlank()) {
            attachmentLabel.setText("📎 " + pendingAttachmentName);
        } else {
            attachmentLabel.setText("Tidak ada file dilampirkan");
        }
    }

    private void updateDetailPanel(Task task) {
        boolean hasTask = task != null;
        editTaskButton.setDisable(!hasTask);
        setDetailVisible(hasTask);

        if (!hasTask) {
            detailTitleLabel.setText("Pilih tugas dari tabel");
            detailMetaLabel.setText("Detail tugas akan muncul di sini.");
            detailCategoryLabel.setText("-");
            detailDeadlineLabel.setText("-");
            detailPriorityLabel.setText("-");
            detailStatusLabel.setText("-");
            detailAttachmentLabel.setText("-");
            detailDescriptionArea.setText("");
            return;
        }

        String deadline = task.getDeadline() != null ? task.getDeadline().format(DATE_FORMAT) : "-";
        String category = isBlank(task.getCategory()) ? "-" : task.getCategory();
        String attachment = task.hasAttachment() ? task.getAttachmentName() : "-";
        detailCard.getStyleClass().removeAll("detail-overdue", "detail-done");
        if (task.isOverdue()) {
            detailCard.getStyleClass().add("detail-overdue");
        } else if (task.getStatus() == Task.Status.DONE) {
            detailCard.getStyleClass().add("detail-done");
        }

        detailTitleLabel.setText(task.getTitle());
        detailMetaLabel.setText(getStatusSummary(task));
        detailCategoryLabel.setText(category);
        detailDeadlineLabel.setText(deadline);
        detailPriorityLabel.setText(task.getPriority().name());
        detailStatusLabel.setText(task.getStatus().name());
        detailAttachmentLabel.setText(attachment);
        applyBadgeStyle(detailPriorityLabel, "priority", task.getPriority().name());
        applyBadgeStyle(detailStatusLabel, "status", task.getStatus().name());
        detailDescriptionArea.setText(isBlank(task.getDescription()) ? "Tidak ada deskripsi." : task.getDescription());
    }

    private void setDetailVisible(boolean visible) {
        detailSectionLabel.setVisible(visible);
        detailSectionLabel.setManaged(visible);
        detailCard.setVisible(visible);
        detailCard.setManaged(visible);
        detailFactsGrid.setVisible(visible);
        detailFactsGrid.setManaged(visible);
        detailDescriptionBox.setVisible(visible);
        detailDescriptionBox.setManaged(visible);
        editTaskButton.setVisible(visible);
        editTaskButton.setManaged(visible);
    }

    private String getStatusSummary(Task task) {
        return switch (task.getStatus()) {
            case DONE -> "Selesai";
            case IN_PROGRESS -> "Sedang dikerjakan";
            case PENDING -> task.isOverdue() ? "Pending • Terlambat" : "Pending";
        };
    }

    private TableCell<Task, String> createBadgeCell(String group) {
        return new TableCell<>() {
            private final Label badge = new Label();

            {
                badge.getStyleClass().add("badge");
            }

            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null || value.isBlank()) {
                    setGraphic(null);
                    return;
                }

                badge.setText(value);
                applyBadgeStyle(badge, group, value);
                setGraphic(badge);
            }
        };
    }

    private void applyBadgeStyle(Label label, String group, String value) {
        label.getStyleClass().removeAll(
                "priority-low", "priority-medium", "priority-high",
                "status-pending", "status-progress", "status-done"
        );
        label.getStyleClass().add("badge");

        if ("priority".equals(group)) {
            switch (value) {
                case "LOW" -> label.getStyleClass().add("priority-low");
                case "MEDIUM" -> label.getStyleClass().add("priority-medium");
                case "HIGH" -> label.getStyleClass().add("priority-high");
                default -> { }
            }
            return;
        }

        switch (value) {
            case "PENDING" -> label.getStyleClass().add("status-pending");
            case "IN_PROGRESS" -> label.getStyleClass().add("status-progress");
            case "DONE" -> label.getStyleClass().add("status-done");
            default -> { }
        }
    }

    private void selectTaskById(int taskId) {
        taskList.stream()
                .filter(task -> task.getId() == taskId)
                .findFirst()
                .ifPresent(task -> {
                    taskTableView.getSelectionModel().select(task);
                    taskTableView.scrollTo(task);
                    updateDetailPanel(task);
                });
    }

    @FXML
    private void handlePreviousMonth() {
        visibleCalendarMonth = visibleCalendarMonth.minusMonths(1);
        renderDeadlineCalendar();
    }

    @FXML
    private void handleNextMonth() {
        visibleCalendarMonth = visibleCalendarMonth.plusMonths(1);
        renderDeadlineCalendar();
    }

    private void renderDeadlineCalendar() {
        if (deadlineCalendarGrid == null) {
            return;
        }

        deadlineCalendarGrid.getChildren().clear();
        calendarTitleLabel.setText(visibleCalendarMonth.format(MONTH_FORMAT));

        Map<LocalDate, Long> deadlineCounts = getDeadlineCounts();
        String[] dayNames = {"Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min"};
        for (int i = 0; i < dayNames.length; i++) {
            Label dayLabel = new Label(dayNames[i]);
            dayLabel.getStyleClass().add("calendar-weekday");
            dayLabel.setMaxWidth(Double.MAX_VALUE);
            deadlineCalendarGrid.add(dayLabel, i, 0);
        }

        LocalDate firstDay = visibleCalendarMonth.atDay(1);
        int firstColumn = firstDay.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue();
        int daysInMonth = visibleCalendarMonth.lengthOfMonth();

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = visibleCalendarMonth.atDay(day);
            int cellIndex = firstColumn + day - 1;
            int row = (cellIndex / 7) + 1;
            int column = cellIndex % 7;

            VBox cell = createCalendarCell(date, deadlineCounts.getOrDefault(date, 0L));
            deadlineCalendarGrid.add(cell, column, row);
        }
    }

    private VBox createCalendarCell(LocalDate date, long deadlineCount) {
        Label dayNumber = new Label(String.valueOf(date.getDayOfMonth()));
        dayNumber.getStyleClass().add("calendar-day-number");

        Label marker = new Label(deadlineCount > 0 ? "●" : "");
        marker.getStyleClass().add("calendar-deadline-marker");

        VBox cell = new VBox(1, dayNumber, marker);
        cell.setAlignment(Pos.CENTER);
        cell.getStyleClass().add("calendar-cell");
        if (date.equals(LocalDate.now())) {
            cell.getStyleClass().add("today-cell");
        }
        if (deadlineCount > 0) {
            cell.getStyleClass().add("deadline-cell");
            Tooltip.install(cell, new Tooltip(deadlineCount + " deadline pada " + date.format(DATE_FORMAT)));
        }

        cell.setOnMouseClicked(event -> showTasksForDate(date));
        return cell;
    }

    private Map<LocalDate, Long> getDeadlineCounts() {
        try {
            return taskDAO.getAllTasks().stream()
                    .filter(task -> task.getDeadline() != null)
                    .collect(Collectors.groupingBy(Task::getDeadline, HashMap::new, Collectors.counting()));
        } catch (Exception e) {
            setStatus("Gagal memuat kalender deadline: " + e.getMessage());
            return new HashMap<>();
        }
    }

    private void showTasksForDate(LocalDate date) {
        List<Task> tasksOnDate = taskList.stream()
                .filter(task -> date.equals(task.getDeadline()))
                .toList();

        if (tasksOnDate.isEmpty()) {
            setStatus("Tidak ada deadline pada " + date.format(DATE_FORMAT) + ".");
            return;
        }

        taskTableView.getSelectionModel().select(tasksOnDate.get(0));
        taskTableView.scrollTo(tasksOnDate.get(0));
        setStatus(tasksOnDate.size() + " deadline pada " + date.format(DATE_FORMAT) + ".");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private void setStatus(String message) {
        statusBarLabel.setText(message);
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
