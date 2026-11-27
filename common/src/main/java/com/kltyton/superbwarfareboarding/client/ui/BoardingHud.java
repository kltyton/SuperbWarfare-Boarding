package com.kltyton.superbwarfareboarding.client.ui;

import com.atsuishio.superbwarfare.init.ModKeyMappings;
import com.kltyton.superbwarfareboarding.client.camera.BoardingView;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Renders a compact, AAA-style circular hold-to-skip prompt in the bottom-right corner
 * during boarding and dismounting cutscenes.
 */
public final class BoardingHud {
    private static final int MARGIN_RIGHT = 14;
    private static final int MARGIN_BOTTOM = 14;
    private static final float OUTER_RADIUS = 9.5f;
    private static final float INNER_RADIUS = 7.4f;
    private static final float BACKDROP_RADIUS = 10.6f;
    private static final int RING_SEGMENTS = 64;
    private static final float FADE_IN_TICKS = 5.0f;

    private BoardingHud() {}

    public static void render(GuiGraphics graphics, float partialTick) {
        if (!BoardingView.isPlaying()) return;
        Minecraft game = Minecraft.getInstance();
        if (game.options.hideGui || game.screen != null || game.player == null) return;

        float fade = Mth.clamp(BoardingView.playingTicks(partialTick) / FADE_IN_TICKS, 0.0f, 1.0f);
        if (fade < 0.02f) return;

        float progress = BoardingView.skipHoldProgress(partialTick);
        boolean holding = progress > 0.005f;

        float cx = graphics.guiWidth() - MARGIN_RIGHT - OUTER_RADIUS;
        float cy = graphics.guiHeight() - MARGIN_BOTTOM - OUTER_RADIUS;

        // 1. Subtle dark circular backing disc + hairline rim
        int backdropAlpha = Math.round((holding ? 0.74f : 0.60f) * fade * 255.0f);
        HudRingCanvas.fillDisc(graphics, cx, cy, BACKDROP_RADIUS, 0.9f, RING_SEGMENTS, withAlpha(0x0B0E14, backdropAlpha));
        int rimAlpha = Math.round((holding ? 0.30f : 0.16f) * fade * 255.0f);
        HudRingCanvas.drawArc(graphics, cx, cy, BACKDROP_RADIUS - 0.55f, BACKDROP_RADIUS,
                0.0f, (float) (Math.PI * 2.0), 0.5f, RING_SEGMENTS, withAlpha(0xFFFFFF, rimAlpha));

        // 2. Muted full-circle track
        int trackAlpha = Math.round((holding ? 0.32f : 0.24f) * fade * 255.0f);
        HudRingCanvas.drawArc(graphics, cx, cy, INNER_RADIUS + 0.15f, OUTER_RADIUS - 0.15f,
                0.0f, (float) (Math.PI * 2.0), 0.6f, RING_SEGMENTS, withAlpha(0xFFFFFF, trackAlpha));

        // 3. Smooth clockwise hold-progress radial fill (starts at 12 o'clock)
        if (holding) {
            float startRad = (float) (-Math.PI * 0.5);
            float sweepRad = (float) (Math.PI * 2.0 * progress);
            int rgb = progress > 0.72f
                    ? lerpRgb(0xFFFFFF, 0xFFE082, (progress - 0.72f) / 0.28f)
                    : 0xFFFFFF;
            int glowAlpha = Math.round(0.28f * fade * 255.0f);
            HudRingCanvas.drawArc(graphics, cx, cy, INNER_RADIUS - 0.6f, OUTER_RADIUS + 0.8f,
                    startRad, sweepRad, 0.9f, RING_SEGMENTS, withAlpha(rgb, glowAlpha));
            int arcAlpha = Math.round(0.96f * fade * 255.0f);
            HudRingCanvas.drawArc(graphics, cx, cy, INNER_RADIUS - 0.1f, OUTER_RADIUS + 0.2f,
                    startRad, sweepRad, 0.6f, RING_SEGMENTS, withAlpha(rgb, arcAlpha));
        }

        // 4. Compact centered double-chevron skip glyph inside the ring
        int iconRgb = holding && progress > 0.72f ? 0xFFF3C4 : 0xF2F5F8;
        int iconAlpha = Math.round((holding ? 1.0f : 0.88f) * fade * 255.0f);
        int iconColor = withAlpha(iconRgb, iconAlpha);
        float triHalfH = 2.6f;
        float triW = 2.9f;
        float leftTipX = cx - 0.1f;
        HudRingCanvas.fillTriangle(graphics,
                leftTipX - triW, cy - triHalfH,
                leftTipX - triW, cy + triHalfH,
                leftTipX, cy,
                iconColor);
        float rightTipX = cx + 3.1f;
        HudRingCanvas.fillTriangle(graphics,
                rightTipX - triW, cy - triHalfH,
                rightTipX - triW, cy + triHalfH,
                rightTipX, cy,
                iconColor);

        // 5. Minimal shadowed key + Skip label neatly aligned to the left of the ring
        int textAlpha = Math.round(fade * 255.0f);
        if (textAlpha < 8) return;

        Font font = game.font;
        String keyLabel = activeKeyLabel(game);
        String actionLabel = Component.translatable("hud.superbwarfare_boarding.skip").getString();

        int keyRgb = holding ? 0xFFE082 : 0xCFD8E3;
        int actionRgb = holding ? 0xFFFFFF : 0xF5F7FA;

        int keyW = font.width(keyLabel);
        int actionW = font.width(actionLabel);
        int innerGap = 4;
        int ringGap = 6;
        int rightEdge = Math.round(cx - OUTER_RADIUS) - ringGap;
        int actionX = rightEdge - actionW;
        int keyX = actionX - innerGap - keyW;
        int textY = Math.round(cy - font.lineHeight * 0.5f) + 1;

        graphics.drawString(font, keyLabel, keyX, textY, withAlpha(keyRgb, textAlpha), true);
        graphics.drawString(font, actionLabel, actionX, textY, withAlpha(actionRgb, textAlpha), true);
    }

    private static String activeKeyLabel(Minecraft game) {
        KeyMapping mapping = BoardingView.isHoldingDismount()
                ? (ModKeyMappings.DISMOUNT.isUnbound() ? game.options.keyShift : ModKeyMappings.DISMOUNT)
                : game.options.keyUse;
        return shortenKeyName(mapping.getTranslatedKeyMessage().getString());
    }

    private static String shortenKeyName(String raw) {
        if (raw == null || raw.isBlank()) return "";
        String trimmed = raw.trim();
        return switch (trimmed) {
            case "Right Button" -> "RMB";
            case "Left Button" -> "LMB";
            case "Middle Button" -> "MMB";
            case "Left Alt" -> "LAlt";
            case "Right Alt" -> "RAlt";
            case "Left Shift" -> "LShift";
            case "Right Shift" -> "RShift";
            case "Left Control" -> "LCtrl";
            case "Right Control" -> "RCtrl";
            default -> trimmed;
        };
    }

    private static int withAlpha(int rgb, int alpha) {
        return (Mth.clamp(alpha, 0, 255) << 24) | (rgb & 0x00FFFFFF);
    }

    private static int lerpRgb(int from, int to, float t) {
        float clamped = Mth.clamp(t, 0.0f, 1.0f);
        int r = Math.round(Mth.lerp(clamped, (from >>> 16) & 0xFF, (to >>> 16) & 0xFF));
        int g = Math.round(Mth.lerp(clamped, (from >>> 8) & 0xFF, (to >>> 8) & 0xFF));
        int b = Math.round(Mth.lerp(clamped, from & 0xFF, to & 0xFF));
        return (r << 16) | (g << 8) | b;
    }
}
