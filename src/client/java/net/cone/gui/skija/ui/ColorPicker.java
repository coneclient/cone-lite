package net.cone.gui.skija.ui;

import net.cone.gui.skija.SkDesign;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

public final class ColorPicker extends Widget {
    private static final int BAR_H = 10, SWATCH = 26;

    private final DoubleSupplier hueGet;
    private final DoubleConsumer hueSet;
    private final DoubleSupplier satGet;
    private final DoubleConsumer satSet;
    private int dragging = -1;

    public ColorPicker(String label, DoubleSupplier hueGet, DoubleConsumer hueSet,
                       DoubleSupplier satGet, DoubleConsumer satSet) {
        this.searchLabel = label;
        this.hueGet = hueGet;
        this.hueSet = hueSet;
        this.satGet = satGet;
        this.satSet = satSet;
    }

    @Override
    public int height() {
        return 54;
    }

    private float barX() {
        return labelX() + SWATCH + 10;
    }

    private float barW() {
        return x + w - (hero ? 10 : 0) - barX();
    }

    private float hueY() {
        return y + 22;
    }

    private float satY() {
        return y + 38;
    }

    private static final int STOCK_W = 14;

    @Override
    public void draw(Brush b, double mx, double my) {
        rowChrome(b);
        b.textV(b.ellipsize(11, searchLabel, w - 20), labelX(), y, 20, 11, SkDesign.TEXT);
        String cur = hueGet.getAsDouble() < 0 ? "stock orange"
                : Math.round(hueGet.getAsDouble()) + "° · " + Math.round(satGet.getAsDouble() * 100) + "%";
        b.textV(cur, x + w - (hero ? 10 : 0) - b.width(10, cur), y, 20, 10, SkDesign.BRAND_300);

        float sx = labelX(), sy = y + 24;
        b.round(sx, sy, SWATCH, SWATCH, 8, SkDesign.BRAND);
        b.roundStroke(sx + 0.5f, sy + 0.5f, SWATCH - 1, SWATCH - 1, 8, edge(), 1);

        float bx = barX(), bw = barW();
        double hue = hueGet.getAsDouble();
        double sat = Math.max(0.3, Math.min(1, satGet.getAsDouble()));

        b.round(bx, hueY(), STOCK_W, BAR_H, 3, 0xFFFF7A1F);
        float wheelX = bx + STOCK_W + 2, wheelW = bw - STOCK_W - 2;
        int slices = Math.max(24, (int) (wheelW / 4));
        for (int i = 0; i < slices; i++) {
            float fx = wheelX + wheelW * i / slices;
            float fw = wheelW / slices + 0.5f;
            b.rect(fx, hueY(), fw, BAR_H, hsv(360f * i / slices, 0.85f, 1f));
        }
        float cursorX = hue < 0 ? bx + STOCK_W / 2f
                : wheelX + (float) (wheelW * (hue % 360) / 360);
        cursor(b, cursorX, hueY());

        float sh = hue < 0 ? 24 : (float) (hue % 360);
        for (int i = 0; i < slices; i++) {
            float fx = bx + bw * i / slices;
            float fw = bw / slices + 0.5f;
            b.rect(fx, satY(), fw, BAR_H, hsv(sh, 0.3f + 0.7f * i / slices, 1f));
        }
        cursor(b, bx + (float) (bw * (sat - 0.3) / 0.7), satY());
    }

    private void cursor(Brush b, float cx, float cy) {
        b.round(cx - 2, cy - 2, 4, BAR_H + 4, 2, SkDesign.WHITE);
        b.roundStroke(cx - 2.5f, cy - 2.5f, 5, BAR_H + 5, 2.5f, 0x66000000, 1);
    }

    @Override
    public boolean click(double mx, double my) {
        float bx = barX(), bw = barW();
        if (in(mx, my, bx - 4, hueY() - 4, bw + 8, BAR_H + 8)) {
            dragging = 0;
            apply(mx);
            return true;
        }
        if (in(mx, my, bx - 4, satY() - 4, bw + 8, BAR_H + 8)) {
            dragging = 1;
            apply(mx);
            return true;
        }
        return false;
    }

    @Override
    public boolean drag(double mx, double my) {
        if (dragging < 0) return false;
        apply(mx);
        return true;
    }

    @Override
    public void release() {
        dragging = -1;
    }

    @Override
    public void blur() {
        dragging = -1;
    }

    private void apply(double mx) {
        float bx = barX(), bw = barW();
        if (dragging == 0) {
            if (mx <= bx + STOCK_W) {
                hueSet.accept(-1);
                return;
            }
            float wheelX = bx + STOCK_W + 2, wheelW = bw - STOCK_W - 2;
            double frac = Math.max(0, Math.min(1, (mx - wheelX) / wheelW));
            hueSet.accept(Math.round(frac * 360));
        } else {
            double frac = Math.max(0, Math.min(1, (mx - bx) / bw));
            satSet.accept(0.3 + frac * 0.7);
        }
    }

    private static int hsv(float h, float s, float v) {
        float c = v * s;
        float xx = c * (1 - Math.abs((h / 60f) % 2 - 1));
        float m = v - c;
        float r, g, b;
        if (h < 60)       { r = c; g = xx; b = 0; }
        else if (h < 120) { r = xx; g = c; b = 0; }
        else if (h < 180) { r = 0; g = c; b = xx; }
        else if (h < 240) { r = 0; g = xx; b = c; }
        else if (h < 300) { r = xx; g = 0; b = c; }
        else              { r = c; g = 0; b = xx; }
        return 0xFF000000
                | (Math.round((r + m) * 255) << 16)
                | (Math.round((g + m) * 255) << 8)
                | Math.round((b + m) * 255);
    }
}
