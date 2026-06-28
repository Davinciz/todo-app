package com.todoapp.util;

import javafx.animation.*;
import javafx.scene.Node;
import javafx.util.Duration;

public class SpringAnimationUtil {

    public static class SpringInterpolator extends Interpolator {
        private final double stiffness;
        private final double damping;
        private final double mass;
        private final double criticalDamping;

        public SpringInterpolator() {
            this(200, 20, 1.0);
        }

        public SpringInterpolator(double stiffness, double damping, double mass) {
            this.stiffness = stiffness;
            this.damping = damping;
            this.mass = mass;
            this.criticalDamping = 2 * Math.sqrt(stiffness * mass);
        }

        @Override
        protected double curve(double t) {
            if (t == 0.0 || t == 1.0) {
                return t;
            }

            double dampingRatio = damping / criticalDamping;
            double omega = Math.sqrt(stiffness / mass);

            if (dampingRatio < 1) {
                double dampedFreq = omega * Math.sqrt(1 - dampingRatio * dampingRatio);
                double envelope = Math.exp(-dampingRatio * omega * t);
                double oscillation = Math.cos(dampedFreq * t - Math.atan2(dampedFreq, dampingRatio * omega));
                return 1 - envelope * oscillation;
            } else if (dampingRatio == 1) {
                return 1 - Math.exp(-omega * t) * (1 + omega * t);
            } else {
                double r1 = -omega * (dampingRatio + Math.sqrt(dampingRatio * dampingRatio - 1));
                double r2 = -omega * (dampingRatio - Math.sqrt(dampingRatio * dampingRatio - 1));
                return 1 - (Math.exp(r1 * t) - Math.exp(r2 * t)) / (r1 - r2);
            }
        }
    }

    private static final SpringInterpolator DEFAULT_SPRING = new SpringInterpolator();
    private static final SpringInterpolator BOUNCE_SPRING = new SpringInterpolator(300, 15, 1.0);
    private static final SpringInterpolator SMOOTH_SPRING = new SpringInterpolator(150, 25, 1.0);

    public static ScaleTransition createSpringScale(Node node, double fromX, double fromY, 
                                                     double toX, double toY, Duration duration) {
        ScaleTransition scale = new ScaleTransition(duration, node);
        scale.setFromX(fromX);
        scale.setFromY(fromY);
        scale.setToX(toX);
        scale.setToY(toY);
        scale.setInterpolator(DEFAULT_SPRING);
        return scale;
    }

    public static ScaleTransition createHoverScale(Node node, Duration duration) {
        return createSpringScale(node, 1.0, 1.0, 1.02, 1.02, duration);
    }

    public static ScaleTransition createPressScale(Node node, Duration duration) {
        return createSpringScale(node, 1.0, 1.0, 0.97, 0.97, duration);
    }

    public static TranslateTransition createSpringTranslate(Node node, double fromY, double toY, Duration duration) {
        TranslateTransition translate = new TranslateTransition(duration, node);
        translate.setFromY(fromY);
        translate.setToY(toY);
        translate.setInterpolator(DEFAULT_SPRING);
        return translate;
    }

    public static FadeTransition createFade(Node node, double from, double to, Duration duration) {
        FadeTransition fade = new FadeTransition(duration, node);
        fade.setFromValue(from);
        fade.setToValue(to);
        return fade;
    }

    public static ParallelTransition createModalEntrance(Node node, Duration duration) {
        ScaleTransition scale = createSpringScale(node, 0.92, 0.92, 1.0, 1.0, duration);
        FadeTransition fade = createFade(node, 0.0, 1.0, duration);
        
        ParallelTransition parallel = new ParallelTransition(scale, fade);
        return parallel;
    }

    public static ParallelTransition createModalExit(Node node, Duration duration) {
        ScaleTransition scale = createSpringScale(node, 1.0, 1.0, 0.95, 0.95, duration);
        FadeTransition fade = createFade(node, 1.0, 0.0, duration);
        
        ParallelTransition parallel = new ParallelTransition(scale, fade);
        return parallel;
    }

    public static ParallelTransition createToastEntrance(Node node, Duration duration) {
        TranslateTransition translate = createSpringTranslate(node, 80, 0, duration);
        FadeTransition fade = createFade(node, 0.0, 1.0, duration);
        
        ParallelTransition parallel = new ParallelTransition(translate, fade);
        return parallel;
    }

    public static ParallelTransition createToastExit(Node node, Duration duration) {
        TranslateTransition translate = createSpringTranslate(node, 0, 80, duration);
        FadeTransition fade = createFade(node, 1.0, 0.0, duration);
        
        ParallelTransition parallel = new ParallelTransition(translate, fade);
        return parallel;
    }

    public static void applyHoverAnimation(Node node) {
        node.setOnMouseEntered(e -> {
            ScaleTransition scale = createHoverScale(node, Duration.millis(150));
            scale.play();
        });
        
        node.setOnMouseExited(e -> {
            ScaleTransition scale = createSpringScale(node, 1.02, 1.02, 1.0, 1.0, Duration.millis(150));
            scale.play();
        });
    }

    public static void applyPressAnimation(Node node) {
        node.setOnMousePressed(e -> {
            ScaleTransition scale = createPressScale(node, Duration.millis(100));
            scale.play();
        });
        
        node.setOnMouseReleased(e -> {
            ScaleTransition scale = createSpringScale(node, 0.97, 0.97, 1.0, 1.0, Duration.millis(150));
            scale.play();
        });
    }

    public static Timeline createShimmerEffect(Node node, double width) {
        Timeline timeline = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(node.translateXProperty(), -width)
            ),
            new KeyFrame(Duration.millis(1500),
                new KeyValue(node.translateXProperty(), width)
            )
        );
        timeline.setCycleCount(Timeline.INDEFINITE);
        return timeline;
    }

    public static SpringInterpolator getDefaultSpring() {
        return DEFAULT_SPRING;
    }

    public static SpringInterpolator getBounceSpring() {
        return BOUNCE_SPRING;
    }

    public static SpringInterpolator getSmoothSpring() {
        return SMOOTH_SPRING;
    }
}
