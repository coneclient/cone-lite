package net.cone.gui.hud;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import net.cone.ConeClient;
import net.cone.config.ConfigManager;
import net.cone.config.ConeConfig;
import net.cone.core.ConeCore;
import net.cone.core.world.PlayerState;
import net.cone.gui.Theme;
import net.cone.gui.Ui;
import net.cone.skyblock.SkyblockData;

import java.util.ArrayList;
import java.util.List;

public final class ConeHud {
    private static final int PAD = 8;
    private static final int LH = 11;
    private static final int HEADER_H = 19;
    private static final int MIN_W = 156;
    private static final int MAX_W = 240;

    private static final int AMBER    = 0xFFE0A44B;

    private ConeHud() {}

    public static void register() {
        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(ConeClient.MOD_ID, "status_hud"),
                ConeHud::render);
    }

    private static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        ConeConfig c = ConfigManager.get();
        if (mc.player == null || !c.hudEnabled) return;
        drawScaled(g, mc.font, c.hudX, c.hudY, false);
    }

    private static float scale() {
        return Math.max(0.5f, Math.min(2.5f, ConfigManager.get().hudScale));
    }

    public static int[] drawScaled(GuiGraphicsExtractor g, Font font, int x, int y, boolean preview) {
        float s = scale();
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(s, s);
        int[] wh = draw(g, font, 0, 0, preview);
        g.pose().popMatrix();
        return new int[]{Math.round(wh[0] * s), Math.round(wh[1] * s)};
    }

    private static long lastNs = System.nanoTime();

    private static void tickClock() {
        lastNs = System.nanoTime();
    }

    private static float pulse() {
        return 0.5f + 0.5f * (float) Math.sin(lastNs / 1_000_000_000d * Math.PI * 2 * 1.6);
    }

    private static void hFade(GuiGraphicsExtractor g, int x, int y, int w, int h, int from, int to) {
        if (w <= 0 || h <= 0) return;
        for (int i = 0; i < w; i++) {
            g.fill(x + i, y, x + i + 1, y + h, Ui.lerpColor(from, to, i / (float) Math.max(1, w - 1)));
        }
    }

    private interface Row {
        int minWidth(Font f);

        void draw(GuiGraphicsExtractor g, Font f, int x, int y, int w);
    }

    private record TextRow(String text, int color) implements Row {
        public int minWidth(Font f) { return Math.min(Ui.width(f, text), MAX_W); }
        public void draw(GuiGraphicsExtractor g, Font f, int x, int y, int w) {
            Ui.text(g, f, Ui.ellipsize(f, text, w), x, y, color);
        }
    }

    private record KVRow(String label, String value, int valueColor) implements Row {
        public int minWidth(Font f) { return Ui.width(f, label) + 12 + Ui.width(f, value); }
        public void draw(GuiGraphicsExtractor g, Font f, int x, int y, int w) {
            int vw = Ui.width(f, value);
            Ui.text(g, f, Ui.ellipsize(f, label, w - vw - 8), x, y, Theme.SUBTEXT);
            Ui.text(g, f, value, x + w - vw, y, valueColor);
        }
    }

    private record Section(String title, int tint, List<Row> rows) {
        int height() { return (title == null ? 0 : LH) + rows.size() * LH; }
    }

    public static int[] draw(GuiGraphicsExtractor g, Font font, int x, int y, boolean preview) {
        tickClock();
        ConeConfig c = ConfigManager.get();
        boolean expanded = c.hudExpanded;
        List<Section> sections = new ArrayList<>();

        int accent = Theme.ACCENT;
        String state = "Watching";
        boolean live = false;

        var sess = ConeCore.session();
        if (expanded && sess.active()) {
            var s = sess.current();
            sections.add(new Section("Session", Theme.ACCENT, List.of(
                    new KVRow("⏱ " + s.runtime(),
                            s.failsafes == 0 ? "clean" : s.failsafes + " failsafe(s)", Theme.TEXT))));
        } else if (preview) {
            sections.add(new Section("Session", Theme.ACCENT, List.of(
                    new KVRow("⏱ 00:12:30", "clean", Theme.TEXT))));
        }

        var sky = ConeCore.skyblock();

        if (sky.onSkyblock()) {
            String where = sky.region().label + (sky.subLocation().isBlank() ? "" : " · " + sky.subLocation());
            sections.add(new Section(null, accent,
                    List.of(new TextRow("◆ " + where, Theme.SUBTEXT))));
        }

        if (expanded && c.hudShowPos) {
            Vec3 p = PlayerState.pos();
            sections.add(new Section(null, accent, List.of(new TextRow(
                    String.format("⌖ %.0f %.0f %.0f · %.0f°", p.x, p.y, p.z, PlayerState.yaw()),
                    Theme.MUTED))));
        }

        String brand = "Cone";
        String ver = "v" + ConeClient.VERSION;
        String pillText = Ui.ellipsize(font, state, 96);
        int pillW = 6 + 4 + 3 + Ui.width(font, pillText) + 6;
        int headerMin = 4 + Ui.width(font, brand) + 4 + Ui.width(font, ver) + 10 + pillW;
        int contentW = Math.max(MIN_W, Math.min(MAX_W, headerMin));
        for (Section s : sections) {
            int t = s.title == null ? 0 : Ui.width(font, s.title) + 24;
            contentW = Math.max(contentW, Math.min(MAX_W, t));
            for (Row r : s.rows) contentW = Math.max(contentW, Math.min(MAX_W, r.minWidth(font)));
        }
        int w = PAD * 2 + contentW;
        int h = HEADER_H + 4;
        for (Section s : sections) h += s.height() + 3;
        h += PAD - 3;

        if (c.hudBackground) {
            Ui.roundRect(g, x, y, w, h, 0, Ui.alpha(Theme.PANEL, 0.95f));
            Theme.border(g, x, y, w, h, Ui.alpha(Theme.LINE, 0.9f));

            Ui.roundRectGradient(g, x, y, 1, h, 0, accent, Ui.alpha(accent, 0.0f));
        }

        int hx = x + PAD;
        int hy = y + PAD - 2;
        Ui.text(g, font, brand, hx, hy, Theme.ACCENT);
        Ui.text(g, font, ver, hx + Ui.width(font, brand) + 4, hy, Theme.MUTED);

        int px = x + w - PAD - pillW;
        int py = hy - 2;
        Ui.roundRect(g, px, py, pillW, 12, 0, Ui.alpha(accent, 0.16f));
        Theme.border(g, px, py, pillW, 12, Ui.alpha(accent, 0.45f));
        int dot = live ? Ui.lerpColor(Ui.alpha(accent, 0.3f), accent, pulse()) : Ui.alpha(accent, 0.8f);
        Ui.roundRect(g, px + 5, py + 4, 4, 4, 0, dot);
        Ui.text(g, font, pillText, px + 12, py + 2, Ui.lerpColor(accent, Theme.TEXT, 0.35f));

        int divY = y + HEADER_H;
        hFade(g, x + 1, divY, w - 2, 1, Ui.alpha(accent, 0.9f), Ui.alpha(accent, 0.05f));

        int tx = x + PAD;
        int ty = divY + 4;
        for (int i = 0; i < sections.size(); i++) {
            Section s = sections.get(i);
            if (s.title != null) {
                Ui.roundRect(g, tx, ty + 2, 2, 6, 0, s.tint);
                Ui.text(g, font, s.title, tx + 6, ty + 1, Theme.SUBTEXT);
                int lx = tx + 6 + Ui.width(font, s.title) + 6;
                if (lx < tx + contentW) {
                    hFade(g, lx, ty + 5, tx + contentW - lx, 1,
                            Ui.alpha(Theme.LINE, 0.9f), Ui.alpha(Theme.LINE, 0.1f));
                }
                ty += LH;
            }
            for (Row r : s.rows) { r.draw(g, font, tx, ty, contentW); ty += LH; }
            ty += 3;
            if (c.hudBackground && i < sections.size() - 1 && s.title == null
                    && sections.get(i + 1).title == null) {
                hFade(g, tx, ty - 2, contentW, 1, Ui.alpha(Theme.LINE, 0.9f), Ui.alpha(Theme.LINE, 0.1f));
            }
        }
        return new int[]{w, h};
    }
}
