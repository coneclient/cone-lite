package net.cone.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public final class Ui {
    private static long last = System.nanoTime();
    private static float dt = 0f;

    private Ui() {}

    public static void text(GuiGraphicsExtractor g, Font font, String s, int x, int y, int color) {
        g.text(font, Component.literal(s), x, y, color, true);
    }

    public static int width(Font font, String s) {
        return font.width(s);
    }

    public static String ellipsize(Font font, String s, int maxW) {
        if (s == null || s.isEmpty() || width(font, s) <= maxW) return s;
        int ellW = width(font, "…");
        int end = s.length();
        while (end > 0 && width(font, s.substring(0, end)) + ellW > maxW) end--;
        return end <= 0 ? "…" : s.substring(0, end) + "…";
    }

    private static float motion = 1f;

    public static void beginFrame() {
        long now = System.nanoTime();
        dt = Math.min(0.05f, (now - last) / 1_000_000_000f);
        last = now;
        motion = switch (net.cone.config.ConfigManager.get().uiMotion) {
            case "Off" -> 0f;
            case "Reduced" -> 3f;
            default -> 1f;
        };
    }

    public static float dt() {
        return dt;
    }

    public static boolean motionOff() {
        return motion <= 0f;
    }

    public static float approach(float v, float target, float rate) {
        if (motion <= 0f) return target;
        float t = 1f - (float) Math.exp(-rate * motion * dt);
        return v + (target - v) * t;
    }

    public static float easeOutCubic(float t) {
        float u = 1f - clamp01(t);
        return 1f - u * u * u;
    }

    public static float clamp01(float t) {
        return t < 0 ? 0 : (t > 1 ? 1 : t);
    }

    public static int lerpColor(int a, int b, float t) {
        t = clamp01(t);
        int aa = (a >>> 24) & 0xFF, ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int ba = (b >>> 24) & 0xFF, br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        int oa = (int) (aa + (ba - aa) * t);
        int or = (int) (ar + (br - ar) * t);
        int og = (int) (ag + (bg - ag) * t);
        int ob = (int) (ab + (bb - ab) * t);
        return (oa << 24) | (or << 16) | (og << 8) | ob;
    }

    public static int alpha(int color, float mult) {
        int a = (int) (((color >>> 24) & 0xFF) * clamp01(mult));
        return (a << 24) | (color & 0xFFFFFF);
    }

    public static void roundRect(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, int color) {
        if (w <= 0 || h <= 0) return;
        g.fill(x, y, x + w, y + h, color);
    }

    public static void roundRectGradient(GuiGraphicsExtractor g, int x, int y, int w, int h, int r,
                                         int top, int bottom) {
        if (w <= 0 || h <= 0) return;
        for (int dy = 0; dy < h; dy++) {
            int col = lerpColor(top, bottom, dy / (float) Math.max(1, h - 1));
            g.fill(x, y + dy, x + w, y + dy + 1, col);
        }
    }

    public static void tooltip(GuiGraphicsExtractor g, Font font, java.util.List<String> lines,
                               int mouseX, int mouseY, int screenW, int screenH) {
        if (lines.isEmpty()) return;
        int tw = 0;
        for (String l : lines) tw = Math.max(tw, width(font, l));
        int w = tw + 12, h = lines.size() * 10 + 8;
        int x = mouseX + 10, y = mouseY + 12;
        if (x + w > screenW - 2) x = mouseX - w - 6;
        if (y + h > screenH - 2) y = mouseY - h - 6;
        x = Math.max(2, x); y = Math.max(2, y);
        shadow(g, x, y, w, h, 4);
        roundRect(g, x, y, w, h, 4, net.cone.gui.Theme.PANEL_2);
        g.fill(x, y, x + w, y + 1, net.cone.gui.Theme.LINE);
        g.fill(x, y + h - 1, x + w, y + h, net.cone.gui.Theme.LINE);
        g.fill(x, y, x + 1, y + h, net.cone.gui.Theme.LINE);
        g.fill(x + w - 1, y, x + w, y + h, net.cone.gui.Theme.LINE);
        int ty = y + 4;
        for (String l : lines) {
            text(g, font, l, x + 6, ty, net.cone.gui.Theme.TEXT);
            ty += 10;
        }
    }

    public static java.util.List<String> wrap(Font font, String s, int maxW) {
        java.util.List<String> out = new java.util.ArrayList<>();
        if (s == null || s.isEmpty()) return out;
        StringBuilder line = new StringBuilder();
        for (String word : s.split(" ")) {
            String probe = line.isEmpty() ? word : line + " " + word;
            if (width(font, probe) > maxW && !line.isEmpty()) {
                out.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(probe);
            }
        }
        if (!line.isEmpty()) out.add(line.toString());
        return out;
    }

    public static void shadow(GuiGraphicsExtractor g, int x, int y, int w, int h, int r) {
        for (int i = 6; i >= 1; i--) {
            int a = 0x08 * i;
            roundRect(g, x - i, y - i + 2, w + i * 2, h + i * 2, r + i, (a << 24));
        }
    }
}
