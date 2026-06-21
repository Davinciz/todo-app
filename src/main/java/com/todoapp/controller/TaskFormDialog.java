package com.todoapp.controller;

import com.todoapp.dao.TaskDAO;
import com.todoapp.model.Task;
import com.todoapp.util.FileAttachmentManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.util.function.BiConsumer;

/**
 * Mengelola dialog form untuk membuat / mengedit Task, termasuk
 * pemilihan & pelampiran file. Hasil simpan dikembalikan lewat callback.
 */
public class TaskFormDialog {

    private final TaskDAO taskDAO;
    private final String stylesheetUrl;

    // --- Dialog form fields (dibuat ulang setiap dialog dibuka) ---
    private TextField titleField;
    private TextArea descriptionArea;
    private DatePicker deadlinePicker;
    private TextField categoryField;
    private ChoiceBox<Task.Priority> priorityChoiceBox;
    private ChoiceBox<Task.Status> statusChoiceBox;
    private Label attachmentLabel;
    private Button attachFileButton;
    private Button removeAttachmentButton;

    private String pendingAttachmentPath;
    private String pendingAttachmentName;
    private Task taskBeingEdited;

    /**
     * @param taskDAO       DAO untuk menyimpan task ke database
     * @param stylesheetUrl URL stylesheet CSS aplikasi
     */
    public TaskFormDialog(TaskDAO taskDAO, String stylesheetUrl) {
        this.taskDAO = taskDAO;
        this.stylesheetUrl = stylesheetUrl;
    }

    /**
     * Membuka dialog create/edit. Jika berhasil disimpan, {@code onSaved} dipanggil
     * dengan task hasil simpan dan pesan status yang sesuai.
     *
     * @param taskToEdit task yang akan diedit, atau null untuk membuat task baru
     * @param onSaved    callback (task, statusMessage) dipanggil setelah berhasil simpan
     */
    public void open(Task taskToEdit, BiConsumer<Task, String> onSaved, String currentTheme, javafx.stage.Window ownerWindow) {
        this.taskBeingEdited = taskToEdit;
        VBox form = createTaskForm(taskToEdit);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(taskToEdit == null ? "Buat Tugas Baru" : "Edit Tugas");
        dialog.setHeaderText(null);
        dialog.getDialogPane().getStyleClass().add("task-dialog");
        dialog.getDialogPane().getStylesheets().add(stylesheetUrl);
        dialog.getDialogPane().getStyleClass().add(currentTheme);
        dialog.getDialogPane().setContent(form);

        ButtonType saveType = new ButtonType("Simpan", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        Node saveButton = dialog.getDialogPane().lookupButton(saveType);
        saveButton.getStyleClass().add("primary-button");
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            Task saved = trySave();
            if (saved == null) {
                event.consume();
            } else {
                String message = taskToEdit == null
                        ? "Tugas \"" + saved.getTitle() + "\" berhasil ditambahkan."
                        : "Tugas \"" + saved.getTitle() + "\" berhasil diperbarui.";
                onSaved.accept(saved, message);
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

        priorityChoiceBox = new ChoiceBox<>(javafx.collections.FXCollections.observableArrayList(Task.Priority.values()));
        statusChoiceBox = new ChoiceBox<>(javafx.collections.FXCollections.observableArrayList(Task.Status.values()));

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
            } catch (IOException e) {
                showError("Gagal melampirkan file", e.getMessage());
            }
        }
    }

    private void handleRemoveAttachment() {
        pendingAttachmentPath = null;
        pendingAttachmentName = null;
        updateAttachmentLabel();
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

    /** @return Task yang berhasil disimpan, atau null jika validasi/penyimpanan gagal. */
    private Task trySave() {
        String title = titleField.getText();
        if (title == null || title.isBlank()) {
            showError("Validasi gagal", "Judul tugas tidak boleh kosong.");
            return null;
        }

        Task task = (taskBeingEdited != null) ? taskBeingEdited : new Task();
        task.setTitle(title.trim());
        task.setDescription(descriptionArea.getText());
        task.setDeadline(deadlinePicker.getValue());
        task.setCategory(categoryField.getText());
        task.setPriority(priorityChoiceBox.getValue());
        task.setStatus(statusChoiceBox.getValue());
        task.setAttachmentPath(pendingAttachmentPath);
        task.setAttachmentName(pendingAttachmentName);

        try {
            if (taskBeingEdited == null) {
                taskDAO.addTask(task);
            } else {
                taskDAO.updateTask(task);
            }
            return task;
        } catch (Exception e) {
            showError("Gagal menyimpan tugas", e.getMessage());
            return null;
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
