package com.todoapp.controller;

import com.todoapp.dao.TaskDAO;
import com.todoapp.model.Task;
import com.todoapp.util.ButtonAnimationUtil;
import com.todoapp.util.FileAttachmentManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;

import java.util.List;
import java.util.prefs.Preferences;
import javafx.collections.ListChangeListener;

/**
 * Orchestrator utama UI. Tugasnya hanya menghubungkan komponen-komponen
 * (table, dialog, detail panel, calendar, notification, theme) satu sama lain
 * dan menjadi titik masuk untuk semua aksi FXML (@FXML onAction).
 */
public class MainController {

    @FXML private BorderPane rootPane;

    @FXML private Button deleteButton;
    @FXML private Button refreshButton;
    @FXML private Button newTaskButton;
    @FXML private Button editTaskButton;
    @FXML private Button openAttachmentButton;
    @FXML private Button themeToggleButton;
    @FXML private Button filterButton;

    @FXML private TextField searchField;
    @FXML private ChoiceBox<String> notificationStyleChoiceBox;

    @FXML private Label detailSectionLabel;
    @FXML private VBox detailCard;
    @FXML private GridPane detailFactsGrid;
    @FXML private VBox detailDescriptionBox;
    @FXML private Label detailTitleLabel;
    @FXML private Label detailMetaLabel;
    @FXML private Label detailCategoryLabel;
    @FXML private Label detailCourseLabel;
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
    @FXML private TableColumn<Task, String> mataKuliahColumn;
    @FXML private TableColumn<Task, String> deadlineColumn;
    @FXML private TableColumn<Task, String> priorityColumn;
    @FXML private TableColumn<Task, String> statusColumn;
    @FXML private TableColumn<Task, String> attachmentColumn;

    @FXML private Label statTotalLabel;
    @FXML private Label statDoneLabel;
    @FXML private Label statProgressLabel;
    @FXML private Label statPendingLabel;
    @FXML private Label statCompletionRateLabel;
    @FXML private VBox upcomingDeadlinesBox;

    @FXML private Label statusBarLabel;

    private final TaskDAO taskDAO = new TaskDAO();
    private final Preferences preferences = Preferences.userNodeForPackage(MainController.class);

    // Internal filter controls (di dalam popup)
    private final ChoiceBox<String> filterStatusChoiceBox = new ChoiceBox<>();
    private final ChoiceBox<String> filterCategoryChoiceBox = new ChoiceBox<>();
    private final ChoiceBox<String> filterMataKuliahChoiceBox = new ChoiceBox<>();

    private ThemeManager themeManager;
    private TaskTableManager tableManager;
    private TaskDetailPanel detailPanel;
    private DeadlineCalendarView calendarView;
    private NotificationToastManager notificationManager;
    private TaskFormDialog formDialog;
    private DashboardStatsPanel dashboardStatsPanel;

    @FXML
    public void initialize() {
        String stylesheetUrl = getClass().getResource("/css/style.css").toExternalForm();

        themeManager = new ThemeManager(rootPane, themeToggleButton, preferences);

        tableManager = new TaskTableManager(
                taskTableView, titleColumn, categoryColumn, mataKuliahColumn,
                deadlineColumn, priorityColumn, statusColumn, attachmentColumn
        );
        taskTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tableManager.setOnSelectionChanged(this::onTaskSelected);

        detailPanel = new TaskDetailPanel(
                detailSectionLabel, detailCard, detailFactsGrid, detailDescriptionBox,
                detailTitleLabel, detailMetaLabel, detailCategoryLabel, detailCourseLabel,
                detailDeadlineLabel, detailPriorityLabel, detailStatusLabel, detailAttachmentLabel,
                detailDescriptionArea, editTaskButton, openAttachmentButton
        );

        calendarView = new DeadlineCalendarView(
                calendarTitleLabel, deadlineCalendarGrid,
                deadlinePreviewBox, deadlinePreviewTitleLabel, deadlinePreviewList
        );
        calendarView.setTaskSupplier(() -> tableManager.getTaskList());
        tableManager.getTaskList().addListener((ListChangeListener<Task>) change -> {
            calendarView.clearPreview();
            calendarView.render();
        });
        calendarView.setOnTaskSelected(this::selectTask);
        calendarView.setStatusReporter(this::setStatus);

        notificationManager = new NotificationToastManager(
                notificationStyleChoiceBox, preferences, stylesheetUrl,
                () -> rootPane.getScene(), () -> themeManager.getCurrentTheme()
        );

        formDialog = new TaskFormDialog(taskDAO, stylesheetUrl);

        dashboardStatsPanel = new DashboardStatsPanel(
                statTotalLabel, statDoneLabel, statProgressLabel,
                statPendingLabel, statCompletionRateLabel, upcomingDeadlinesBox
        );
        dashboardStatsPanel.setOnTaskSelected(this::selectTask);

        setupInternalFilters();
        setupSearchListener();

        setupButtonAnimations();

        loadAllTasks();
        refreshFilterDropdowns();
        detailPanel.update(null);
    }
    // =========================================================
    // BUTTON ANIMATIONS
    // =========================================================

    private void setupButtonAnimations() {
        ButtonAnimationUtil.addHoverAndPressedScale(newTaskButton);
        ButtonAnimationUtil.addHoverAndPressedScale(editTaskButton);
        ButtonAnimationUtil.addHoverAndPressedScale(deleteButton);
        ButtonAnimationUtil.addHoverAndPressedScale(refreshButton);
        ButtonAnimationUtil.addHoverAndPressedScale(themeToggleButton, 1.08);
        ButtonAnimationUtil.addHoverAndPressedScale(filterButton);
    }

    // =========================================================
    // SETUP FILTER (internal ChoiceBox di dalam popup)
    // =========================================================

    private void setupInternalFilters() {
        filterStatusChoiceBox.setItems(FXCollections.observableArrayList(
                "Semua", "PENDING", "IN_PROGRESS", "DONE"
        ));
        filterStatusChoiceBox.setValue("Semua");
        filterStatusChoiceBox.setOnAction(e -> applyFilter());

        filterCategoryChoiceBox.setValue("Semua");
        filterCategoryChoiceBox.setOnAction(e -> applyFilter());

        filterMataKuliahChoiceBox.setValue("Semua");
        filterMataKuliahChoiceBox.setOnAction(e -> applyFilter());
    }

    /** Memperbarui isi dropdown kategori & mata kuliah dari database. */
    private void refreshFilterDropdowns() {
        String prevCategory = filterCategoryChoiceBox.getValue();
        String prevMk = filterMataKuliahChoiceBox.getValue();

        List<String> categories = taskDAO.getDistinctCategories();
        List<String> mkList = taskDAO.getDistinctMataKuliah();

        filterCategoryChoiceBox.setItems(FXCollections.observableArrayList("Semua"));
        filterCategoryChoiceBox.getItems().addAll(categories);
        filterCategoryChoiceBox.setValue(categories.contains(prevCategory) ? prevCategory : "Semua");

        filterMataKuliahChoiceBox.setItems(FXCollections.observableArrayList("Semua"));
        filterMataKuliahChoiceBox.getItems().addAll(mkList);
        filterMataKuliahChoiceBox.setValue(mkList.contains(prevMk) ? prevMk : "Semua");

        // Update label tombol filter dengan badge jumlah filter aktif
        updateFilterButtonText();
    }

    private void updateFilterButtonText() {
        int activeCount = 0;
        if (!"Semua".equals(filterStatusChoiceBox.getValue())) activeCount++;
        if (!"Semua".equals(filterCategoryChoiceBox.getValue())) activeCount++;
        if (!"Semua".equals(filterMataKuliahChoiceBox.getValue())) activeCount++;

        if (activeCount > 0) {
            filterButton.setText("🔽 Filter (" + activeCount + ")");
            filterButton.getStyleClass().add("filter-active");
        } else {
            filterButton.setText("🔽 Filter");
            filterButton.getStyleClass().remove("filter-active");
        }
    }

    private void setupSearchListener() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilter());
    }

    // =========================================================
    // FILTER POPUP
    // =========================================================

    @FXML
    private void handleShowFilterPopover() {
        Popup popup = new Popup();
        popup.setAutoHide(true);
        popup.setHideOnEscape(true);

        Label headerLabel = new Label("Filter Tugas");
        headerLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 800; -fx-text-fill: -app-accent-primary; -fx-padding: 0 0 10 0;");

        Label statusLabel = new Label("Status");
        statusLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: -app-text-secondary;");
        filterStatusChoiceBox.setMaxWidth(Double.MAX_VALUE);

        Label categoryLabel = new Label("Kategori");
        categoryLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: -app-text-secondary;");
        filterCategoryChoiceBox.setMaxWidth(Double.MAX_VALUE);

        Label mkLabel = new Label("Mata Kuliah");
        mkLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: -app-text-secondary;");
        filterMataKuliahChoiceBox.setMaxWidth(Double.MAX_VALUE);

        Button resetButton = new Button("Reset Filter");
        resetButton.setMaxWidth(Double.MAX_VALUE);
        resetButton.getStyleClass().add("secondary-button");
        resetButton.setOnAction(e -> {
            filterStatusChoiceBox.setValue("Semua");
            filterCategoryChoiceBox.setValue("Semua");
            filterMataKuliahChoiceBox.setValue("Semua");
            popup.hide();
            applyFilter();
        });

        Button closeButton = new Button("Tutup");
        closeButton.setMaxWidth(Double.MAX_VALUE);
        closeButton.getStyleClass().add("primary-button");
        closeButton.setOnAction(e -> popup.hide());

        VBox content = new VBox(8,
                headerLabel,
                statusLabel, filterStatusChoiceBox,
                categoryLabel, filterCategoryChoiceBox,
                mkLabel, filterMataKuliahChoiceBox,
                new Separator(),
                resetButton, closeButton
        );
        content.getStyleClass().add("filter-popup-content");
        headerLabel.getStyleClass().add("filter-popup-title");
        statusLabel.getStyleClass().add("filter-popup-field-label");
        categoryLabel.getStyleClass().add("filter-popup-field-label");
        mkLabel.getStyleClass().add("filter-popup-field-label");
        content.setPadding(new Insets(16));
        content.setPrefWidth(220);

        popup.getContent().add(content);

        // Posisikan popup di bawah tombol filter
        popup.show(filterButton, filterButton.localToScreen(0, filterButton.getHeight()).getX(),
                   filterButton.localToScreen(0, filterButton.getHeight()).getY());
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
            dashboardStatsPanel.render(tasks);
            notificationManager.showDeadlineNotifications(tasks);
            setStatus("Berhasil memuat " + tasks.size() + " tugas.");
        } catch (Exception e) {
            showError("Gagal memuat data", e.getMessage());
        }
    }

    private void applyFilter() {
        String keyword = searchField.getText();
        String statusFilter = filterStatusChoiceBox.getValue();
        String categoryFilter = filterCategoryChoiceBox.getValue();
        String mkFilter = filterMataKuliahChoiceBox.getValue();

        try {
            List<Task> result = taskDAO.getFilteredTasks(
                    keyword, statusFilter, categoryFilter, mkFilter
            );
            tableManager.setTasks(result);
            calendarView.clearPreview();
            calendarView.render();
            updateFilterButtonText();
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
        refreshFilterDropdowns();
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
                    refreshFilterDropdowns();
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
        filterCategoryChoiceBox.setValue("Semua");
        filterMataKuliahChoiceBox.setValue("Semua");
        loadAllTasks();
        refreshFilterDropdowns();
        tableManager.clearSelection();
        detailPanel.update(null);
    }

    @FXML
    private void handleToggleTheme() {
        themeManager.toggleTheme();
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