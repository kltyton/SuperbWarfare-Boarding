package com.kltyton.superbwarfareboarding.client.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;

/** Anti-aliased 2D vector disc, radial arc, and glyph renderer for Minecraft 1.20.1 HUDs. */
public final class HudRingCanvas {
    private HudRingCanvas() {}

    public static void fillDisc(GuiGraphics graphics, float cx, float cy, float radius, float feather,
                                int segments, int argb) {
        int a = (argb >>> 24) & 0xFF;
        if (a <= 0 || radius <= 0.0f || segments < 6) return;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;

        float innerR = Math.max(0.0f, radius - feather * 0.5f);
        float outerR = radius + feather * 0.5f;

        graphics.flush();
        beginDraw();
        Matrix4f matrix = graphics.pose().last().pose();
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i < segments; i++) {
            double a0 = (Math.PI * 2.0 * i) / segments;
            double a1 = (Math.PI * 2.0 * (i + 1)) / segments;
            float cos0 = (float) Math.cos(a0);
            float sin0 = (float) Math.sin(a0);
            float cos1 = (float) Math.cos(a1);
            float sin1 = (float) Math.sin(a1);

            float ix0 = cx + cos0 * innerR;
            float iy0 = cy + sin0 * innerR;
            float ix1 = cx + cos1 * innerR;
            float iy1 = cy + sin1 * innerR;

            if (innerR > 0.0f) {
                emitTriangle(builder, matrix, cx, cy, r, g, b, a, ix0, iy0, r, g, b, a, ix1, iy1, r, g, b, a);
            }
            if (outerR > innerR) {
                float ox0 = cx + cos0 * outerR;
                float oy0 = cy + sin0 * outerR;
                float ox1 = cx + cos1 * outerR;
                float oy1 = cy + sin1 * outerR;
                emitQuad(builder, matrix,
                        ix0, iy0, r, g, b, a,
                        ix1, iy1, r, g, b, a,
                        ox1, oy1, r, g, b, 0,
                        ox0, oy0, r, g, b, 0);
            }
        }
        BufferUploader.drawWithShader(builder.end());
        endDraw();
    }

    public static void drawArc(GuiGraphics graphics, float cx, float cy, float innerR, float outerR,
                               float startRad, float sweepRad, float feather, int segments, int argb) {
        int a = (argb >>> 24) & 0xFF;
        if (a <= 0 || outerR <= innerR || Math.abs(sweepRad) < 1.0e-4f || segments < 6) return;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;

        float halfFeather = Math.min(feather * 0.5f, (outerR - innerR) * 0.35f);
        float r0 = Math.max(0.0f, innerR - halfFeather);
        float r1 = innerR + halfFeather;
        float r2 = Math.max(r1, outerR - halfFeather);
        float r3 = outerR + halfFeather;
        int steps = Math.max(6, Math.round(segments * (Math.abs(sweepRad) / (float) (Math.PI * 2.0))));

        graphics.flush();
        beginDraw();
        Matrix4f matrix = graphics.pose().last().pose();
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i < steps; i++) {
            float t0 = startRad + (sweepRad * i) / steps;
            float t1 = startRad + (sweepRad * (i + 1)) / steps;
            float cos0 = (float) Math.cos(t0);
            float sin0 = (float) Math.sin(t0);
            float cos1 = (float) Math.cos(t1);
            float sin1 = (float) Math.sin(t1);

            if (r1 > r0) {
                emitQuad(builder, matrix,
                        cx + cos0 * r0, cy + sin0 * r0, r, g, b, 0,
                        cx + cos1 * r0, cy + sin1 * r0, r, g, b, 0,
                        cx + cos1 * r1, cy + sin1 * r1, r, g, b, a,
                        cx + cos0 * r1, cy + sin0 * r1, r, g, b, a);
            }
            if (r2 > r1) {
                emitQuad(builder, matrix,
                        cx + cos0 * r1, cy + sin0 * r1, r, g, b, a,
                        cx + cos1 * r1, cy + sin1 * r1, r, g, b, a,
                        cx + cos1 * r2, cy + sin1 * r2, r, g, b, a,
                        cx + cos0 * r2, cy + sin0 * r2, r, g, b, a);
            }
            if (r3 > r2) {
                emitQuad(builder, matrix,
                        cx + cos0 * r2, cy + sin0 * r2, r, g, b, a,
                        cx + cos1 * r2, cy + sin1 * r2, r, g, b, a,
                        cx + cos1 * r3, cy + sin1 * r3, r, g, b, 0,
                        cx + cos0 * r3, cy + sin0 * r3, r, g, b, 0);
            }
        }
        BufferUploader.drawWithShader(builder.end());
        endDraw();
    }

    public static void fillTriangle(GuiGraphics graphics, float x1, float y1, float x2, float y2,
                                    float x3, float y3, int argb) {
        int a = (argb >>> 24) & 0xFF;
        if (a <= 0) return;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;

        graphics.flush();
        beginDraw();
        Matrix4f matrix = graphics.pose().last().pose();
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        emitTriangle(builder, matrix, x1, y1, r, g, b, a, x2, y2, r, g, b, a, x3, y3, r, g, b, a);
        BufferUploader.drawWithShader(builder.end());
        endDraw();
    }

    private static void beginDraw() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private static void endDraw() {
        RenderSystem.enableCull();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private static void emitQuad(BufferBuilder builder, Matrix4f matrix,
                                 float x0, float y0, int r0, int g0, int b0, int a0,
                                 float x1, float y1, int r1, int g1, int b1, int a1,
                                 float x2, float y2, int r2, int g2, int b2, int a2,
                                 float x3, float y3, int r3, int g3, int b3, int a3) {
        emitTriangle(builder, matrix, x0, y0, r0, g0, b0, a0, x1, y1, r1, g1, b1, a1, x2, y2, r2, g2, b2, a2);
        emitTriangle(builder, matrix, x0, y0, r0, g0, b0, a0, x2, y2, r2, g2, b2, a2, x3, y3, r3, g3, b3, a3);
    }

    private static void emitTriangle(BufferBuilder builder, Matrix4f matrix,
                                     float x0, float y0, int r0, int g0, int b0, int a0,
                                     float x1, float y1, int r1, int g1, int b1, int a1,
                                     float x2, float y2, int r2, int g2, int b2, int a2) {
        builder.vertex(matrix, x0, y0, 0.0f).color(r0, g0, b0, a0).endVertex();
        builder.vertex(matrix, x1, y1, 0.0f).color(r1, g1, b1, a1).endVertex();
        builder.vertex(matrix, x2, y2, 0.0f).color(r2, g2, b2, a2).endVertex();
    }
}
