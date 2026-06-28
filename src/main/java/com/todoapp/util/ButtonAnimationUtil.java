package com.todoapp.util;

import javafx.animation.ScaleTransition;
import javafx.scene.Node;
import javafx.util.Duration;

public class ButtonAnimationUtil {

    private static final Duration ANIM_DURATION = Duration.millis(150);
    private static final double SCALE_HOVER = 1.02;
    private static final double SCALE_PRESSED = 0.97;
    private static final double SCALE_NORMAL = 1.0;

    public static void addHoverScale(Node node) {
        addHoverScale(node, SCALE_HOVER);
    }

    public static void addHoverScale(Node node, double scaleFactor) {
        node.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(ANIM_DURATION, node);
            st.setToX(scaleFactor);
            st.setToY(scaleFactor);
            st.setInterpolator(SpringAnimationUtil.getDefaultSpring());
            st.play();
        });

        node.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(ANIM_DURATION, node);
            st.setToX(SCALE_NORMAL);
            st.setToY(SCALE_NORMAL);
            st.setInterpolator(SpringAnimationUtil.getDefaultSpring());
            st.play();
        });
    }

    public static void addPressedScale(Node node) {
        node.setOnMousePressed(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(100), node);
            st.setToX(SCALE_PRESSED);
            st.setToY(SCALE_PRESSED);
            st.setInterpolator(SpringAnimationUtil.getDefaultSpring());
            st.play();
        });

        node.setOnMouseReleased(e -> {
            ScaleTransition st = new ScaleTransition(ANIM_DURATION, node);
            st.setToX(SCALE_NORMAL);
            st.setToY(SCALE_NORMAL);
            st.setInterpolator(SpringAnimationUtil.getDefaultSpring());
            st.play();
        });
    }

    /**
     * Menambahkan animasi hover + pressed sekaligus.
     */
    public static void addHoverAndPressedScale(Node node) {
        addHoverAndPressedScale(node, SCALE_HOVER);
    }

    /**
     * Menambahkan animasi hover + pressed dengan custom scale factor.
     */
    public static void addHoverAndPressedScale(Node node, double hoverScaleFactor) {
        addHoverScale(node, hoverScaleFactor);
        addPressedScale(node);
    }
}