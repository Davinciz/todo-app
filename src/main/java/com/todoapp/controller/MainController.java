package com.todoapp.controller;

import com.todoapp.dao.TaskDAO;
import com.todoapp.model.Task;
import com.todoapp.util.FileAttachmentManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Orchestrator utama UI. Tugasnya hanya menghubungkan komponen-komponen
 * (table, dialog, detail panel, calendar, notification, theme) satu sama lain
 * dan menjadi titik masuk untuk semua aksi FXML (@FXML onAction).
 *
 * Logika tampilan masing-masing bagian didelegasikan ke:
 * - {@link ThemeManager}            : dark/light theme
 * - {@link TaskTableManager}        : tabel daftar task
 * - {@link TaskFormDialog}          : dialog create/edit task + attachment
 * - {@link TaskDetailPanel}         : panel detail task terpilih
 * - {@link DeadlineCalendarView}    : kalender & preview deadline
 * - {@link NotificationToastManager}: toast notifikasi deadline
 */
public class MainController {

    @FXML private BorderPane rootPane;

    @FXML private Button openAttachmentButton;
    @FXML private Button deleteButton;
    @FXML private Button refreshButton;
    @FXML private Button newTaskButton;
    @FXML private Button editTaskButton;
    @FXML private Button themeToggleButton;

    @FXML private TextField searchField;
    @FXML private ChoiceBox<String> filterStatusChoiceBox;
    @FXML private ChoiceBox<String> notificationStyleChoiceBox;

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
    @FXML private VBox deadlinePreviewBox;
    @FXML private Label deadlinePreviewTitleLabel;
    @FXML private VBox deadlinePreviewList;

    @FXML private TableView<Task> taskTableView;
    @FXML private TableColumn<Task, String> titleColumn;
    @FXML private TableColumn<Task, String> categoryColumn;
    @FXML private TableColumn<Task, String> deadlineColumn;
    @FXML private TableColumn<Task, String> priorityColumn;
    @FXML private TableColumn<Task, String> statusColumn;
    @FXML private TableColumn<Task, String> attachmentColumn;

    @FXML private Label statusBarLabel;

    private final TaskDAO taskDAO = new TaskDAO();
    private final Preferences preferences = Preferences.userNodeForPackage(MainController.class);

    private ThemeManager themeManager;
    private TaskTableManager tableManager;
    private TaskDetailPanel detailPanel;
    private DeadlineCalendarView calendarView;
    private NotificationToastManager notificationManager;
    private TaskFormDialog formDialog;

    @FXML
    public void initialize() {
        String stylesheetUrl = getClass().getResource("/css/style.css").toExternalForm();

        themeManager = new ThemeManager(rootPane, themeToggleButton, preferences);

        tableManager = new TaskTableManager(
                taskTableView, titleColumn, categoryColumn, deadlineColumn,
                priorityColumn, statusColumn, attachmentColumn
        );
        tableManager.setOnSelectionChanged(this::onTaskSelected);

        detailPanel = new TaskDetailPanel(
                detailSectionLabel, detailCard, detailFactsGrid, detailDescriptionBox,
                detailTitleLabel, detailMetaLabel, detailCategoryLabel, detailDeadlineLabel,
                detailPriorityLabel, detailStatusLabel, detailAttachmentLabel,
                detailDescriptionArea, editTaskButton
        );

        calendarView = new DeadlineCalendarView(
                calendarTitleLabel, deadlineCalendarGrid,
                deadlinePreviewBox, deadlinePreviewTitleLabel, deadlinePreviewList
        );
        calendarView.setTaskSupplier(() -> tableManager.getTaskList());
        calendarView.setOnTaskSelected(this::selectTask);
        calendarView.setStatusReporter(this::setStatus);

        notificationManager = new NotificationToastManager(
                notificationStyleChoiceBox, preferences, stylesheetUrl,
                () -> rootPane.getScene(), () -> themeManager.getCurrentTheme()
        );

        formDialog = new TaskFormDialog(taskDAO, stylesheetUrl);

        setupFilterChoiceBox();
        setupSearchListener();

        loadAllTasks();
        detailPanel.update(null);
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

    private void setupSearchListener() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilter());
    }

    // =========================================================
    // DATA LOADING & FILTERING
    // =========================================================

    private void loadAllTasks() {
        try {
            List<Task> tasks = taskDAO.getAllTasks();
            tableManager.setTasks(tasks);
            calendarView.clearPreview();
            calendarView.render();
            notificationManager.showDeadlineNotifications(tasks);
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

            tableManager.setTasks(result);
            calendarView.clearPreview();
            calendarView.render();
        } catch (Exception e) {
            showError("Gagal memfilter data", e.getMessage());
        }
    }

    // =========================================================
    // FXML ACTION HANDLERS
    // =========================================================

    @FXML
    private void handleShowNewTaskDialog() {
        formDialog.open(null, this::onTaskSaved, themeManager.getCurrentTheme(), rootPane.getScene().getWindow());
    }

    @FXML
    private void handleShowEditTaskDialog() {
        Task task = tableManager.getSelectedTask();
        if (task == null) {
            showError("Tidak ada yang dipilih", "Pilih tugas pada tabel yang ingin diedit.");
            return;
        }
        formDialog.open(task, this::onTaskSaved, themeManager.getCurrentTheme(), rootPane.getScene().getWindow());
    }

    private void onTaskSaved(Task savedTask, String statusMessage) {
        loadAllTasks();
        Task reselected = tableManager.selectTaskById(savedTask.getId());
        detailPanel.update(reselected);
        setStatus(statusMessage);
    }

    @FXML
    private void handleDeleteTask() {
        Task task = tableManager.getSelectedTask();
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
                    detailPanel.update(null);
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

    @FXML
    private void handleToggleTheme() {
        themeManager.toggleTheme();
    }

    @FXML
    private void handleOpenAttachment() {
        Task task = tableManager.getSelectedTask();
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

    @FXML
    private void handlePreviousMonth() {
        calendarView.goToPreviousMonth();
    }

    @FXML
    private void handleNextMonth() {
        calendarView.goToNextMonth();
    }

    // =========================================================
    // SELECTION / HELPERS
    // =========================================================

    private void onTaskSelected(Task task) {
        detailPanel.update(task);
    }

    private void selectTask(Task task) {
        tableManager.selectTask(task);
        detailPanel.update(task);
    }

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
