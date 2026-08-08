package net.cone.gui.hud;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.cone.ConeClient;
import net.cone.config.ConfigManager;
import net.cone.config.ConeConfig;
import net.cone.economy.BazaarTracker;
import net.cone.economy.PriceClient;
import net.cone.gui.Theme;
import net.cone.gui.Ui;
import net.cone.gui.skija.SkijaLib;

import java.util.List;

public final class BazaarHud {
    private static final int PAD = 6;
    private static final int LH = 11;
    private static final int MIN_W = 120;
    private static final int MAX_W = 200;

    private BazaarHud() {}

    public static void register() {
        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(ConeClient.MOD_ID, "bazaar_hud"),
                BazaarHud::render);
    }

    private static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        ConeConfig c = ConfigManager.get();
        if (mc.player == null || !c.bazaarHudEnabled) return;
        List<PriceClient.Entry> rows = BazaarTracker.snapshot();
        if (rows.isEmpty()) return;

        SkijaLib.ensureStarted();
        if (SkijaLib.state() == SkijaLib.State.READY) {
            net.cone.gui.skija.BazaarHudRender.draw(g, c.bazaarHudX, c.bazaarHudY, rows, BazaarTracker.ageMs());
        } else {
            draw(g, mc.font, c.bazaarHudX, c.bazaarHudY, rows);
        }
    }

    static int[] draw(GuiGraphicsExtractor g, Font font, int x, int y, List<PriceClient.Entry> rows) {
        int contentW = Ui.width(font, "BAZAAR");
        for (PriceClient.Entry e : rows) {
            int w = Ui.width(font, e.name()) + 12 + Ui.width(font, PriceClient.comma(e.instaSell()));
            contentW = Math.max(contentW, w);
        }
        contentW = Math.max(MIN_W, Math.min(MAX_W, contentW));
        int w = contentW + PAD * 2;
        int h = PAD * 2 + LH + rows.size() * LH;

        Ui.shadow(g, x, y, w, h, 5);
        Ui.roundRect(g, x, y, w, h, 5, Theme.PANEL);

        int tx = x + PAD;
        int ty = y + PAD;
        Ui.text(g, font, "BAZAAR", tx, ty, Theme.ACCENT);
        ty += LH;
        for (PriceClient.Entry e : rows) {
            String name = e.name();
            String val = PriceClient.comma(e.instaSell());
            int vw = Ui.width(font, val);
            int nameCol = e.flagged() ? Theme.DANGER : Theme.TEXT;
            Ui.text(g, font, Ui.ellipsize(font, name, contentW - vw - 8), tx, ty, nameCol);
            Ui.text(g, font, val, tx + contentW - vw, ty, Theme.GOOD);
            ty += LH;
        }
        return new int[]{w, h};
    }

    public static int[] drawPreview(GuiGraphicsExtractor g, Font font, int x, int y) {
        List<PriceClient.Entry> rows = BazaarTracker.snapshot();
        if (rows.isEmpty()) rows = List.of(
                new PriceClient.Entry("ENCHANTED_DIAMOND", "Enchanted Diamond", 1335, 1260, 0, 0, 1244, 0, 0, false, ""),
                new PriceClient.Entry("ENCHANTED_LAPIS_BLOCK", "Enchanted Lapis Block", 13100, 12300, 0, 0, 12146, 0, 0, false, ""));
        return draw(g, font, x, y, rows);
    }

    private static String pretty(String id) {
        String[] parts = id.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return sb.toString();
    }
}
