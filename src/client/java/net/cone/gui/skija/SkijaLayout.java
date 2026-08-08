package net.cone.gui.skija;

import java.util.ArrayList;
import java.util.List;

final class SkijaLayout {
    private SkijaLayout() {}

    record R(int x, int y, int w, int h) {
        boolean has(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    static final int MARGIN = 24;
    static final int MAX_W = 1100;
    static final int MAX_H = 640;
    static final int SIDE_W = 196;
    static final int GAP = 12;
    static final int GLASS_PAD = 16;
    static final int PAD = 20;

    static final int SIDE_HEAD_H = 64;
    static final int SIDE_ROW_H = 34;
    static final int SIDE_ROW_MIN = 20;
    static final int SIDE_FOOT_H = 48;

    static final int HEADER_H = 58;
    static final int FOOTER_H = 40;
    static final int SEARCH_W = 190;
    static final int SEARCH_H = 30;

    static final int CARD_H = 64;
    static final int CARD_GAP = 8;
    static final int BODY_PAD = 16;
    static final int PILL_W = 46, PILL_H = 24;
    static final int MAX_ROW_W = 720;

    static final int DASH_BTN_H = 26;
    static final int DASH_HEAD_H = 20;
    static final int DASH_HERO_H = 96;
    static final int DASH_MOSAIC_H = 96;
    static final int DASH_STRIP_H = 22;
    static final int DASH_PILL_H = 30;
    static final int DASH_POS_ROW_H = 30;
    static final int DASH_STEP_ROW_H = 34;
    static final int DASH_GAP = 8;
    static final int SIDE_MORE_GAP = 16;

    static final class SideRow {
        final int index;
        final R box;
        SideRow(int index, R box) { this.index = index; this.box = box; }
    }

    static final class Card {
        final SkijaModel.Module module;
        final R box;
        final R head;
        final R pill;
        final boolean hero;
        final boolean expanded;
        Card(SkijaModel.Module module, R box, R head, R pill, boolean hero, boolean expanded) {
            this.module = module;
            this.box = box;
            this.head = head;
            this.pill = pill;
            this.hero = hero;
            this.expanded = expanded;
        }
    }

    static final class DashBtn {
        final SkijaDash.Btn btn;
        final R box;
        DashBtn(SkijaDash.Btn btn, R box) { this.btn = btn; this.box = box; }
    }

    static final class DashHead {
        final String text;
        final R box;
        DashHead(String text, R box) { this.text = text; this.box = box; }
    }

    static final class Frame {
        R glass, side, main, search, content, footer, stopPill, appearanceBtn;
        int sideMoreGap = SIDE_MORE_GAP;
        final List<SideRow> sideRows = new ArrayList<>();
        final List<Card> cards = new ArrayList<>();

        boolean dash;
        R dashHero, dashPositions, dashSurprise, dashBoard, dashStrip;
        R dashSteps;
        final List<DashBtn> dashBtns = new ArrayList<>();
        final List<DashHead> dashHeads = new ArrayList<>();
        int scrollMax;
    }

    private static int cardHeight(SkijaModel.Module m, boolean expanded) {
        if (!expanded || m.body.isEmpty()) return CARD_H;
        return CARD_H + BODY_PAD + m.body.height() + BODY_PAD;
    }

    static Frame compute(SkijaState st, int screenW, int screenH) {
        Frame f = layout(st, screenW, screenH);

        if (st.scroll > f.scrollMax) {
            st.scroll = f.scrollMax;
            f = layout(st, screenW, screenH);
        }
        return f;
    }

    private static Frame layout(SkijaState st, int screenW, int screenH) {
        Frame f = new Frame();

        int totalW = Math.min(MAX_W, screenW - 2 * MARGIN);
        int totalH = Math.min(MAX_H, screenH - 2 * MARGIN);
        int ox = (screenW - totalW) / 2;
        int oy = (screenH - totalH) / 2;

        f.side = new R(ox, oy, SIDE_W, totalH);
        f.main = new R(ox + SIDE_W + GAP, oy, totalW - SIDE_W - GAP, totalH);
        f.glass = new R(ox - GLASS_PAD, oy - GLASS_PAD, totalW + 2 * GLASS_PAD, totalH + 2 * GLASS_PAD);

        int rows = st.categories.size() - 1;
        int avail = totalH - SIDE_HEAD_H - 6 - SIDE_FOOT_H - 6;
        int rowH = SIDE_ROW_H, rowGap = 2, moreGap = SIDE_MORE_GAP;
        if (rows * (rowH + rowGap) + moreGap > avail) {
            rowGap = 0;
            moreGap = SIDE_MORE_GAP / 2;
            rowH = Math.max(SIDE_ROW_MIN, (avail - moreGap) / Math.max(1, rows));
        }
        f.sideMoreGap = moreGap;
        int ry = oy + SIDE_HEAD_H + 6;
        for (int i = 0; i < rows; i++) {
            if (st.categories.get(i).startsMore) ry += moreGap;
            f.sideRows.add(new SideRow(i, new R(ox + 8, ry, SIDE_W - 16, rowH)));
            ry += rowH + rowGap;
        }
        f.appearanceBtn = new R(ox + SIDE_W - 34, oy + totalH - SIDE_FOOT_H + (SIDE_FOOT_H - 22) / 2, 22, 22);

        R m = f.main;
        f.search = new R(m.x() + m.w() - PAD - SEARCH_W, m.y() + (HEADER_H - SEARCH_H) / 2, SEARCH_W, SEARCH_H);
        st.search.bounds(f.search.x(), f.search.y(), f.search.w(), f.search.h());
        f.footer = new R(m.x(), m.y() + m.h() - FOOTER_H, m.w(), FOOTER_H);
        f.stopPill = new R(m.x() + m.w() - PAD - 130, f.footer.y() + (FOOTER_H - 24) / 2, 130, 24);
        f.content = new R(m.x() + PAD, m.y() + HEADER_H + 4, m.w() - 2 * PAD,
                m.h() - HEADER_H - 4 - FOOTER_H - 6);

        if (st.dashboard()) {
            layoutDashboard(f, st);
            return f;
        }

        List<SkijaModel.Module> mods = st.visibleModules();
        int rowW = Math.min(MAX_ROW_W, f.content.w());
        int rowX = f.content.x() + (f.content.w() - rowW) / 2;
        int scroll = Math.round(st.scroll);

        int y = 0;
        for (SkijaModel.Module mod : mods) {
            boolean expanded = mod.id.equals(st.expandedId);
            int ch = cardHeight(mod, expanded);
            addCard(f, mod, new R(rowX, f.content.y() + y - scroll, rowW, ch), expanded);
            y += ch + CARD_GAP;
        }

        f.scrollMax = Math.max(0, y - CARD_GAP - f.content.h());
        return f;
    }

    private static void addCard(Frame f, SkijaModel.Module m, R box, boolean expanded) {
        R head = new R(box.x(), box.y(), box.w(), CARD_H);
        R pill = m.master == null ? null
                : new R(box.x() + box.w() - BODY_PAD - PILL_W, box.y() + (CARD_H - PILL_H) / 2, PILL_W, PILL_H);

        if (expanded && !m.body.isEmpty()) {
            m.body.setHero(false);
            m.body.bounds(box.x() + BODY_PAD, box.y() + CARD_H + BODY_PAD,
                    box.w() - 2 * BODY_PAD, m.body.height());
        }
        f.cards.add(new Card(m, box, head, pill, false, expanded));
    }

    private static void layoutDashboard(Frame f, SkijaState st) {
        f.dash = true;
        R ct = f.content;
        int scroll = Math.round(st.scroll);
        int x = ct.x(), w = ct.w();
        int y = ct.y() - scroll;

        SkijaDash.Btn banner = SkijaDash.updateBanner();
        if (banner != null) {
            f.dashBtns.add(new DashBtn(banner, new R(x, y, w, DASH_BTN_H)));
            y += DASH_BTN_H + DASH_GAP + 2;
        }

        f.dashHero = new R(x, y, w, DASH_HERO_H);
        y += DASH_HERO_H + DASH_GAP + 4;

        y = head(f, "Open positions", x, y, w);
        int posRows = Math.max(1, SkijaDash.openPositions().size());
        int posH = 10 + posRows * DASH_POS_ROW_H;
        f.dashPositions = new R(x, y, w, posH);
        y += posH + DASH_GAP + 4;

        if (SkijaDash.firstRun()) {
            y = head(f, "Start here", x, y, w);
            int stepH = 10 + SkijaDash.firstSteps().size() * DASH_STEP_ROW_H;
            f.dashSteps = new R(x, y, w, stepH);
            y += stepH + DASH_GAP + 4;
        } else {
            y = head(f, "Lifetime", x, y, w);
            f.dashSurprise = new R(x + w - 92, f.dashHeads.get(f.dashHeads.size() - 1).box.y() + 1, 92, 18);

            f.dashBoard = new R(x, y, w, DASH_MOSAIC_H);
            y += DASH_MOSAIC_H + DASH_GAP;

            f.dashStrip = new R(x, y, w, DASH_STRIP_H);
            y += DASH_STRIP_H + DASH_GAP + 4;
        }

        y = head(f, "Quick actions", x, y, w);
        y = pillRow(f, SkijaDash.quickActions(), x, y, w);

        List<SkijaDash.Btn> profs = SkijaDash.profiles();
        if (!profs.isEmpty()) {
            y = head(f, "Profiles", x, y, w);
            y = pillRow(f, profs, x, y, w);
        }

        f.scrollMax = Math.max(0, (y + scroll - ct.y()) - DASH_GAP - ct.h());
    }

    private static int head(Frame f, String text, int x, int y, int w) {
        f.dashHeads.add(new DashHead(text, new R(x, y, w, DASH_HEAD_H)));
        return y + DASH_HEAD_H;
    }

    private static int pillW(String label) {
        return 30 + label.length() * 6;
    }

    private static int pillRow(Frame f, List<SkijaDash.Btn> btns, int x, int y, int w) {
        int cx = 0;
        for (SkijaDash.Btn b : btns) {
            int bw = Math.min(w, pillW(b.label()));
            if (cx > 0 && cx + bw > w) {
                cx = 0;
                y += DASH_PILL_H + DASH_GAP;
            }
            f.dashBtns.add(new DashBtn(b, new R(x + cx, y, bw, DASH_PILL_H)));
            cx += bw + DASH_GAP;
        }
        return y + DASH_PILL_H + DASH_GAP + 6;
    }
}
