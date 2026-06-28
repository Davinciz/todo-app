package com.todoapp.controller;

import com.todoapp.model.Task;
import com.todoapp.util.FileAttachmentManager;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

/**
 * Mengelola panel detail tugas: menampilkan info lengkap task yang sedang
 * dipilih pada tabel, atau placeholder kosong jika tidak ada yang dipilih.
 */
public class TaskDetailPanel {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final Label detailSectionLabel;
    private final VBox detailCard;
    private final GridPane detailFactsGrid;
    private final VBox detailDescriptionBox;
    private final Label detailTitleLabel;
    private final Label detailMetaLabel;
    private final Label detailCategoryLabel;
    private final Label detailCourseLabel;
    private final Label detailDeadlineLabel;
    private final Label detailPriorityLabel;
    private final Label detailStatusLabel;
    private final Label detailAttachmentLabel;
    private final TextArea detailDescriptionArea;
    private final Button editTaskButton;
    private final Button openAttachmentButton;
    
    private Task currentTask;

    public TaskDetailPanel(Label detailSectionLabel,
                            VBox detailCard,
                            GridPane detailFactsGrid,
                            VBox detailDescriptionBox,
                            Label detailTitleLabel,
                            Label detailMetaLabel,
                            Label detailCategoryLabel,
                            Label detailCourseLabel,
                            Label detailDeadlineLabel,
                            Label detailPriorityLabel,
                            Label detailStatusLabel,
                            Label detailAttachmentLabel,
                            TextArea detailDescriptionArea,
                            Button editTaskButton,
                            Button openAttachmentButton) {
        this.detailSectionLabel = detailSectionLabel;
        this.detailCard = detailCard;
        this.detailFactsGrid = detailFactsGrid;
        this.detailDescriptionBox = detailDescriptionBox;
        this.detailTitleLabel = detailTitleLabel;
        this.detailMetaLabel = detailMetaLabel;
        this.detailCategoryLabel = detailCategoryLabel;
        this.detailCourseLabel = detailCourseLabel;
        this.detailDeadlineLabel = detailDeadlineLabel;
        this.detailPriorityLabel = detailPriorityLabel;
        this.detailStatusLabel = detailStatusLabel;
        this.detailAttachmentLabel = detailAttachmentLabel;
        this.detailDescriptionArea = detailDescriptionArea;
        this.editTaskButton = editTaskButton;
        this.openAttachmentButton = openAttachmentButton;
        
        setupOpenAttachmentButton();
    }
    
    private void setupOpenAttachmentButton() {
        openAttachmentButton.setOnAction(e -> {
            if (currentTask != null && currentTask.hasAttachment()) {
                try {
                    FileAttachmentManager.openAttachment(currentTask.getAttachmentPath());
                } catch (IOException ex) {
                    System.err.println("Error opening attachment: " + ex.getMessage());
                }
            }
        });
    }

    /** Memperbarui seluruh isi panel detail sesuai task yang diberikan (null = kosongkan). */
    public void update(Task task) {
        this.currentTask = task;
        boolean hasTask = task != null;
        editTaskButton.setDisable(!hasTask);
        
        boolean hasAttachment = hasTask && task.hasAttachment();
        openAttachmentButton.setVisible(hasAttachment);
        openAttachmentButton.setManaged(hasAttachment);
        
        setVisible(hasTask);

        if (!hasTask) {
            detailCard.getStyleClass().removeAll("detail-overdue", "detail-done");
            detailTitleLabel.setText("Pilih tugas dari tabel");
            detailMetaLabel.setText("Detail tugas akan muncul di sini.");
            detailCategoryLabel.setText("-");
            detailCourseLabel.setText("-");
            detailDeadlineLabel.setText("-");
            detailPriorityLabel.setText("-");
            detailStatusLabel.setText("-");
            detailAttachmentLabel.setText("-");
            detailDescriptionArea.setText("");
            return;
        }

        String deadline = task.getDeadline() != null ? task.getDeadline().format(DATE_FORMAT) : "-";
        String category = isBlank(task.getCategory()) ? "-" : task.getCategory();
        String mataKuliah = isBlank(task.getMataKuliah()) ? "-" : task.getMataKuliah();
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
        detailCourseLabel.setText(mataKuliah);
        detailDeadlineLabel.setText(deadline);
        detailPriorityLabel.setText(task.getPriority().name());
        detailStatusLabel.setText(task.getStatus().name());
        detailAttachmentLabel.setText(attachment);

        TaskTableManager.applyBadgeStyle(detailPriorityLabel, "priority", task.getPriority().name());
        TaskTableManager.applyBadgeStyle(detailStatusLabel, "status", task.getStatus().name());

        detailDescriptionArea.setText(isBlank(task.getDescription()) ? "Tidak ada deskripsi." : task.getDescription());
    }

    private void setVisible(boolean visible) {
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

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
