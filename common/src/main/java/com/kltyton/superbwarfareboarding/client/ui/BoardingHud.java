package com.kltyton.superbwarfareboarding.client.ui;

import com.atsuishio.superbwarfare.init.ModKeyMappings;
import com.kltyton.superbwarfareboarding.client.camera.BoardingView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Renders a subtle, localized HUD hint and live hold-progress bar while a boarding
 * or dismounting animation is playing, letting players know they can hold Interact
 * (right-click) or Dismount to skip.
 */
public final class BoardingHud {
    private BoardingHud() {}

    public static void render(GuiGraphics graphics, float partialTick) {
        if (!BoardingView.isPlaying()) return;
        Minecraft game = Minecraft.getInstance();
        if (game.options.hideGui || game.screen != null || game.player == null) return;

        Component useKey = game.options.keyUse.getTranslatedKeyMessage();
        Component dismountKey = ModKeyMappings.DISMOUNT.isUnbound()
                ? game.options.keyShift.getTranslatedKeyMessage()
                : ModKeyMappings.DISMOUNT.getTranslatedKeyMessage();
        Component text = Component.translatable("hud.superbwarfare_boarding.hold_skip", useKey, dismountKey);

        var font = game.font;
        int textW = font.width(text);
        int padX = 10;
        int boxW = Math.max(textW + padX * 2, 128);
        int boxH = 24;
        int x = (graphics.guiWidth() - boxW) / 2;
        int y = graphics.guiHeight() - 68;

        float progress = BoardingView.skipHoldProgress(partialTick);
        boolean holding = progress > 0.01f;

        // Subtle tactical dark pill background + thin border
        graphics.fill(x, y, x + boxW, y + boxH, 0xAA10141A);
        int border = holding ? 0xBBFFD54F : 0x55FFFFFF;
        graphics.fill(x, y, x + boxW, y + 1, border);
        graphics.fill(x, y + boxH - 1, x + boxW, y + boxH, 0x44000000);
        graphics.fill(x, y, x + 1, y + boxH, 0x33FFFFFF);
        graphics.fill(x + boxW - 1, y, x + boxW, y + boxH, 0x33FFFFFF);

        int textColor = holding ? 0xFFFFE57F : 0xFFF2F5F8;
        graphics.drawString(font, text, (graphics.guiWidth() - textW) / 2, y + 5, textColor, true);

        // Live hold-to-skip progress bar directly underneath the prompt text
        int barX = x + 8;
        int barW = boxW - 16;
        int barY = y + boxH - 6;
        int barH = 3;
        graphics.fill(barX, barY, barX + barW, barY + barH, 0x44FFFFFF);
        if (progress > 0.0f) {
            int fillW = Math.max(1, Math.round(barW * progress));
            int fillColor = progress > 0.72f ? 0xFFFFD54F : 0xFF64E6EE;
            graphics.fill(barX, barY, barX + fillW, barY + barH, fillColor);
        }
    }
}
