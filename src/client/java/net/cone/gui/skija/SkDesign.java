package net.cone.gui.skija;

import net.cone.config.ConeConfig;

public final class SkDesign {
    private SkDesign() {}

    public static int SCRIM           = 0x5C0A0A0A;
    public static final int PANEL     = 0xFF1A1A1A;
    public static final int CARD      = 0xFF1A1A1A;
    public static final int CARD_EDGE = 0xFF242424;
    public static final int INSET     = 0xFF0F0F0F;
    public static final int HOVER     = 0x0DFFFFFF;
    public static final int ROW_ON    = 0x14FFFFFF;

    public static final int GLASS      = 0x14FFFFFF;
    public static final int GLASS_EDGE = 0x24FFFFFF;

    public static final int TEXT  = 0xFFFCFCFC;
    public static final int SUB   = 0xFFA5A5A5;
    public static final int MUTED = 0xFF757575;

    public static final int WHITE  = 0xFFFFFFFF;
    public static final int DANGER = 0xFFE05252;
    public static final int GOOD   = 0xFF5DD674;

    public static int BRAND     = 0xFFFF7A1F;
    public static int BRAND_300 = 0xFFFFB877;

    public static int DEEP        = 0xFF2E1A0D;
    public static int DEEP_EDGE   = 0xFF4A2A14;
    public static int DEEP_TEXT   = 0xFFD8C3B2;
    public static final int DEEP_ROW      = 0x40120A04;
    public static final int DEEP_ROW_EDGE = 0x2EFFFFFF;

    public static boolean FROST    = true;
    public static boolean GLASS_ON = true;
    public static float MENU_SCALE = 1.5f;
    private static int RASTER_CAP = 1600;

    public static int R_PANEL  = 22;
    public static int R_HERO   = 20;
    public static int R_HERO_S = 7;
    public static int R_CARD   = 16;
    public static int R_CARD_S = 6;
    public static int R_ROW    = 8;
    public static final int R_CHIP  = 6;

    public static final int T_TITLE   = 22;
    public static final int T_HEAD    = 16;
    public static final int T_BODY    = 15;
    public static final int T_CAPTION = 13;
    public static final int T_SMALL   = 12;

    public static void refresh(ConeConfig c) {
        double hue = c == null ? -1 : c.skijaAccentHue;
        float sat = c == null ? 0.88f : (float) Math.max(0.3, Math.min(1, c.skijaAccentSat));
        if (hue < 0) {
            BRAND = 0xFFFF7A1F; BRAND_300 = 0xFFFFB877;
            DEEP = 0xFF2E1A0D; DEEP_EDGE = 0xFF4A2A14; DEEP_TEXT = 0xFFD8C3B2;
        } else {
            float h = (float) (hue % 360);
            BRAND     = hsv(h, sat, 1.00f);
            BRAND_300 = hsv(h, sat * 0.60f, 1.00f);
            DEEP      = hsv(h, sat * 0.82f, 0.18f);
            DEEP_EDGE = hsv(h, sat * 0.83f, 0.29f);
            DEEP_TEXT = hsv(h, sat * 0.20f, 0.85f);
        }
        if (c != null && !c.skijaHeroTint) {
            DEEP = INSET; DEEP_EDGE = CARD_EDGE; DEEP_TEXT = SUB;
        }

        FROST = c == null || c.skijaFrost;
        GLASS_ON = c == null || c.skijaGlass;

        double dim = c == null ? 0.36 : Math.max(0, Math.min(0.9, c.skijaDim));
        SCRIM = ((int) Math.round(dim * 255) << 24) | 0x0A0A0A;

        MENU_SCALE = c == null ? 1.5f : (float) Math.max(1, Math.min(2, c.skijaMenuScale));

        String quality = c == null ? "Balanced" : c.skijaRenderCap;
        RASTER_CAP = switch (quality) {
            case "Full" -> 8192;
            case "Fast" -> 1080;
            default -> 1600;
        };

        String corners = c == null ? "Round" : c.skijaCorners;
        switch (corners) {
            case "Sharp" -> { R_PANEL = 8;  R_HERO = 6;  R_HERO_S = 3; R_CARD = 5;  R_CARD_S = 2; R_ROW = 3; }
            case "Soft"  -> { R_PANEL = 15; R_HERO = 13; R_HERO_S = 5; R_CARD = 10; R_CARD_S = 4; R_ROW = 5; }
            default      -> { R_PANEL = 22; R_HERO = 20; R_HERO_S = 7; R_CARD = 16; R_CARD_S = 6; R_ROW = 8; }
        }
    }

    private static int hsv(float h, float s, float v) {
        float c = v * s;
        float x = c * (1 - Math.abs((h / 60f) % 2 - 1));
        float m = v - c;
        float r, g, b;
        if (h < 60)       { r = c; g = x; b = 0; }
        else if (h < 120) { r = x; g = c; b = 0; }
        else if (h < 180) { r = 0; g = c; b = x; }
        else if (h < 240) { r = 0; g = x; b = c; }
        else if (h < 300) { r = x; g = 0; b = c; }
        else              { r = c; g = 0; b = x; }
        return 0xFF000000
                | (Math.round((r + m) * 255) << 16)
                | (Math.round((g + m) * 255) << 8)
                | Math.round((b + m) * 255);
    }

    public static int rasterCap() {
        return RASTER_CAP;
    }

    public static float density(double guiScale) {
        if (guiScale <= 1) return 1f;
        return Math.min(MENU_SCALE, (float) guiScale);
    }
}
