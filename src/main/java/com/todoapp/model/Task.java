package com.todoapp.model;

import java.time.LocalDate;

/**
 * Model class merepresentasikan satu Task (tugas) dalam to-do list.
 */
public class Task {

    public enum Priority {
        LOW, MEDIUM, HIGH
    }

    public enum Status {
        PENDING, IN_PROGRESS, DONE
    }

    private int id;
    private String title;
    private String description;
    private LocalDate deadline;
    private Priority priority;
    private Status status;
    private String category;
    private String mataKuliah;
    private String attachmentPath;
    private String attachmentName;

    public Task() {
        this.priority = Priority.MEDIUM;
        this.status = Status.PENDING;
    }

    public Task(String title, String description, LocalDate deadline,
                Priority priority, String category) {
        this.title = title;
        this.description = description;
        this.deadline = deadline;
        this.priority = priority;
        this.category = category;
        this.status = Status.PENDING;
    }

    // Getters and Setters

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }

    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getMataKuliah() { return mataKuliah; }
    public void setMataKuliah(String mataKuliah) { this.mataKuliah = mataKuliah; }

    public String getAttachmentPath() { return attachmentPath; }
    public void setAttachmentPath(String attachmentPath) { this.attachmentPath = attachmentPath; }

    public String getAttachmentName() { return attachmentName; }
    public void setAttachmentName(String attachmentName) { this.attachmentName = attachmentName; }

    public boolean hasAttachment() {
        return attachmentPath != null && !attachmentPath.isBlank();
    }

    public boolean isOverdue() {
        return deadline != null
                && deadline.isBefore(LocalDate.now())
                && status != Status.DONE;
    }

    @Override
    public String toString() {
        return title;
    }
}