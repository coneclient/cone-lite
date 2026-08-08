package net.cone.gui.skija;

import io.github.humbleui.skija.Canvas;
import net.cone.ConeClient;
import net.cone.core.ConeCore;
import net.cone.gui.skija.SkijaLayout.Card;
import net.cone.gui.skija.SkijaLayout.DashBtn;
import net.cone.gui.skija.SkijaLayout.DashHead;
import net.cone.gui.skija.SkijaLayout.Frame;
import net.cone.gui.skija.SkijaLayout.R;
import net.cone.gui.skija.SkijaLayout.SideRow;
import net.cone.gui.skija.ui.Widget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;

final class SkijaRender {
    private SkijaRender() {}

    private static final SkBrush BRUSH = new SkBrush();

    private static final SkijaCanvas CANVAS = new SkijaCanvas("menu");

    private static final long LIVE_MS = 200;

    private static final long HOVER_DWELL_MS = 450;

    private static int lastW, lastH, lastMx = Integer.MIN_VALUE, lastMy;
    private static double lastScale;
    private static float lastDens;
    private static long lastPaint;
    private static boolean dirty = true;

    private static Frame lastFrame;
    private static Object lastHoverKey;

    static void invalidate() {
        dirty = true;
    }

    static void render(GuiGraphicsExtractor g, SkijaScreen screen, int mouseX, int mouseY) {
        int W = screen.width, H = screen.height;
        double scale = Minecraft.getInstance().getWindow().getGuiScale();

        float d = SkDesign.density(scale);
        int uiW = (int) Math.ceil(W * d), uiH = (int) Math.ceil(H * d);

        long now = System.currentTimeMillis();
        boolean repaint = dirty
                || W != lastW || H != lastH || scale != lastScale || d != lastDens
                || (mouseX != lastMx || mouseY != lastMy) && cursorMatters(screen.state(),
                        Math.round(mouseX * d), Math.round(mouseY * d), now)
                || now - lastPaint >= LIVE_MS;

        if (repaint) {
            Canvas c = CANVAS.begin(uiW, uiH, scale / d);
            draw(c, screen.state(), Math.round(mouseX * d), Math.round(mouseY * d), uiW, uiH);
            CANVAS.end();
            lastW = W; lastH = H; lastScale = scale; lastDens = d;
            lastMx = mouseX; lastMy = mouseY;
            lastPaint = now;
            dirty = false;
        }
        CANVAS.blit(g, 0, 0, W, H);
    }

    private static boolean cursorMatters(SkijaState st, int mx, int my, long now) {
        if (st.dragging != null) return true;
        if (st.hover != null && now - st.hoverSince >= HOVER_DWELL_MS
                && net.cone.config.ConfigManager.get().uiTooltips) {
            return true;
        }
        Object key = hoverKey(lastFrame, mx, my);
        if (key == lastHoverKey) return false;
        lastHoverKey = key;
        return true;
    }

    private static Object hoverKey(Frame f, int mx, int my) {
        if (f == null) return null;
        for (SideRow r : f.sideRows) if (r.box.has(mx, my)) return r;
        for (DashBtn b : f.dashBtns) if (b.box.has(mx, my)) return b;
        for (DashHead h : f.dashHeads) if (h.box.has(mx, my)) return h;
        for (Card card : f.cards) {
            if (card.pill != null && card.pill.has(mx, my)) return card.pill;
            if (card.head.has(mx, my)) return card.head;
            if (!card.expanded || card.module.body.isEmpty()) continue;
            List<Widget> all = new ArrayList<>();
            card.module.body.collect(all);
            for (Widget w : all) if (w.over(mx, my)) return w;
        }
        return null;
    }

    private static void draw(Canvas c, SkijaState st, int mx, int my, int W, int H) {
        Frame f = SkijaLayout.compute(st, W, H);
        lastFrame = f;
        BRUSH.with(c);

        if (SkDesign.GLASS_ON) {
            R gl = f.glass;
            int glr = SkDesign.R_PANEL + 6;
            Sk.fillRound(c, gl.x(), gl.y(), gl.w(), gl.h(), glr, SkDesign.GLASS);
            Sk.strokeRound(c, gl.x() + 0.5f, gl.y() + 0.5f, gl.w() - 1, gl.h() - 1, glr, SkDesign.GLASS_EDGE, 1);
        }

        panel(c, f.side);
        panel(c, f.main);

        drawSidebar(c, st, f, mx, my);
        drawHeader(c, st, f, mx, my);
        if (f.dash) drawDashboard(c, f, mx, my);
        else drawContent(c, st, f, mx, my);
        drawScrollbar(c, f, st.scroll);
        drawFooter(c, f, mx, my);
        drawTooltip(c, st, f, mx, my, W, H);
    }

    private static void panel(Canvas c, R p) {
        Sk.fillRound(c, p.x(), p.y(), p.w(), p.h(), SkDesign.R_PANEL, SkDesign.PANEL);
        Sk.strokeRound(c, p.x() + 0.5f, p.y() + 0.5f, p.w() - 1, p.h() - 1, SkDesign.R_PANEL, SkDesign.CARD_EDGE, 1);
    }

    private static void drawSidebar(Canvas c, SkijaState st, Frame f, int mx, int my) {
        R s = f.side;

        float lx = s.x() + 16, ly = s.y() + 20;
        Sk.fillTriangle(c, lx + 7, ly, lx + 14, ly + 15, lx, ly + 15, SkDesign.BRAND);
        Sk.fillRound(c, lx - 1, ly + 15, 16, 2.5f, 1.25f, SkDesign.BRAND);
        Sk.textBold(c, "Cone", lx + 24, ly, 15, SkDesign.TEXT);
        Sk.text(c, "v" + ConeClient.VERSION, lx + 24 + Sk.textWidth(15, "Cone") + 8, ly + 5, 9, SkDesign.MUTED);

        boolean searching = !st.query.isBlank();

        Sk.clip(c, s.x(), s.y(), s.w(), s.h() - SkijaLayout.SIDE_FOOT_H);
        for (SideRow row : f.sideRows) {
            R r = row.box;
            SkijaModel.Category cat = st.categories.get(row.index);

            if (cat.startsMore) {
                float dy = r.y() - f.sideMoreGap / 2f - 1;
                Sk.textVCenter(c, "MORE", r.x() + 4, dy - 6, 12, 8, SkDesign.MUTED);
                float mlx = r.x() + 4 + Sk.textWidth(8, "MORE") + 8;
                Sk.fillRect(c, mlx, dy, r.x() + r.w() - 4 - mlx, 1, SkDesign.CARD_EDGE);
            }
            boolean active = !searching && row.index == st.selectedCat;
            if (active) {
                Sk.fillRound(c, r.x(), r.y(), r.w(), r.h(), SkDesign.R_ROW, SkDesign.ROW_ON);
                Sk.fillRound(c, r.x(), r.y() + 7, 3, r.h() - 14, 1.5f, SkDesign.BRAND);
            } else if (r.has(mx, my)) {
                Sk.fillRound(c, r.x(), r.y(), r.w(), r.h(), SkDesign.R_ROW, SkDesign.HOVER);
            }
            Sk.textVCenter(c, cat.name, r.x() + 12, r.y(), r.h(), 11, active ? SkDesign.TEXT : SkDesign.SUB);
            int on = cat.onCount();
            if (on > 0) {
                String n = String.valueOf(on);
                Sk.textVCenter(c, n, r.x() + r.w() - 10 - Sk.textWidth(10, n), r.y(), r.h(), 10, SkDesign.BRAND_300);
            }
        }
        Sk.unclip(c);

        float fy = s.y() + s.h() - SkijaLayout.SIDE_FOOT_H;
        Sk.fillRect(c, s.x() + 12, fy, s.w() - 24, 1, SkDesign.CARD_EDGE);
        String name = SkijaDash.displayName();
        Sk.textVCenter(c, Sk.ellipsize(10, name, s.w() - 58), s.x() + 16, fy, SkijaLayout.SIDE_FOOT_H - 12, 10, SkDesign.TEXT);
        Sk.text(c, "public beta", s.x() + 16, fy + 22, 9, SkDesign.MUTED);

        R a = f.appearanceBtn;
        boolean appActive = !searching && st.selectedCat == st.categories.size() - 1;
        boolean appHover = a.has(mx, my);
        if (appActive) Sk.fillRound(c, a.x(), a.y(), a.w(), a.h(), SkDesign.R_ROW, SkDesign.ROW_ON);
        else if (appHover) Sk.fillRound(c, a.x(), a.y(), a.w(), a.h(), SkDesign.R_ROW, SkDesign.HOVER);
        int ic = appActive ? SkDesign.BRAND : appHover ? SkDesign.TEXT : SkDesign.SUB;
        float gx = a.x() + 4, gw = 14;
        float[] ys = { a.y() + 6, a.y() + 11, a.y() + 16 };
        float[] kx = { 10, 3, 7 };
        for (int i = 0; i < 3; i++) {
            Sk.line(c, gx, ys[i], gx + gw, ys[i], ic, 1.2f);
            Sk.fillRound(c, gx + kx[i] - 2, ys[i] - 2, 4, 4, 2, ic);
        }
    }

    private static void drawHeader(Canvas c, SkijaState st, Frame f, int mx, int my) {
        R m = f.main;
        float tx = m.x() + SkijaLayout.PAD, ty = m.y() + 14;
        String title = st.listTitle();
        Sk.textBold(c, title, tx, ty, 15, SkDesign.TEXT);
        if (!st.dashboard()) {
            List<SkijaModel.Module> mods = st.visibleModules();
            int on = 0;
            for (SkijaModel.Module mod : mods) if (mod.master != null && mod.master.getAsBoolean()) on++;
            Sk.text(c, mods.size() + " modules · " + on + " on",
                    tx + Sk.textWidth(15, title) + 10, ty + 5, 9, SkDesign.MUTED);
        }
        st.search.draw(BRUSH, mx, my);
    }

    private static void drawDashboard(Canvas c, Frame f, int mx, int my) {
        R ct = f.content;
        Sk.clip(c, ct.x(), ct.y(), ct.w(), ct.h());

        drawDashHero(c, f.dashHero);
        for (DashHead h : f.dashHeads) drawDashHead(c, h, f);
        if (f.dashPositions != null) drawPositions(c, f.dashPositions);
        drawSurprise(c, f.dashSurprise, mx, my);
        if (f.dashSteps != null) drawSteps(c, f.dashSteps);
        if (f.dashBoard != null) drawMosaic(c, f.dashBoard);
        if (f.dashStrip != null) drawStrip(c, f.dashStrip);
        for (DashBtn b : f.dashBtns) drawDashButton(c, b, mx, my);

        Sk.unclip(c);
    }

    private static void drawDashHero(Canvas c, R b) {
        Sk.fillRound(c, b.x(), b.y(), b.w(), b.h(), SkDesign.R_HERO, SkDesign.DEEP);
        Sk.strokeRound(c, b.x() + 0.5f, b.y() + 0.5f, b.w() - 1, b.h() - 1, SkDesign.R_HERO, SkDesign.DEEP_EDGE, 1);

        Sk.clip(c, b.x(), b.y(), b.w(), b.h());
        int mark = faint(SkDesign.BRAND_300, 0x2E);
        float tx = b.x() + b.w() - 64, tb = b.y() + b.h() + 14;
        Sk.fillTriangle(c, tx, tb - 92, tx + 42, tb, tx - 42, tb, mark);
        Sk.unclip(c);

        float pad = 18;

        double pnl = SkijaDash.heroProfitValue();
        int pnlColor = pnl > 0 ? SkDesign.GOOD : pnl < 0 ? SkDesign.DANGER : SkDesign.WHITE;
        String value = SkijaDash.heroProfit();
        int vSize = 26;
        Sk.textBold(c, value, b.x() + pad, b.y() + 14, vSize, pnlColor);
        float after = b.x() + pad + Sk.textWidth(vSize, value) + 10;

        Sk.text(c, "market data",
                after, b.y() + 28, 10, SkDesign.DEEP_TEXT);
        String rate = SkijaDash.heroRate();
        if (rate != null) {
            float rx = after + Sk.textWidth(10, "realized") + 10;
            Sk.text(c, "·  " + rate, rx, b.y() + 28, 10, SkDesign.BRAND_300);
        }

        Sk.text(c, SkijaDash.heroIdentity(), b.x() + pad, b.y() + 50, 10, SkDesign.DEEP_TEXT);

        int acc = SkijaDash.heroLiveAccent();
        if (acc == SkDesign.MUTED) acc = SkDesign.DEEP_TEXT;
        String live = Sk.ellipsize(10, SkijaDash.heroLive(), b.w() - 2 * pad);
        Sk.text(c, live, b.x() + pad, b.y() + b.h() - 24, 10, acc);

        String fs = SkijaDash.failsafeNote();
        if (fs != null) {
            Sk.text(c, fs, b.x() + b.w() - pad - Sk.textWidth(10, fs), b.y() + 14, 10, SkDesign.DANGER);
        }
    }

    private static void drawPositions(Canvas c, R b) {
        Sk.fillRound(c, b.x(), b.y(), b.w(), b.h(), SkDesign.R_CARD, SkDesign.INSET);
        Sk.strokeRound(c, b.x() + 0.5f, b.y() + 0.5f, b.w() - 1, b.h() - 1, SkDesign.R_CARD, SkDesign.CARD_EDGE, 1);

        List<String[]> pos = SkijaDash.openPositions();
        if (pos.isEmpty()) {
            Sk.textVCenter(c, "Cone Lite tracks the bazaar. Set alerts with /alert",
                    b.x() + 16, b.y(), b.h(), 11, SkDesign.MUTED);
            return;
        }
        int rowH = SkijaLayout.DASH_POS_ROW_H;
        float ry = b.y() + 5;
        for (String[] p : pos) {
            Sk.fillRound(c, b.x() + 14, ry + rowH / 2f - 2.5f, 5, 5, 2.5f, SkDesign.GOOD);
            String detail = p[1] == null ? "" : p[1];
            float detailW = Sk.textWidth(10, detail);
            float headMax = b.w() - 30 - detailW - 24;
            Sk.textVCenter(c, Sk.ellipsize(12, p[0], headMax), b.x() + 28, ry, rowH, 12, SkDesign.TEXT);
            Sk.textVCenter(c, detail, b.x() + b.w() - 16 - detailW, ry, rowH, 10, SkDesign.SUB);
            ry += rowH;
        }
    }

    private static void drawSteps(Canvas c, R b) {
        Sk.fillRound(c, b.x(), b.y(), b.w(), b.h(), SkDesign.R_CARD, SkDesign.INSET);
        Sk.strokeRound(c, b.x() + 0.5f, b.y() + 0.5f, b.w() - 1, b.h() - 1, SkDesign.R_CARD, SkDesign.CARD_EDGE, 1);

        int rowH = SkijaLayout.DASH_STEP_ROW_H;
        float ry = b.y() + 5;
        int n = 1;
        for (String[] s : SkijaDash.firstSteps()) {
            String num = String.valueOf(n++);
            float cx = b.x() + 22, cy = ry + rowH / 2f;
            Sk.fillRound(c, cx - 9, cy - 9, 18, 18, 9, SkDesign.HOVER);
            Sk.text(c, num, cx - Sk.textWidth(9, num) / 2, cy - 5, 9, SkDesign.BRAND_300);
            Sk.text(c, Sk.ellipsize(11, s[0], b.w() - 60), b.x() + 42, ry + 6, 11, SkDesign.TEXT);
            Sk.text(c, Sk.ellipsize(9, s[1], b.w() - 60), b.x() + 42, ry + 20, 9, SkDesign.MUTED);
            ry += rowH;
        }
    }

    private static void drawDashHead(Canvas c, DashHead h, Frame f) {
        R r = h.box;
        String text = h.text.toUpperCase(java.util.Locale.ROOT);
        Sk.textVCenter(c, text, r.x(), r.y(), r.h(), 9, SkDesign.MUTED);
        float lineX = r.x() + Sk.textWidth(9, text) + 10;

        float lineEnd = f.dashSurprise != null && h.box.y() == f.dashSurprise.y() - 1
                ? f.dashSurprise.x() - 10 : r.x() + r.w();
        if (lineEnd > lineX) {
            Sk.fillRect(c, lineX, r.y() + r.h() / 2f, lineEnd - lineX, 1, SkDesign.CARD_EDGE);
        }
    }

    private static void drawSurprise(Canvas c, R sp, int mx, int my) {
        if (sp == null) return;
        float rad = sp.h() / 2f;
        boolean hover = sp.has(mx, my);
        if (hover) Sk.fillRound(c, sp.x(), sp.y(), sp.w(), sp.h(), rad, SkDesign.HOVER);
        Sk.strokeRound(c, sp.x() + 0.5f, sp.y() + 0.5f, sp.w() - 1, sp.h() - 1, rad,
                hover ? SkDesign.BRAND : SkDesign.CARD_EDGE, 1);
        String label = "Surprise me";
        Sk.textVCenter(c, label, sp.x() + (sp.w() - Sk.textWidth(9, label)) / 2, sp.y(), sp.h(), 9, SkDesign.BRAND_300);
        if (!SkijaDash.flavor.isEmpty()) {
            String flavor = Sk.ellipsize(9, SkijaDash.flavor, 260);
            Sk.textVCenter(c, flavor, sp.x() - 10 - Sk.textWidth(9, flavor), sp.y(), sp.h(), 9, SkDesign.SUB);
        }
    }

    private static void drawMosaic(Canvas c, R b) {
        int gap = SkijaLayout.DASH_GAP;
        int bigW = Math.round(b.w() * 0.38f);
        int smallW = (b.w() - bigW - 3 * gap) / 2;
        int rowH = (b.h() - gap) / 2;

        Sk.fillRound(c, b.x(), b.y(), bigW, b.h(), SkDesign.R_CARD, SkDesign.INSET);
        Sk.strokeRound(c, b.x() + 0.5f, b.y() + 0.5f, bigW - 1, b.h() - 1, SkDesign.R_CARD, SkDesign.CARD_EDGE, 1);
        String[] big = SkijaDash.bigStat();
        Sk.textBold(c, Sk.ellipsize(26, big[0], bigW - 32), b.x() + 16, b.y() + 18, 26, SkDesign.TEXT);
        float labelY = b.y() + b.h() - 26;
        Sk.fillTriangle(c, b.x() + 20, labelY + 2, b.x() + 23.5f, labelY + 9, b.x() + 16.5f, labelY + 9, SkDesign.BRAND);
        Sk.text(c, big[1].toUpperCase(java.util.Locale.ROOT), b.x() + 30, labelY, 9, SkDesign.MUTED);

        String[][] smalls = SkijaDash.smallStats();
        for (int i = 0; i < smalls.length; i++) {
            float sx = b.x() + bigW + gap + (i % 2) * (smallW + gap);
            float sy = b.y() + (i / 2) * (rowH + gap);
            Sk.fillRound(c, sx, sy, smallW, rowH, SkDesign.R_CARD, SkDesign.INSET);
            Sk.strokeRound(c, sx + 0.5f, sy + 0.5f, smallW - 1, rowH - 1, SkDesign.R_CARD, SkDesign.CARD_EDGE, 1);
            Sk.textBold(c, Sk.ellipsize(13, smalls[i][0], smallW - 24), sx + 14, sy + 8, 13, SkDesign.TEXT);
            Sk.text(c, smalls[i][1].toUpperCase(java.util.Locale.ROOT), sx + 14, sy + 25, 8, SkDesign.MUTED);
        }
    }

    private static void drawStrip(Canvas c, R b) {
        Sk.textVCenter(c, SkijaDash.stripLine(), b.x() + 2, b.y(), b.h(), 9, SkDesign.MUTED);
        String session = SkijaDash.sessionLine();
        if (session != null) {
            Sk.textVCenter(c, session, b.x() + b.w() - Sk.textWidth(9, session), b.y(), b.h(), 9, SkDesign.SUB);
        }
    }

    private static void drawDashButton(Canvas c, DashBtn b, int mx, int my) {
        R r = b.box;
        float rad = r.h() / 2f;
        boolean hover = r.has(mx, my);
        switch (b.btn.style()) {
            case PRIMARY -> {
                Sk.fillRound(c, r.x(), r.y(), r.w(), r.h(), rad, hover ? 0xFFF5F5F5 : SkDesign.WHITE);
                center(c, b.btn.label(), r, 0xFF0F0F0F);
            }
            case DANGER -> {
                Sk.fillRound(c, r.x(), r.y(), r.w(), r.h(), rad, hover ? 0x33E05252 : SkDesign.INSET);
                Sk.strokeRound(c, r.x() + 0.5f, r.y() + 0.5f, r.w() - 1, r.h() - 1, rad, 0x66E05252, 1);
                center(c, b.btn.label(), r, SkDesign.DANGER);
            }
            default -> {
                Sk.fillRound(c, r.x(), r.y(), r.w(), r.h(), rad, hover ? SkDesign.ROW_ON : SkDesign.INSET);
                Sk.strokeRound(c, r.x() + 0.5f, r.y() + 0.5f, r.w() - 1, r.h() - 1, rad, SkDesign.CARD_EDGE, 1);
                center(c, b.btn.label(), r, SkDesign.TEXT);
            }
        }
    }

    private static int faint(int argb, int alpha) {
        return (argb & 0x00FFFFFF) | (alpha << 24);
    }

    private static void center(Canvas c, String s, R r, int argb) {
        String shown = Sk.ellipsize(10, s, r.w() - 16);
        Sk.textVCenter(c, shown, r.x() + (r.w() - Sk.textWidth(10, shown)) / 2, r.y(), r.h(), 10, argb);
    }

    private static void drawContent(Canvas c, SkijaState st, Frame f, int mx, int my) {
        R ct = f.content;
        Sk.clip(c, ct.x(), ct.y(), ct.w(), ct.h());
        for (Card card : f.cards) {
            if (card.box.y() + card.box.h() < ct.y() || card.box.y() > ct.y() + ct.h()) continue;
            drawCard(c, card, mx, my);
        }
        Sk.unclip(c);

        if (f.cards.isEmpty()) {
            Sk.text(c, "No modules match \"" + st.query + "\"", ct.x(), ct.y() + 8, 12, SkDesign.MUTED);
        }
    }

    private static void drawScrollbar(Canvas c, Frame f, float scroll) {
        if (f.scrollMax <= 0) return;
        R ct = f.content;
        float frac = ct.h() / (float) (ct.h() + f.scrollMax);
        float barH = Math.max(24, ct.h() * frac);
        float barY = ct.y() + (ct.h() - barH) * (scroll / f.scrollMax);
        Sk.fillRound(c, f.main.x() + f.main.w() - 8, barY, 3, barH, 1.5f, SkDesign.CARD_EDGE);
    }

    private static void drawCard(Canvas c, Card card, int mx, int my) {
        R b = card.box;
        SkijaModel.Module mod = card.module;
        boolean on = mod.master != null && mod.master.getAsBoolean();
        boolean hot = card.head.has(mx, my);

        Sk.fillRound(c, b.x(), b.y(), b.w(), b.h(), SkDesign.R_CARD, on ? SkDesign.DEEP : SkDesign.INSET);
        Sk.strokeRound(c, b.x() + 0.5f, b.y() + 0.5f, b.w() - 1, b.h() - 1, SkDesign.R_CARD,
                on ? SkDesign.DEEP_EDGE : SkDesign.CARD_EDGE, 1);
        if (hot) {
            Sk.fillRound(c, b.x(), b.y(), b.w(), card.expanded ? SkijaLayout.CARD_H : b.h(),
                    SkDesign.R_CARD, SkDesign.HOVER);
        }

        if (on) Sk.fillRound(c, b.x() + 1, b.y() + 12, 3, SkijaLayout.CARD_H - 24, 1.5f, SkDesign.BRAND);

        float pad = SkijaLayout.BODY_PAD;
        float tx = b.x() + pad + 6;
        float pillSpace = card.pill != null ? SkijaLayout.PILL_W + 14 : 0;
        float chevSpace = mod.body.isEmpty() ? 0 : 18;
        float textW = b.w() - (tx - b.x()) - pad - pillSpace - chevSpace;

        int titleSize = SkDesign.T_HEAD;
        String title = Sk.ellipsize(titleSize, mod.name, textW);
        Sk.textBold(c, title, tx, b.y() + 13, titleSize, on ? SkDesign.WHITE : SkDesign.TEXT);
        if (!mod.body.isEmpty()) {
            String count = mod.body.size() + (mod.body.size() == 1 ? " setting" : " settings");
            float cxx = tx + Sk.textWidth(titleSize, title) + 10;
            if (cxx + Sk.textWidth(SkDesign.T_SMALL, count) < b.x() + b.w() - pad - pillSpace - chevSpace) {
                Sk.text(c, count, cxx, b.y() + 17, SkDesign.T_SMALL, SkDesign.MUTED);
            }
        }

        String desc = Sk.ellipsize(SkDesign.T_CAPTION, mod.desc, textW);
        Sk.text(c, desc, tx, b.y() + 36, SkDesign.T_CAPTION, on ? SkDesign.DEEP_TEXT : SkDesign.SUB);

        if (card.pill != null) drawPill(c, card.pill, on, SkDesign.CARD_EDGE);
        if (!mod.body.isEmpty()) {
            drawChevron(c, b, SkijaLayout.CARD_H, card.expanded,
                    hot ? SkDesign.TEXT : SkDesign.MUTED);
        }

        if (card.expanded && !mod.body.isEmpty()) {
            Sk.fillRect(c, b.x() + pad, b.y() + SkijaLayout.CARD_H, b.w() - 2 * pad, 1,
                    on ? SkDesign.DEEP_ROW_EDGE : SkDesign.CARD_EDGE);
            mod.body.draw(BRUSH, mx, my);
        }
    }

    private static void drawChevron(Canvas c, R b, int headH, boolean expanded, int color) {
        float chx = b.x() + b.w() - SkijaLayout.BODY_PAD - SkijaLayout.PILL_W - 26;
        float chy = b.y() + headH / 2f - 2;
        if (expanded) {
            Sk.line(c, chx, chy + 3, chx + 4, chy, color, 1.4f);
            Sk.line(c, chx + 4, chy, chx + 8, chy + 3, color, 1.4f);
        } else {
            Sk.line(c, chx, chy, chx + 4, chy + 3, color, 1.4f);
            Sk.line(c, chx + 4, chy + 3, chx + 8, chy, color, 1.4f);
        }
    }

    private static void drawPill(Canvas c, R t, boolean on, int offColor) {
        int track = on ? SkDesign.BRAND : offColor;
        Sk.fillRound(c, t.x(), t.y(), t.w(), t.h(), t.h() / 2f, track);
        float r = t.h() - 4;
        float knobX = on ? t.x() + t.w() - r - 2 : t.x() + 2;
        Sk.fillRound(c, knobX, t.y() + 2, r, r, r / 2f, SkDesign.WHITE);
    }

    private static void drawTooltip(Canvas c, SkijaState st, Frame f, int mx, int my, int W, int H) {
        if (!net.cone.config.ConfigManager.get().uiTooltips) return;
        Widget hit = null;
        if (!f.dash && f.content.has(mx, my)) {
            outer:
            for (Card card : f.cards) {
                if (!card.expanded || card.module.body.isEmpty()) continue;
                List<Widget> all = new ArrayList<>();
                card.module.body.collect(all);
                for (Widget w : all) {
                    if (!w.tooltip.isEmpty() && w.over(mx, my)) {
                        hit = w;
                        break outer;
                    }
                }
            }
        }
        if (hit != st.hover) {
            st.hover = hit;
            st.hoverSince = System.currentTimeMillis();
            return;
        }
        if (hit == null || System.currentTimeMillis() - st.hoverSince < HOVER_DWELL_MS) return;
        if (st.dragging != null) return;

        List<String> lines = wrap(10, hit.tooltip, 220, 4);
        if (hit.reset != null || hit.pinId != null) {
            StringBuilder hints = new StringBuilder();
            if (hit.reset != null) hints.append("R-click: reset");
            if (hit.pinId != null) {
                if (hints.length() > 0) hints.append("   ");
                hints.append(net.cone.config.ConfigManager.get().uiPinned.contains(hit.pinId)
                        ? "M-click: unpin" : "M-click: pin");
            }
            lines.add(hints.toString());
        }
        float tw = 0;
        for (String l : lines) tw = Math.max(tw, Sk.textWidth(10, l));
        float th = lines.size() * 13 + 12;
        float tx = Math.min(mx + 12, W - tw - 24);
        float ty = my + 14 + th > H ? my - th - 6 : my + 14;
        Sk.fillRound(c, tx, ty, tw + 16, th, 7, 0xF5101010);
        Sk.strokeRound(c, tx + 0.5f, ty + 0.5f, tw + 15, th - 1, 7, SkDesign.CARD_EDGE, 1);
        for (int i = 0; i < lines.size(); i++) {
            boolean hint = i == lines.size() - 1 && (hit.reset != null || hit.pinId != null);
            Sk.text(c, lines.get(i), tx + 8, ty + 6 + i * 13, 10, hint ? SkDesign.MUTED : SkDesign.SUB);
        }
    }

    private static void drawFooter(Canvas c, Frame f, int mx, int my) {
        R ft = f.footer;
        Sk.fillRect(c, ft.x() + SkijaLayout.PAD, ft.y(), ft.w() - 2 * SkijaLayout.PAD, 1, SkDesign.CARD_EDGE);

        Sk.textVCenter(c, "search finds every setting", ft.x() + SkijaLayout.PAD, ft.y(), ft.h(), 9, SkDesign.MUTED);

        String rail = ConeCore.activeTaskName();
        if (rail != null) {
            R sp = f.stopPill;
            float rad = sp.h() / 2f;
            boolean hover = sp.has(mx, my);
            Sk.fillRound(c, sp.x(), sp.y(), sp.w(), sp.h(), rad, hover ? 0x33E05252 : SkDesign.INSET);
            Sk.strokeRound(c, sp.x() + 0.5f, sp.y() + 0.5f, sp.w() - 1, sp.h() - 1, rad, 0x66E05252, 1);
            String label = Sk.ellipsize(10, "Stop " + rail, sp.w() - 16);
            Sk.textVCenter(c, label, sp.x() + (sp.w() - Sk.textWidth(10, label)) / 2, sp.y(), sp.h(), 10, SkDesign.DANGER);
        } else {
            String idle = "nothing running";
            Sk.textVCenter(c, idle, f.main.x() + f.main.w() - SkijaLayout.PAD - Sk.textWidth(9, idle),
                    ft.y(), ft.h(), 9, SkDesign.MUTED);
        }
    }

    private static List<String> wrap(int size, String text, float maxW, int maxLines) {
        List<String> out = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            String probe = line.isEmpty() ? word : line + " " + word;
            if (Sk.textWidth(size, probe) > maxW && !line.isEmpty()) {
                if (out.size() == maxLines - 1) {
                    out.add(Sk.ellipsize(size, line + " " + word, maxW));
                    return out;
                }
                out.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(probe);
            }
        }
        if (!line.isEmpty()) out.add(line.toString());
        return out;
    }
}
