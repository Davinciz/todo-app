package com.todoapp.controller;

import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;

import java.util.prefs.Preferences;

public class ThemeManager {

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

        this.currentTheme = THEME_LIGHT;
        applyTheme(currentTheme);

        toggleButton.setVisible(false);
        toggleButton.setManaged(false);
    }

    public void toggleTheme() {
    }

    public void applyTheme(String theme) {
        currentTheme = THEME_LIGHT;
        rootPane.getStyleClass().removeAll("dark-theme", THEME_LIGHT);
        rootPane.getStyleClass().add(THEME_LIGHT);
        preferences.put(PREF_THEME, currentTheme);
    }

    public String getCurrentTheme() {
        return currentTheme;
    }

    public static String themeLight() {
        return THEME_LIGHT;
    }
}
