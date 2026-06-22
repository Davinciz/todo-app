package com.todoapp.controller;

import com.todoapp.dao.TaskDAO;
import com.todoapp.model.Task;
import com.todoapp.util.FileAttachmentManager;
import javafx.collections.FXCollections;
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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.prefs.Preferences;

/**
 * Mengelola dialog form untuk membuat / mengedit Task, termasuk
 * pemilihan & pelampiran file. Hasil simpan dikembalikan lewat callback.
 *
 * Field "Mata Kuliah" menyimpan history input ke Preferences (persistent
 * antar sesi) dan menampilkan dropdown autocomplete. Item history bisa
 * dihapus satu per satu lewat tombol ✕ di sebelah kanan tiap item.
 */
public class TaskFormDialog {

    // Key penyimpanan history di java.util.prefs.Preferences
    private static final String PREF_MK_HISTORY = "mk_history";
    // Pemisah antar entri dalam string yang disimpan
    private static final String HISTORY_SEP = "||";
    private static final int MAX_HISTORY = 20;

    private final TaskDAO taskDAO;
    private final String stylesheetUrl;
    private final Preferences prefs = Preferences.userNodeForPackage(TaskFormDialog.class);

    // --- Dialog form fields ---
    private TextField titleField;
    private TextArea descriptionArea;
    private DatePicker deadlinePicker;
    private TextField categoryField;
    private TextField mataKuliahField;
    private ContextMenu mataKuliahPopup;
    private ChoiceBox<Task.Priority> priorityChoiceBox;
    private ChoiceBox<Task.Status> statusChoiceBox;
    private Label attachmentLabel;
    private Button attachFileButton;
    private Button removeAttachmentButton;

    private String pendingAttachmentPath;
    private String pendingAttachmentName;
    private Task taskBeingEdited;

    // --- Error labels untuk validasi inline ---
    private Label titleError;
    private Label deadlineError;
    private Label mataKuliahError;
    private Label priorityError;
    private Label statusError;

    public TaskFormDialog(TaskDAO taskDAO, String stylesheetUrl) {
        this.taskDAO = taskDAO;
        this.stylesheetUrl = stylesheetUrl;
    }

    /**
     * Membuka dialog create/edit. Jika berhasil disimpan, {@code onSaved} dipanggil
     * dengan task hasil simpan dan pesan status yang sesuai.
     */
    public void open(Task taskToEdit, BiConsumer<Task, String> onSaved,
                     String currentTheme, javafx.stage.Window ownerWindow) {
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

    // =========================================================
    // FORM BUILDING
    // =========================================================

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

        mataKuliahField = new TextField();
        mataKuliahField.setPromptText("Contoh: Pemrograman Lanjut, Basis Data...");
        mataKuliahPopup = new ContextMenu();
        setupMataKuliahAutocomplete();

        priorityChoiceBox = new ChoiceBox<>(
                FXCollections.observableArrayList(Task.Priority.values()));
        statusChoiceBox = new ChoiceBox<>(
                FXCollections.observableArrayList(Task.Status.values()));

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

        // Inisialisasi error labels
        titleError     = createErrorLabel();
        deadlineError  = createErrorLabel();
        mataKuliahError = createErrorLabel();
        priorityError  = createErrorLabel();
        statusError    = createErrorLabel();

        // Hapus error saat user mulai mengisi field
        titleField.textProperty().addListener((o, ov, nv) -> clearError(titleField, titleError));
        deadlinePicker.valueProperty().addListener((o, ov, nv) -> clearError(deadlinePicker, deadlineError));
        mataKuliahField.textProperty().addListener((o, ov, nv) -> clearError(mataKuliahField, mataKuliahError));
        priorityChoiceBox.valueProperty().addListener((o, ov, nv) -> clearError(priorityChoiceBox, priorityError));
        statusChoiceBox.valueProperty().addListener((o, ov, nv) -> clearError(statusChoiceBox, statusError));

        if (taskToEdit != null) {
            titleField.setText(taskToEdit.getTitle());
            descriptionArea.setText(taskToEdit.getDescription());
            deadlinePicker.setValue(taskToEdit.getDeadline());
            categoryField.setText(taskToEdit.getCategory());
            mataKuliahField.setText(taskToEdit.getMataKuliah() != null ? taskToEdit.getMataKuliah() : "");
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
                createRequiredFormGroup("Judul Tugas", titleField, titleError),
                createFormGroup("Deskripsi", descriptionArea),
                createRequiredFormGroup("Tenggat Waktu", deadlinePicker, deadlineError),
                createFormGroup("Kategori", categoryField),
                createRequiredFormGroup("Mata Kuliah", mataKuliahField, mataKuliahError),
                createRequiredFormGroup("Tingkat Prioritas", priorityChoiceBox, priorityError),
                createRequiredFormGroup("Status", statusChoiceBox, statusError),
                createAttachmentGroup()
        );
        return form;
    }

    // =========================================================
    // MATA KULIAH TEXTFIELD + CONTEXTMENU AUTOCOMPLETE
    // =========================================================

    /**
     * Menghubungkan mataKuliahField dengan ContextMenu yang:
     * - Muncul saat mengetik (filter contains, case-insensitive)
     * - Tiap item punya tombol ✕ yang benar-benar bisa diklik untuk hapus history
     * - Klik label item mengisi TextField tanpa menutup dialog
     *
     * Pendekatan ContextMenu menghindari bug JavaFX ComboBox di mana cell-selection
     * dan button-action saling konflik satu sama lain.
     */
    private void setupMataKuliahAutocomplete() {
        // Tampilkan/update popup saat teks berubah
        mataKuliahField.textProperty().addListener((obs, oldVal, newVal) -> {
            showFilteredPopup(newVal == null ? "" : newVal);
        });

        // Tutup popup saat field kehilangan fokus
        mataKuliahField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) {
                mataKuliahPopup.hide();
            }
        });

        // Tutup popup saat tekan Escape; Enter mengkonfirmasi nilai
        mataKuliahField.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                mataKuliahPopup.hide();
            }
        });
    }

    /**
     * Membangun ulang isi ContextMenu berdasarkan filter kata kunci,
     * lalu menampilkannya di bawah mataKuliahField.
     */
    private void showFilteredPopup(String keyword) {
        String lower = keyword.toLowerCase();
        List<String> history = loadHistory();
        List<String> filtered = history.stream()
                .filter(h -> h.toLowerCase().contains(lower))
                .toList();

        mataKuliahPopup.getItems().clear();

        if (filtered.isEmpty()) {
            mataKuliahPopup.hide();
            return;
        }

        for (String item : filtered) {
            // Label teks — klik mengisi field
            Label textLabel = new Label(item);
            textLabel.setMaxWidth(Double.MAX_VALUE);
            textLabel.setStyle("-fx-padding: 0 8 0 0;");

            // Tombol hapus — TERPISAH dari label, tidak ada konflik
            Button deleteBtn = new Button("✕");
            deleteBtn.getStyleClass().add("mk-history-delete-btn");
            deleteBtn.setFocusTraversable(false);

            javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            HBox row = new HBox(spacer, deleteBtn);
            row.setAlignment(Pos.CENTER_RIGHT);

            // StackPane: label di kiri, HBox(delete) di kanan
            javafx.scene.layout.StackPane cell = new javafx.scene.layout.StackPane();
            javafx.scene.layout.StackPane.setAlignment(textLabel, javafx.geometry.Pos.CENTER_LEFT);
            javafx.scene.layout.StackPane.setAlignment(row, javafx.geometry.Pos.CENTER_RIGHT);
            cell.getChildren().addAll(textLabel, row);
            cell.setPrefWidth(mataKuliahField.getWidth() > 0
                    ? mataKuliahField.getWidth() - 20 : 340);
            cell.setPadding(new Insets(4, 4, 4, 8));

            CustomMenuItem menuItem = new CustomMenuItem(cell, false); // false = jangan tutup saat klik
            menuItem.getStyleClass().add("mk-autocomplete-item");

            // Klik area label → isi field, tutup popup
            textLabel.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_CLICKED, e -> {
                mataKuliahField.setText(item);
                mataKuliahField.positionCaret(item.length());
                mataKuliahPopup.hide();
                e.consume();
            });

            // Klik spacer/row kiri pun → isi field (UX nyaman)
            cell.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_CLICKED, e -> {
                // Cek apakah yang diklik bukan deleteBtn (node target)
                javafx.scene.Node target = (javafx.scene.Node) e.getTarget();
                // Traverse up untuk cek apakah klik di dalam deleteBtn
                boolean clickedDelete = false;
                javafx.scene.Node n = target;
                while (n != null) {
                    if (n == deleteBtn) { clickedDelete = true; break; }
                    n = n.getParent();
                }
                if (!clickedDelete) {
                    mataKuliahField.setText(item);
                    mataKuliahField.positionCaret(item.length());
                    mataKuliahPopup.hide();
                }
                e.consume();
            });

            // Klik tombol ✕ → hapus dari history, refresh popup
            deleteBtn.setOnAction(e -> {
                removeFromHistory(item);
                // Jika field berisi item yang dihapus, kosongkan
                if (item.equals(mataKuliahField.getText())) {
                    mataKuliahField.clear();
                }
                showFilteredPopup(mataKuliahField.getText());
                e.consume();
            });

            mataKuliahPopup.getItems().add(menuItem);
        }

        // Tampilkan popup tepat di bawah field
        if (!mataKuliahPopup.isShowing() && mataKuliahField.getScene() != null) {
            mataKuliahPopup.show(mataKuliahField,
                    javafx.geometry.Side.BOTTOM, 0, 0);
        }
    }

    private VBox createMataKuliahGroup() {
        Label label = new Label("Mata Kuliah");
        label.getStyleClass().add("field-label");
        mataKuliahField.setMaxWidth(Double.MAX_VALUE);
        return new VBox(6, label, mataKuliahField);
    }

    // =========================================================
    // HISTORY PERSISTENCE (java.util.prefs)
    // =========================================================

    /** Memuat daftar history dari Preferences. Urutan: paling baru di atas. */
    private List<String> loadHistory() {
        String raw = prefs.get(PREF_MK_HISTORY, "");
        if (raw.isBlank()) return new ArrayList<>();
        List<String> result = new ArrayList<>();
        for (String entry : raw.split("\\|\\|")) {
            String trimmed = entry.trim();
            if (!trimmed.isBlank()) result.add(trimmed);
        }
        return result;
    }

    /** Menambah entri baru ke depan history, buang duplikat, batasi MAX_HISTORY. */
    private void addToHistory(String value) {
        if (value == null || value.isBlank()) return;
        String trimmed = value.trim();
        LinkedHashSet<String> set = new LinkedHashSet<>();
        set.add(trimmed); // baru di depan
        set.addAll(loadHistory());
        List<String> limited = new ArrayList<>(set).subList(0, Math.min(set.size(), MAX_HISTORY));
        saveHistory(limited);
    }

    /** Menghapus satu entri dari history. */
    private void removeFromHistory(String value) {
        List<String> history = loadHistory();
        history.remove(value);
        saveHistory(history);
    }

    private void saveHistory(List<String> history) {
        prefs.put(PREF_MK_HISTORY, String.join(HISTORY_SEP, history));
    }

    // =========================================================
    // FORM HELPERS
    // =========================================================

    /** Membuat group field biasa (label + control). */
    private VBox createFormGroup(String labelText, Control control) {
        Label label = new Label(labelText);
        label.getStyleClass().add("field-label");
        control.setMaxWidth(Double.MAX_VALUE);
        return new VBox(6, label, control);
    }

    /**
     * Membuat group field wajib:
     * - Baris atas: [NamaField *]  [pesan error — muncul sejajar di kanan]
     * - Baris bawah: control input
     * Error sejajar label sehingga tidak menambah tinggi dialog.
     */
    private VBox createRequiredFormGroup(String labelText, Control control, Label errorLabel) {
        Label asterisk = new Label(" *");
        asterisk.setStyle("-fx-text-fill: #e53935; -fx-font-weight: bold;");

        Label label = new Label(labelText);
        label.getStyleClass().add("field-label");

        // Spacer mendorong errorLabel ke kanan
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Error sejajar label di baris yang sama
        HBox labelRow = new HBox(0, label, asterisk, spacer, errorLabel);
        labelRow.setAlignment(Pos.CENTER_LEFT);

        control.setMaxWidth(Double.MAX_VALUE);
        return new VBox(6, labelRow, control);
    }

    /** Membuat Label error yang awalnya tersembunyi. */
    private Label createErrorLabel() {
        Label lbl = new Label();
        lbl.setStyle("-fx-text-fill: #e53935; -fx-font-size: 11px;");
        lbl.setVisible(false);
        lbl.setManaged(false); // tidak makan ruang saat tersembunyi
        return lbl;
    }

    /** Menampilkan error pada control + error label (sejajar dengan label field). */
    private void showFieldError(Control control, Label errorLabel, String message) {
        control.setStyle("-fx-border-color: #e53935; -fx-border-width: 1.5px; -fx-border-radius: 6px;");
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    /** Menghapus error dari control + error label. */
    private void clearError(Control control, Label errorLabel) {
        control.setStyle("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
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
        if (attachmentLabel == null) return;
        if (pendingAttachmentName != null && !pendingAttachmentName.isBlank()) {
            attachmentLabel.setText("📎 " + pendingAttachmentName);
        } else {
            attachmentLabel.setText("Tidak ada file dilampirkan");
        }
    }

    // =========================================================
    // SAVE
    // =========================================================

    /**
     * Validasi semua field wajib sekaligus — menampilkan error inline
     * pada setiap field yang bermasalah, bukan berhenti di field pertama.
     *
     * @return Task yang berhasil disimpan, atau null jika ada field kosong
     *         atau penyimpanan gagal.
     */
    private Task trySave() {
        boolean valid = true;

        // --- Judul Tugas ---
        String title = titleField.getText();
        if (title == null || title.isBlank()) {
            showFieldError(titleField, titleError, "Judul tugas wajib diisi.");
            valid = false;
        }

        // --- Tenggat Waktu ---
        if (deadlinePicker.getValue() == null) {
            showFieldError(deadlinePicker, deadlineError, "Tenggat waktu wajib dipilih.");
            valid = false;
        }

        // --- Mata Kuliah ---
        String mataKuliah = mataKuliahField.getText();
        if (mataKuliah == null || mataKuliah.isBlank()) {
            showFieldError(mataKuliahField, mataKuliahError, "Mata kuliah wajib diisi.");
            valid = false;
        }

        // --- Tingkat Prioritas ---
        if (priorityChoiceBox.getValue() == null) {
            showFieldError(priorityChoiceBox, priorityError, "Prioritas wajib dipilih.");
            valid = false;
        }

        // --- Status ---
        if (statusChoiceBox.getValue() == null) {
            showFieldError(statusChoiceBox, statusError, "Status wajib dipilih.");
            valid = false;
        }

        if (!valid) return null;

        Task task = (taskBeingEdited != null) ? taskBeingEdited : new Task();
        task.setTitle(title.trim());
        task.setDescription(descriptionArea.getText());
        task.setDeadline(deadlinePicker.getValue());
        task.setCategory(categoryField.getText());
        task.setMataKuliah(mataKuliah.trim());
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
            addToHistory(mataKuliah);
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