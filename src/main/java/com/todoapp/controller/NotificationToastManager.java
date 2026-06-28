package com.todoapp.controller;

import com.todoapp.util.SpringAnimationUtil;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;

import com.todoapp.model.Task;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

/**
 * Mengelola tampilan toast notifikasi (popup) untuk deadline tugas yang mendekat,
 * termasuk pemilihan style notifikasi (Minimal / Aksen / Urgent).
 */
public class NotificationToastManager {

    public static final String NOTIFICATION_STYLE_MINIMAL = "Minimal";
    public static final String NOTIFICATION_STYLE_ACCENT = "Aksen";
    public static final String NOTIFICATION_STYLE_URGENT = "Urgent";
    private static final String PREF_NOTIFICATION_STYLE = "notificationStyle";

    private final ChoiceBox<String> notificationStyleChoiceBox;
    private final Preferences preferences;
    private final String stylesheetUrl;
    private final java.util.function.Supplier<javafx.scene.Scene> sceneSupplier;
    private final java.util.function.Supplier<String> currentThemeSupplier;

    private final Set<String> shownDeadlineNotificationKeys = new HashSet<>();

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    /**
     * @param notificationStyleChoiceBox ChoiceBox untuk memilih style notifikasi
     * @param preferences                penyimpan preferensi pengguna
     * @param stylesheetUrl              URL stylesheet CSS aplikasi (untuk toast)
     * @param sceneSupplier              supplier yang mengembalikan Scene aktif (untuk menentukan posisi popup)
     * @param currentThemeSupplier       supplier yang mengembalikan kelas tema saat ini (theme-dark/theme-light)
     */
    public NotificationToastManager(ChoiceBox<String> notificationStyleChoiceBox,
                                     Preferences preferences,
                                     String stylesheetUrl,
                                     java.util.function.Supplier<javafx.scene.Scene> sceneSupplier,
                                     java.util.function.Supplier<String> currentThemeSupplier) {
        this.notificationStyleChoiceBox = notificationStyleChoiceBox;
        this.preferences = preferences;
        this.stylesheetUrl = stylesheetUrl;
        this.sceneSupplier = sceneSupplier;
        this.currentThemeSupplier = currentThemeSupplier;

        setupNotificationStyleChoiceBox();
    }

    private void setupNotificationStyleChoiceBox() {
        notificationStyleChoiceBox.setItems(javafx.collections.FXCollections.observableArrayList(
                NOTIFICATION_STYLE_ACCENT,
                NOTIFICATION_STYLE_MINIMAL,
                NOTIFICATION_STYLE_URGENT
        ));
        notificationStyleChoiceBox.setValue(
                preferences.get(PREF_NOTIFICATION_STYLE, NOTIFICATION_STYLE_ACCENT)
        );
        notificationStyleChoiceBox.setTooltip(new javafx.scene.control.Tooltip("Style notifikasi deadline"));
        notificationStyleChoiceBox.setOnAction(event -> {
            String style = notificationStyleChoiceBox.getValue();
            preferences.put(PREF_NOTIFICATION_STYLE, style);
            showToast(
                    "Style notifikasi: " + style,
                    "Pengingat deadline berikutnya akan memakai style ini.",
                    style
            );
        });
    }

    /**
     * Mengevaluasi daftar tugas dan menampilkan notifikasi toast jika ada
     * tugas dengan deadline kurang dari 1 hari yang belum pernah diberitahukan.
     */
    public void showDeadlineNotifications(List<Task> tasks) {
        List<Task> dueSoonTasks = tasks.stream()
                .filter(this::shouldNotifyDeadline)
                .toList();

        if (dueSoonTasks.isEmpty()) {
            return;
        }

        String key = dueSoonTasks.stream()
                .map(task -> task.getId() + ":" + task.getDeadline())
                .collect(Collectors.joining("|"));
        if (!shownDeadlineNotificationKeys.add(key)) {
            return;
        }

        String title = dueSoonTasks.size() == 1
                ? "Deadline kurang dari 1 hari"
                : dueSoonTasks.size() + " tugas mendekati deadline";
        String message = buildDeadlineNotificationMessage(dueSoonTasks);

        Platform.runLater(() -> showToast(
                title,
                message,
                notificationStyleChoiceBox.getValue()
        ));
    }

    private boolean shouldNotifyDeadline(Task task) {
        if (task == null || task.getDeadline() == null || task.getStatus() == Task.Status.DONE) {
            return false;
        }

        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        return !task.getDeadline().isBefore(today) && !task.getDeadline().isAfter(tomorrow);
    }

    private String buildDeadlineNotificationMessage(List<Task> tasks) {
        if (tasks.size() == 1) {
            Task task = tasks.get(0);
            return task.getTitle() + " - deadline " + task.getDeadline().format(DATE_FORMAT) + ".";
        }

        String titles = tasks.stream()
                .limit(3)
                .map(Task::getTitle)
                .collect(Collectors.joining(", "));
        int remaining = tasks.size() - 3;
        return remaining > 0 ? titles + ", dan " + remaining + " lainnya." : titles + ".";
    }

    /** Menampilkan satu toast notifikasi di pojok kanan atas window aktif. */
    public void showToast(String title, String message, String style) {
        javafx.scene.Scene scene = sceneSupplier.get();
        if (scene == null || scene.getWindow() == null) {
            return;
        }

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("notification-title");

        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().add("notification-message");
        messageLabel.setWrapText(true);

        Button closeButton = new Button("x");
        closeButton.getStyleClass().add("notification-close-button");

        HBox header = new HBox(10, titleLabel, closeButton);
        header.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        VBox content = new VBox(6, header, messageLabel);
        content.getStyleClass().addAll("notification-toast", "glass-toast", getNotificationStyleClass(style));
        content.getStyleClass().add(currentThemeSupplier.get());
        content.getStylesheets().add(stylesheetUrl);
        content.setPrefWidth(340);

        Popup popup = new Popup();
        popup.setAutoFix(true);
        popup.setAutoHide(true);
        popup.getContent().add(content);
        closeButton.setOnAction(event -> popup.hide());

        Window window = scene.getWindow();
        popup.show(window);
        popup.setX(window.getX() + window.getWidth() - content.getPrefWidth() - 28);
        popup.setY(window.getY() + 76);

        SpringAnimationUtil.createToastEntrance(content, Duration.millis(300)).play();

        PauseTransition delay = new PauseTransition(Duration.seconds(4));
        delay.setOnFinished(event -> {
            SpringAnimationUtil.createToastExit(content, Duration.millis(250)).setOnFinished(e -> popup.hide());
            SpringAnimationUtil.createToastExit(content, Duration.millis(250)).play();
        });
        
        content.setOnMouseEntered(e -> delay.pause());
        content.setOnMouseExited(e -> delay.play());
        
        delay.play();
    }

    private String getNotificationStyleClass(String style) {
        return switch (style) {
            case NOTIFICATION_STYLE_MINIMAL -> "notification-minimal";
            case NOTIFICATION_STYLE_URGENT -> "notification-urgent";
            default -> "notification-accent";
        };
    }

    public ChoiceBox<String> getChoiceBox() {
        return notificationStyleChoiceBox;
    }
}
