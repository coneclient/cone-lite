package net.cone.gui.skija.ui;

import net.cone.gui.skija.SkDesign;

import java.util.function.BooleanSupplier;

public abstract class Widget {
    public int x, y, w, h;

    public String searchLabel = "";
    public String tooltip = "";

    public String pinId;

    public BooleanSupplier changed;

    public Runnable reset;

    public boolean hero;

    private static boolean rebuildRequested;

    public static void requestRebuild() {
        rebuildRequested = true;
    }

    public static boolean consumeRebuild() {
        boolean r = rebuildRequested;
        rebuildRequested = false;
        return r;
    }

    public int height() {
        return 36;
    }

    public void bounds(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    public boolean over(double mx, double my) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    protected boolean in(double mx, double my, float rx, float ry, float rw, float rh) {
        return mx >= rx && mx < rx + rw && my >= ry && my < ry + rh;
    }

    public void draw(Brush b, double mx, double my) {}

    public boolean click(double mx, double my) {
        return false;
    }

    public boolean drag(double mx, double my) {
        return false;
    }

    public void release() {}

    public boolean key(int code) {
        return false;
    }

    public boolean typed(String chars) {
        return false;
    }

    public boolean typing() {
        return false;
    }

    public boolean listening() {
        return false;
    }

    public boolean mouseWhileListening(int button) {
        return false;
    }

    public void blur() {}

    protected int edge() {
        return hero ? SkDesign.DEEP_ROW_EDGE : SkDesign.CARD_EDGE;
    }

    protected int chipBg() {
        return hero ? SkDesign.DEEP_ROW : SkDesign.CARD;
    }

    protected int trackOff() {
        return hero ? 0x3DFFFFFF : SkDesign.CARD_EDGE;
    }

    protected void rowChrome(Brush b) {
        if (hero) b.round(x, y + 2, w, h - 4, SkDesign.R_ROW, SkDesign.DEEP_ROW);
    }

    protected float labelX() {
        return x + (hero ? 10 : 0);
    }

    protected void drawLabel(Brush b, float maxW) {
        float lx = labelX();
        if (changed != null && changed.getAsBoolean()) {
            b.round(lx, y + h / 2f - 1.5f, 3, 3, 1.5f, SkDesign.BRAND);
            lx += 7;
            maxW -= 7;
        }
        b.textV(b.ellipsize(11, searchLabel, maxW), lx, y, h, 11, SkDesign.TEXT);
    }

    protected void pill(Brush b, float px, float py, float pw, float ph, boolean on) {
        b.round(px, py, pw, ph, ph / 2f, on ? SkDesign.BRAND : trackOff());
        float k = ph - 4;
        float kx = on ? px + pw - k - 2 : px + 2;
        b.round(kx, py + 2, k, k, k / 2f, SkDesign.WHITE);
    }
}
