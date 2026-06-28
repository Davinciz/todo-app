package com.todoapp.controller;

import com.todoapp.model.Task;
import com.todoapp.util.GlassEffectBuilder;
import com.todoapp.util.SpringAnimationUtil;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * Mengelola panel dashboard ringkasan: kartu statistik (Total, Selesai,
 * In Progress, Belum Mulai, Completion Rate) dan daftar "Upcoming Deadlines"
 * (tugas dengan deadline dalam 14 hari ke depan).
 *
 * Murni tampilan turunan dari data task yang sudah ada — tidak menyentuh
 * database atau menambah kolom baru, jadi tidak mengubah skema yang sudah
 * dipakai bagian lain aplikasi.
 */
public class DashboardStatsPanel {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final int UPCOMING_HORIZON_DAYS = 14;
    private static final int UPCOMING_MAX_ITEMS = 6;

    private final Label statTotalLabel;
    private final Label statDoneLabel;
    private final Label statProgressLabel;
    private final Label statPendingLabel;
    private final Label statCompletionRateLabel;
    private final VBox upcomingDeadlinesBox;

    /** Dipanggil saat sebuah item upcoming deadline diklik (untuk memilihnya di tabel). */
    private Consumer<Task> onTaskSelected = task -> { };

    public DashboardStatsPanel(Label statTotalLabel,
                                Label statDoneLabel,
                                Label statProgressLabel,
                                Label statPendingLabel,
                                Label statCompletionRateLabel,
                                VBox upcomingDeadlinesBox) {
        this.statTotalLabel = statTotalLabel;
        this.statDoneLabel = statDoneLabel;
        this.statProgressLabel = statProgressLabel;
        this.statPendingLabel = statPendingLabel;
        this.statCompletionRateLabel = statCompletionRateLabel;
        this.upcomingDeadlinesBox = upcomingDeadlinesBox;
    }

    public void setOnTaskSelected(Consumer<Task> onTaskSelected) {
        this.onTaskSelected = onTaskSelected;
    }

    /** Merender ulang kartu statistik dan daftar upcoming deadlines berdasarkan data task terkini. */
    public void render(List<Task> tasks) {
        updateStatCards(tasks);
        renderUpcomingDeadlines(tasks);
    }

    private void updateStatCards(List<Task> tasks) {
        long total = tasks.size();
        long done = tasks.stream().filter(t -> t.getStatus() == Task.Status.DONE).count();
        long inProgress = tasks.stream().filter(t -> t.getStatus() == Task.Status.IN_PROGRESS).count();
        long notStarted = tasks.stream().filter(t -> t.getStatus() == Task.Status.PENDING).count();
        long completionRate = total == 0 ? 0 : Math.round((done * 100.0) / total);

        statTotalLabel.setText(String.valueOf(total));
        statDoneLabel.setText(String.valueOf(done));
        statProgressLabel.setText(String.valueOf(inProgress));
        statPendingLabel.setText(String.valueOf(notStarted));
        statCompletionRateLabel.setText(completionRate + "%");
    }

    private void renderUpcomingDeadlines(List<Task> tasks) {
        upcomingDeadlinesBox.getChildren().clear();

        LocalDate today = LocalDate.now();
        LocalDate horizon = today.plusDays(UPCOMING_HORIZON_DAYS);

        List<Task> upcoming = tasks.stream()
                .filter(t -> t.getStatus() != Task.Status.DONE)
                .filter(t -> t.getDeadline() != null)
                .filter(t -> !t.getDeadline().isAfter(horizon))
                .sorted(Comparator.comparing(Task::getDeadline))
                .limit(UPCOMING_MAX_ITEMS)
                .toList();

        if (upcoming.isEmpty()) {
            Label empty = new Label("Tidak ada deadline dalam 14 hari ke depan.");
            empty.getStyleClass().add("detail-meta");
            upcomingDeadlinesBox.getChildren().add(empty);
            return;
        }

        for (Task task : upcoming) {
            upcomingDeadlinesBox.getChildren().add(createUpcomingDeadlineRow(task));
        }
    }

    private HBox createUpcomingDeadlineRow(Task task) {
        VBox textColumn = new VBox(2);
        Label titleLabel = new Label(task.getTitle());
        titleLabel.getStyleClass().add("upcoming-title");
        titleLabel.setWrapText(true);

        String categoryText = isBlank(task.getCategory()) ? "Tanpa kategori" : task.getCategory();
        Label metaLabel = new Label(categoryText + " • " + task.getDeadline().format(DATE_FORMAT));
        metaLabel.getStyleClass().add("upcoming-meta");

        textColumn.getChildren().addAll(titleLabel, metaLabel);
        HBox.setHgrow(textColumn, Priority.ALWAYS);

        Label statusBadge = new Label(statusDisplayName(task.getStatus()));
        TaskTableManager.applyBadgeStyle(statusBadge, "status", task.getStatus().name());

        HBox row = new HBox(10, textColumn, statusBadge);
        row.getStyleClass().add("upcoming-row");
        // Add priority marker class for left border color
        switch (task.getPriority()) {
            case HIGH -> row.getStyleClass().add("upcoming-priority-high");
            case MEDIUM -> row.getStyleClass().add("upcoming-priority-medium");
            case LOW -> row.getStyleClass().add("upcoming-priority-low");
        }
        // Highlight if deadline within 7 days
        if (!task.isOverdue() && task.getDeadline() != null) {
            LocalDate today = LocalDate.now();
            if (!task.getDeadline().isBefore(today) && !task.getDeadline().isAfter(today.plusDays(7))) {
                row.getStyleClass().add("upcoming-soon");
            }
        }
        row.setAlignment(Pos.CENTER_LEFT);

        if (task.isOverdue()) {
            row.getStyleClass().add("upcoming-overdue");
        }

        row.setOnMouseClicked(event -> onTaskSelected.accept(task));
        
        SpringAnimationUtil.applyHoverAnimation(row);

        return row;
    }

    private String statusDisplayName(Task.Status status) {
        return switch (status) {
            case PENDING -> "Belum Mulai";
            case IN_PROGRESS -> "In Progress";
            case DONE -> "Selesai";
        };
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
