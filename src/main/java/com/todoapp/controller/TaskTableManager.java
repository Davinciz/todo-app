package com.todoapp.controller;

import com.todoapp.model.Task;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

/**
 * Mengelola TableView daftar Task: setup kolom, badge (priority/status),
 * row highlighting (overdue/done), dan listener seleksi baris.
 */
public class TaskTableManager {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final TableView<Task> taskTableView;
    private final ObservableList<Task> taskList = FXCollections.observableArrayList();

    public TaskTableManager(TableView<Task> taskTableView,
                             TableColumn<Task, String> titleColumn,
                             TableColumn<Task, String> categoryColumn,
                             TableColumn<Task, String> deadlineColumn,
                             TableColumn<Task, String> priorityColumn,
                             TableColumn<Task, String> statusColumn,
                             TableColumn<Task, String> attachmentColumn) {
        this.taskTableView = taskTableView;
        setupColumns(titleColumn, categoryColumn, deadlineColumn, priorityColumn, statusColumn, attachmentColumn);
        setupRowFactory();
        taskTableView.setItems(taskList);
    }

    private void setupColumns(TableColumn<Task, String> titleColumn,
                               TableColumn<Task, String> categoryColumn,
                               TableColumn<Task, String> deadlineColumn,
                               TableColumn<Task, String> priorityColumn,
                               TableColumn<Task, String> statusColumn,
                               TableColumn<Task, String> attachmentColumn) {
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));

        deadlineColumn.setCellValueFactory(data -> {
            var deadline = data.getValue().getDeadline();
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
    }

    private void setupRowFactory() {
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
    }

    /** Mendaftarkan callback yang dipanggil setiap kali baris tabel dipilih (bisa null). */
    public void setOnSelectionChanged(Consumer<Task> onSelected) {
        taskTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) ->
                onSelected.accept(newVal));
    }

    /** Mengganti seluruh isi tabel dengan daftar task baru. */
    public void setTasks(java.util.List<Task> tasks) {
        taskList.setAll(tasks);
    }

    /** Memilih & men-scroll ke task tertentu berdasarkan id, lalu mengembalikan task tsb (atau null). */
    public Task selectTaskById(int taskId) {
        return taskList.stream()
                .filter(task -> task.getId() == taskId)
                .findFirst()
                .map(task -> {
                    taskTableView.getSelectionModel().select(task);
                    taskTableView.scrollTo(task);
                    return task;
                })
                .orElse(null);
    }

    /** Memilih & men-scroll ke instance task tertentu. */
    public void selectTask(Task task) {
        taskTableView.getSelectionModel().select(task);
        taskTableView.scrollTo(task);
    }

    public Task getSelectedTask() {
        return taskTableView.getSelectionModel().getSelectedItem();
    }

    public ObservableList<Task> getTaskList() {
        return taskList;
    }

    /** Menerapkan style badge (warna) sesuai grup ("priority"/"status") dan nilainya. */
    public static void applyBadgeStyle(Label label, String group, String value) {
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
}
