package net.cone.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.cone.config.ConeConfig;
import net.cone.config.ConfigManager;

public final class Theme {
    public static int SCRIM      = 0xC8090A0C;
    public static int PANEL      = 0xFF16171B;
    public static int PANEL_2    = 0xFF1D1F24;
    public static int SIDEBAR    = 0xFF101115;
    public static int ROW        = 0xFF202227;
    public static int ROW_HOVER  = 0xFF292C32;
    public static int TRACK      = 0xFF2C2F35;
    public static int ACCENT     = 0xFF3DDCCB;
    public static int ACCENT_2   = 0xFF23B7A8;
    public static int ACCENT_DIM = 0xFF17403C;
    public static int TEXT       = 0xFFF2F3F5;
    public static int SUBTEXT    = 0xFF9AA1AA;
    public static int MUTED      = 0xFF6A7079;
    public static int DANGER     = 0xFFF2635F;
    public static int DANGER_2   = 0xFFC94743;
    public static int DANGER_DIM = 0xFF44201F;
    public static int GOOD       = 0xFF5FD37F;
    public static int LINE       = 0xFF26282E;
    public static int SHADOW     = 0x60000000;
    public static int INK        = 0xFF08201E;

    static {
        apply(null);
    }

    public static void apply(ConeConfig c) {
        String preset = c == null ? "Abyss" : c.uiTheme;
        boolean hc = c != null && c.uiHighContrast;
        double hue = c == null ? -1 : c.uiAccentHue;
        float scrimA = c == null ? 0.78f : (float) Math.max(0, Math.min(1, c.uiScrim));

        switch (preset) {
            case "Void" -> {
                PANEL = 0xFF0A0A0C; PANEL_2 = 0xFF131316; SIDEBAR = 0xFF060607;
                ROW = 0xFF151518; ROW_HOVER = 0xFF1E1E23; TRACK = 0xFF232328;
                LINE = 0xFF1C1C21;
            }
            case "Slate" -> {
                PANEL = 0xFF171B23; PANEL_2 = 0xFF1E232d; SIDEBAR = 0xFF11141B;
                ROW = 0xFF212734; ROW_HOVER = 0xFF2A3140; TRACK = 0xFF2E3545;
                LINE = 0xFF272E3C;
            }
            default -> {
                PANEL = 0xFF16171B; PANEL_2 = 0xFF1D1F24; SIDEBAR = 0xFF101115;
                ROW = 0xFF202227; ROW_HOVER = 0xFF292C32; TRACK = 0xFF2C2F35;
                LINE = 0xFF26282E;
            }
        }

        if (hue >= 0) {
            float h = (float) (hue % 360);
            ACCENT     = hsv(h, 0.62f, 0.88f);
            ACCENT_2   = hsv(h, 0.68f, 0.70f);
            ACCENT_DIM = hsv(h, 0.55f, 0.26f);
            INK        = hsv(h, 0.60f, 0.13f);
        } else {
            ACCENT = 0xFF3DDCCB; ACCENT_2 = 0xFF23B7A8;
            ACCENT_DIM = 0xFF17403C; INK = 0xFF08201E;
        }

        if (hc) {
            TEXT = 0xFFFFFFFF; SUBTEXT = 0xFFC7CDD6; MUTED = 0xFF99A0AB;
            LINE = 0xFF3A3E48;
        } else {
            TEXT = 0xFFF2F3F5; SUBTEXT = 0xFF9AA1AA; MUTED = 0xFF6A7079;
        }

        SCRIM = ((int) (scrimA * 255) << 24) | 0x090A0C;
    }

    public static void refresh() {
        apply(ConfigManager.get());
    }

    public static int categoryColor(String name) {
        if (name == null || name.isEmpty() || name.equals("Default")) return SUBTEXT;
        int h = Math.floorMod(name.hashCode(), 360);
        return hsv(h, 0.45f, 0.95f);
    }

    public static int hsv(float h, float s, float v) {
        float c = v * s, x = c * (1 - Math.abs((h / 60f) % 2 - 1)), m = v - c;
        float r, g, b;
        if (h < 60)      { r = c; g = x; b = 0; }
        else if (h < 120){ r = x; g = c; b = 0; }
        else if (h < 180){ r = 0; g = c; b = x; }
        else if (h < 240){ r = 0; g = x; b = c; }
        else if (h < 300){ r = x; g = 0; b = c; }
        else             { r = c; g = 0; b = x; }
        int ri = Math.round((r + m) * 255), gi = Math.round((g + m) * 255), bi = Math.round((b + m) * 255);
        return 0xFF000000 | (ri << 16) | (gi << 8) | bi;
    }

    private Theme() {}

    public static void rect(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + h, color);
    }

    public static void border(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        rect(g, x, y, w, 1, color);
        rect(g, x, y + h - 1, w, 1, color);
        rect(g, x, y, 1, h, color);
        rect(g, x + w - 1, y, 1, h, color);
    }
}
