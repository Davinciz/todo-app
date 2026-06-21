package com.todoapp.controller;

import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;

import java.util.prefs.Preferences;

/**
 * Mengelola pergantian tema (dark/light) untuk aplikasi,
 * termasuk menyimpan dan memuat preferensi tema pengguna.
 */
public class ThemeManager {

    private static final String THEME_DARK = "theme-dark";
    private static final String THEME_LIGHT = "theme-light";
    private static final String PREF_THEME = "theme";

    private final BorderPane rootPane;
    private final Button toggleButton;
    private final Preferences preferences;

    private String currentTheme;

    public ThemeManager(BorderPane rootPane, Button toggleButton, Preferences preferences) {
        this.rootPane = rootPane;
        this.toggleButton = toggleButton;
        this.preferences = preferences;

        this.currentTheme = preferences.get(PREF_THEME, THEME_DARK);
        applyTheme(currentTheme);

        toggleButton.setOnAction(e -> toggleTheme());
    }

    /** Membalik tema saat ini (dark <-> light) dan menyimpannya. */
    public void toggleTheme() {
        applyTheme(THEME_DARK.equals(currentTheme) ? THEME_LIGHT : THEME_DARK);
    }

    /** Menerapkan tema tertentu ke root pane dan tombol toggle. */
    public void applyTheme(String theme) {
        currentTheme = THEME_LIGHT.equals(theme) ? THEME_LIGHT : THEME_DARK;

        rootPane.getStyleClass().removeAll(THEME_DARK, THEME_LIGHT);
        rootPane.getStyleClass().add(currentTheme);

        toggleButton.setText(THEME_DARK.equals(currentTheme) ? "☀" : "☾");
        toggleButton.setTooltip(new Tooltip(
                THEME_DARK.equals(currentTheme) ? "Ganti ke tema terang" : "Ganti ke tema gelap"
        ));

        preferences.put(PREF_THEME, currentTheme);
    }

    /** @return kelas CSS tema yang sedang aktif ("theme-dark" atau "theme-light") */
    public String getCurrentTheme() {
        return currentTheme;
    }

    public static String themeDark() {
        return THEME_DARK;
    }

    public static String themeLight() {
        return THEME_LIGHT;
    }
}
