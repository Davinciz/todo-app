package com.todoapp.controller;

import com.todoapp.model.Task;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Mengelola kalender deadline (grid bulanan) beserta panel preview daftar
 * tugas pada tanggal yang dipilih.
 */
public class DeadlineCalendarView {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("MMMM yyyy");
    private static final String[] DAY_NAMES = {"Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min"};

    private final Label calendarTitleLabel;
    private final GridPane deadlineCalendarGrid;
    private final VBox deadlinePreviewBox;
    private final Label deadlinePreviewTitleLabel;
    private final VBox deadlinePreviewList;

    private YearMonth visibleMonth = YearMonth.now();
    private LocalDate selectedDate = null;

    /** Dipanggil saat sebuah task pada preview list diklik (misal untuk memilihnya di tabel). */
    private Consumer<Task> onTaskSelected = task -> { };

    /** Supplier untuk mengambil daftar task terkini (dari sumber data eksternal, mis. TaskTableManager). */
    private java.util.function.Supplier<List<Task>> taskSupplier = List::of;

    /** Dipanggil untuk menampilkan pesan status (opsional). */
    private Consumer<String> statusReporter = msg -> { };

    public DeadlineCalendarView(Label calendarTitleLabel,
                                 GridPane deadlineCalendarGrid,
                                 VBox deadlinePreviewBox,
                                 Label deadlinePreviewTitleLabel,
                                 VBox deadlinePreviewList) {
        this.calendarTitleLabel = calendarTitleLabel;
        this.deadlineCalendarGrid = deadlineCalendarGrid;
        this.deadlinePreviewBox = deadlinePreviewBox;
        this.deadlinePreviewTitleLabel = deadlinePreviewTitleLabel;
        this.deadlinePreviewList = deadlinePreviewList;
    }

    public void setOnTaskSelected(Consumer<Task> onTaskSelected) {
        this.onTaskSelected = onTaskSelected;
    }

    public void setTaskSupplier(java.util.function.Supplier<List<Task>> taskSupplier) {
        this.taskSupplier = taskSupplier;
    }

    public void setStatusReporter(Consumer<String> statusReporter) {
        this.statusReporter = statusReporter;
    }

    public void goToPreviousMonth() {
        visibleMonth = visibleMonth.minusMonths(1);
        clearPreview();
        render();
    }

    public void goToNextMonth() {
        visibleMonth = visibleMonth.plusMonths(1);
        clearPreview();
        render();
    }

    /** Merender ulang grid kalender untuk bulan yang sedang ditampilkan. */
    public void render() {
        if (deadlineCalendarGrid == null) {
            return;
        }

        deadlineCalendarGrid.getChildren().clear();
        calendarTitleLabel.setText(visibleMonth.format(MONTH_FORMAT));

        Map<LocalDate, Long> deadlineCounts = getDeadlineCounts();

        for (int i = 0; i < DAY_NAMES.length; i++) {
            Label dayLabel = new Label(DAY_NAMES[i]);
            dayLabel.getStyleClass().add("calendar-weekday");
            dayLabel.setMaxWidth(Double.MAX_VALUE);
            deadlineCalendarGrid.add(dayLabel, i, 0);
        }

        LocalDate firstDay = visibleMonth.atDay(1);
        int firstColumn = firstDay.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue();
        int daysInMonth = visibleMonth.lengthOfMonth();

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = visibleMonth.atDay(day);
            int cellIndex = firstColumn + day - 1;
            int row = (cellIndex / 7) + 1;
            int column = cellIndex % 7;

            VBox cell = createCalendarCell(date, deadlineCounts.getOrDefault(date, 0L));
            deadlineCalendarGrid.add(cell, column, row);
        }
    }

    /** Reset state (dipanggil saat data task berubah, misalnya setelah reload/filter). */
    public void clearPreview() {
        if (deadlinePreviewBox == null || deadlinePreviewList == null) {
            return;
        }
        deadlinePreviewList.getChildren().clear();
        deadlinePreviewBox.setVisible(false);
        deadlinePreviewBox.setManaged(false);
    }

    private VBox createCalendarCell(LocalDate date, long deadlineCount) {
        Label dayNumber = new Label(String.valueOf(date.getDayOfMonth()));
        dayNumber.getStyleClass().add("calendar-day-number");

        // If there are tasks on this date, show the first task title under the day number
        String dayTaskText = "";
        List<Task> tasksOnDate = taskSupplier.get().stream()
            .filter(t -> date.equals(t.getDeadline()))
            .collect(Collectors.toList());
        if (!tasksOnDate.isEmpty()) {
            Task first = tasksOnDate.get(0);
            dayTaskText = first.getTitle();
        }

        Label dayTaskLabel = new Label(dayTaskText);
        dayTaskLabel.getStyleClass().add("calendar-day-task");

        VBox cell = new VBox(2, dayNumber, dayTaskLabel);
        cell.setAlignment(Pos.CENTER);
        cell.getStyleClass().add("calendar-cell");
        if (date.equals(LocalDate.now())) {
            cell.getStyleClass().add("today-cell");
        }
        if (date.equals(selectedDate)) {
            cell.getStyleClass().add("selected-date-cell");
        }
        if (deadlineCount > 0) {
            cell.getStyleClass().add("deadline-cell");
            // show tooltip with titles of tasks on this date
            String tooltipText = tasksOnDate.stream().map(Task::getTitle).collect(Collectors.joining("\n"));
            Tooltip.install(cell, new Tooltip(tooltipText));
        }

        cell.setOnMouseClicked(event -> showTasksForDate(date));
        return cell;
    }

    private Map<LocalDate, Long> getDeadlineCounts() {
        return taskSupplier.get().stream()
                .filter(task -> task.getDeadline() != null)
                .collect(Collectors.groupingBy(Task::getDeadline, HashMap::new, Collectors.counting()));
    }

    private void showTasksForDate(LocalDate date) {
        selectedDate = date;
        render();

        List<Task> tasksOnDate = taskSupplier.get().stream()
                .filter(task -> date.equals(task.getDeadline()))
                .toList();

        if (tasksOnDate.isEmpty()) {
            clearPreview();
            statusReporter.accept("Tidak ada deadline pada " + date.format(DATE_FORMAT) + ".");
            return;
        }

        showPreview(date, tasksOnDate);
        if (tasksOnDate.size() == 1) {
            onTaskSelected.accept(tasksOnDate.get(0));
        }
        statusReporter.accept(tasksOnDate.size() + " deadline pada " + date.format(DATE_FORMAT) + ".");
    }

    private void showPreview(LocalDate date, List<Task> tasksOnDate) {
        // We still populate the preview list for potential future uses,
        // but hide the standalone preview panel per UI preference.
        if (deadlinePreviewList != null) {
            deadlinePreviewTitleLabel.setText("Deadline " + date.format(DATE_FORMAT) + " (" + tasksOnDate.size() + ")");
            deadlinePreviewList.getChildren().clear();
            for (Task task : tasksOnDate) {
                deadlinePreviewList.getChildren().add(createPreviewItem(task));
            }
        }
        if (deadlinePreviewBox != null) {
            deadlinePreviewBox.setVisible(false);
            deadlinePreviewBox.setManaged(false);
        }
    }

    private VBox createPreviewItem(Task task) {
        Label title = new Label(task.getTitle());
        title.getStyleClass().add("deadline-preview-item-title");
        title.setWrapText(true);

        Label meta = new Label(getPreviewMeta(task));
        meta.getStyleClass().add("deadline-preview-item-meta");
        meta.setWrapText(true);

        Label status = new Label(task.getStatus().name());
        TaskTableManager.applyBadgeStyle(status, "status", task.getStatus().name());

        HBox header = new HBox(8, title, status);
        header.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(title, Priority.ALWAYS);

        VBox item = new VBox(4, header, meta);
        item.getStyleClass().add("deadline-preview-item");
        item.setMaxWidth(Double.MAX_VALUE);
        item.setOnMouseClicked(event -> onTaskSelected.accept(task));
        return item;
    }

    private String getPreviewMeta(Task task) {
        String category = isBlank(task.getCategory()) ? "Tanpa kategori" : task.getCategory();
        return category + " • " + task.getPriority().name();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
