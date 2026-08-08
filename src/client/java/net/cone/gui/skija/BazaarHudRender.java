package net.cone.gui.skija;

import io.github.humbleui.skija.Canvas;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.cone.economy.PriceClient;

import java.util.List;

public final class BazaarHudRender {
    private BazaarHudRender() {}

    private static final float PAD = 12;
    private static final float HEADER_H = 20;
    private static final float ROW_H = 34;
    private static final float FOOTER_H = 16;
    private static final float MIN_W = 168;
    private static final float MAX_W = 300;

    private static int lastKey;

    public static void draw(GuiGraphicsExtractor g, int lx, int ly, List<PriceClient.Entry> rows, long ageMs) {
        Minecraft mc = Minecraft.getInstance();
        double scale = mc.getWindow().getGuiScale();
        float d = SkDesign.density(scale);

        float w = cardWidth(rows);
        float h = PAD + HEADER_H + rows.size() * ROW_H + FOOTER_H + PAD;

        SkijaCanvas sc = SkijaCanvas.get();
        int key = key(rows, ageMs, scale, d);
        if (key != lastKey) {
            Canvas c = sc.begin((int) Math.ceil(w), (int) Math.ceil(h), scale / d);
            card(c, 0, 0, w, h, rows, ageMs);
            sc.end();
            lastKey = key;
        }
        sc.blit(g, lx, ly, w / d, h / d);
    }

    private static float cardWidth(List<PriceClient.Entry> rows) {
        float w = MIN_W;
        for (PriceClient.Entry e : rows) {
            float need = Sk.textWidth(SkDesign.T_SMALL, e.name()) + PAD * 2 + 14;
            w = Math.max(w, need);
        }
        return Math.min(MAX_W, w);
    }

    private static int key(List<PriceClient.Entry> rows, long ageMs, double scale, float d) {
        int k = Double.hashCode(scale) * 31 + Float.hashCode(d);
        k = k * 31 + Long.hashCode(ageMs / 1000);
        k = k * 31 + SkDesign.BRAND + SkDesign.R_CARD;
        for (PriceClient.Entry e : rows) {
            k = k * 31 + e.name().hashCode();
            k = k * 31 + Double.hashCode(e.instaSell());
            k = k * 31 + Double.hashCode(e.instaBuy());
            k = k * 31 + Long.hashCode(Math.min(e.buyMovingWk(), e.sellMovingWk()));
            k = k * 31 + Double.hashCode(e.avgSell());
            k = k * 31 + (e.flagged() ? 1 : 0);
        }
        return k;
    }

    private static void card(Canvas c, float x, float y, float w, float h,
                             List<PriceClient.Entry> rows, long ageMs) {
        Sk.fillRound(c, x, y, w, h, SkDesign.R_CARD, SkDesign.PANEL);
        Sk.strokeRound(c, x + 0.5f, y + 0.5f, w - 1, h - 1, SkDesign.R_CARD, SkDesign.CARD_EDGE, 1);

        float cx = x + PAD;
        float cw = w - PAD * 2;
        float cy = y + PAD;

        Sk.textBold(c, "BAZAAR", cx, cy + 2, SkDesign.T_CAPTION, SkDesign.BRAND);
        String count = String.valueOf(rows.size());
        float cntW = Sk.textWidth(SkDesign.T_CAPTION, count);
        Sk.text(c, count, x + w - PAD - cntW, cy + 2, SkDesign.T_CAPTION, SkDesign.SUB);
        Sk.fillRound(c, x + w - PAD - cntW - 12, cy + 4, 5, 5, 2.5f, SkDesign.GOOD);
        cy += HEADER_H;
        Sk.line(c, cx, cy - 4, x + w - PAD, cy - 4, SkDesign.CARD_EDGE, 1);

        for (PriceClient.Entry e : rows) {
            String name = Sk.ellipsize(SkDesign.T_SMALL, e.name(), cw - 12);
            int nameCol = e.flagged() ? SkDesign.DANGER : SkDesign.TEXT;
            Sk.textBold(c, name, cx, cy + 3, SkDesign.T_SMALL, nameCol);
            if (e.flagged()) {
                Sk.fillRound(c, x + w - PAD - 6, cy + 3, 5, 5, 2.5f, SkDesign.DANGER);
            }

            float ly2 = cy + 16;
            Sk.text(c, "sell", cx, ly2, SkDesign.T_SMALL, SkDesign.MUTED);
            float sx = cx + Sk.textWidth(SkDesign.T_SMALL, "sell") + 5;
            String sell = PriceClient.comma(e.instaSell());
            Sk.text(c, sell, sx, ly2, SkDesign.T_SMALL, SkDesign.BRAND_300);
            float tx = sx + Sk.textWidth(SkDesign.T_SMALL, sell) + 5;
            if (e.avgSell() > 0) {
                boolean up = e.instaSell() >= e.avgSell();
                trend(c, tx, ly2 + 4, up);
            }

            String buy = "buy " + PriceClient.comma(e.instaBuy());
            float bw = Sk.textWidth(SkDesign.T_SMALL, buy);
            Sk.text(c, buy, x + w - PAD - bw, ly2, SkDesign.T_SMALL, SkDesign.SUB);

            float ly3 = cy + 27;
            long vol = Math.min(e.buyMovingWk(), e.sellMovingWk());
            Sk.text(c, "vol " + PriceClient.comma(vol) + "/wk", cx, ly3, SkDesign.T_SMALL, SkDesign.MUTED);

            cy += ROW_H;
        }

        String upd = ageMs < 0 ? "—" : (ageMs / 1000) + "s ago";
        Sk.text(c, "updated " + upd, cx, cy + 2, SkDesign.T_SMALL, SkDesign.MUTED);
    }

    private static void trend(Canvas c, float cx, float cy, boolean up) {
        int col = up ? SkDesign.GOOD : SkDesign.DANGER;
        if (up) {
            Sk.fillTriangle(c, cx - 3.5f, cy + 2.5f, cx + 3.5f, cy + 2.5f, cx, cy - 3f, col);
        } else {
            Sk.fillTriangle(c, cx - 3.5f, cy - 2.5f, cx + 3.5f, cy - 2.5f, cx, cy + 3f, col);
        }
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
