package com.todoapp.util;

import javafx.scene.effect.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.layout.Region;
import javafx.scene.image.WritableImage;
import javafx.scene.image.ImageView;
import javafx.scene.SnapshotParameters;

public class GlassEffectBuilder {

    public static DropShadow createDropShadow(double radius, double offsetX, double offsetY, Color color) {
        DropShadow shadow = new DropShadow();
        shadow.setRadius(radius);
        shadow.setOffsetX(offsetX);
        shadow.setOffsetY(offsetY);
        shadow.setColor(color);
        shadow.setSpread(0.0);
        return shadow;
    }

    public static DropShadow createShadowSm() {
        return createDropShadow(8, 0, 2, Color.rgb(0, 0, 0, 0.08));
    }

    public static DropShadow createShadowMd() {
        return createDropShadow(16, 0, 3, Color.rgb(0, 0, 0, 0.12));
    }

    public static DropShadow createShadowLg() {
        return createDropShadow(20, 0, 4, Color.rgb(0, 0, 0, 0.15));
    }

    public static DropShadow createShadowXl() {
        return createDropShadow(32, 0, 6, Color.rgb(0, 0, 0, 0.20));
    }

    public static DropShadow createShadow2xl() {
        return createDropShadow(40, 0, 8, Color.rgb(0, 0, 0, 0.25));
    }

    public static DropShadow createGlowGreen() {
        return createDropShadow(8, 0, 0, Color.rgb(0, 168, 120, 0.30));
    }

    public static DropShadow createGlowGreenStrong() {
        return createDropShadow(12, 0, 0, Color.rgb(0, 168, 120, 0.45));
    }

    public static DropShadow createGlowSuccess() {
        return createDropShadow(8, 0, 0, Color.rgb(34, 197, 94, 0.40));
    }

    public static DropShadow createGlowError() {
        return createDropShadow(8, 0, 0, Color.rgb(239, 68, 68, 0.40));
    }

    public static DropShadow createGlowWarning() {
        return createDropShadow(8, 0, 0, Color.rgb(245, 158, 11, 0.40));
    }

    public static GaussianBlur createBlurLight() {
        return new GaussianBlur(6);
    }

    public static GaussianBlur createBlurMedium() {
        return new GaussianBlur(10);
    }

    public static GaussianBlur createBlurHeavy() {
        return new GaussianBlur(16);
    }

    public static GaussianBlur createBlur(double radius) {
        return new GaussianBlur(radius);
    }

    public static InnerShadow createInnerGlow(Color color, double radius) {
        InnerShadow innerShadow = new InnerShadow();
        innerShadow.setColor(color);
        innerShadow.setRadius(radius);
        innerShadow.setOffsetX(0);
        innerShadow.setOffsetY(0);
        innerShadow.setChoke(0.0);
        return innerShadow;
    }

    public static Rectangle createInnerHighlight(double width, double height, double radius) {
        Rectangle highlight = new Rectangle(width, height);
        highlight.setFill(Color.rgb(255, 255, 255, 0.08));
        highlight.setArcWidth(radius);
        highlight.setArcHeight(radius);
        highlight.setMouseTransparent(true);
        return highlight;
    }

    public static ImageView createBlurredBackground(Region sourceNode, double blurRadius) {
        try {
            SnapshotParameters params = new SnapshotParameters();
            params.setFill(Color.TRANSPARENT);
            
            WritableImage snapshot = sourceNode.snapshot(params, null);
            
            ImageView blurredView = new ImageView(snapshot);
            blurredView.setEffect(new GaussianBlur(blurRadius));
            blurredView.setCache(true);
            blurredView.setCacheHint(javafx.scene.CacheHint.SPEED);
            
            return blurredView;
        } catch (Exception e) {
            return null;
        }
    }

    public static void applyGlassEffect(Region node, boolean withShadow) {
        if (withShadow) {
            node.setEffect(createShadowLg());
        }
        node.setCache(true);
        node.setCacheHint(javafx.scene.CacheHint.SPEED);
    }

    public static void applyHoverEffect(Region node, DropShadow defaultShadow, DropShadow hoverShadow) {
        node.setOnMouseEntered(e -> node.setEffect(hoverShadow));
        node.setOnMouseExited(e -> node.setEffect(defaultShadow));
    }

    public static Blend createGlassBlend() {
        Blend blend = new Blend();
        blend.setMode(BlendMode.OVERLAY);
        return blend;
    }

    public static Color parseRgba(String rgba) {
        try {
            String[] parts = rgba.replace("rgba(", "").replace(")", "").split(",");
            int r = Integer.parseInt(parts[0].trim());
            int g = Integer.parseInt(parts[1].trim());
            int b = Integer.parseInt(parts[2].trim());
            double a = Double.parseDouble(parts[3].trim());
            return Color.rgb(r, g, b, a);
        } catch (Exception e) {
            return Color.TRANSPARENT;
        }
    }

    public static class GlassColors {
        public static final Color GLASS_WHITE = Color.rgb(255, 255, 255, 0.18);
        public static final Color GLASS_WHITE_STRONG = Color.rgb(255, 255, 255, 0.32);
        public static final Color GLASS_TINT_GREEN = Color.rgb(0, 168, 120, 0.12);
        public static final Color GLASS_TINT_GREEN_STR = Color.rgb(0, 168, 120, 0.22);
        public static final Color GLASS_DARK = Color.rgb(13, 17, 23, 0.65);
        public static final Color GLASS_BORDER = Color.rgb(255, 255, 255, 0.28);
        public static final Color GLASS_SHADOW = Color.rgb(0, 0, 0, 0.18);
        public static final Color GLASS_GLOW_GREEN = Color.rgb(0, 168, 120, 0.45);
        
        public static final Color BRAND_TEAL = Color.rgb(0, 168, 120);
        public static final Color SUCCESS = Color.rgb(34, 197, 94);
        public static final Color WARNING = Color.rgb(245, 158, 11);
        public static final Color ERROR = Color.rgb(239, 68, 68);
        public static final Color INFO = Color.rgb(59, 130, 246);
    }
}
