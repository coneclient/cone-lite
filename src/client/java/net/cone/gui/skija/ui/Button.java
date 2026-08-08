package net.cone.gui.skija.ui;

import net.cone.gui.skija.SkDesign;

import java.util.function.Supplier;

public final class Button extends Widget {
    public enum Style { NORMAL, PRIMARY, DANGER, GHOST }

    private final Supplier<String> label;
    private final Supplier<Style> style;
    private final Runnable onClick;
    private final boolean rebuilds;

    public Button(String label, Style style, Runnable onClick) {
        this(() -> label, () -> style, onClick, false);
    }

    public Button(Supplier<String> label, Style style, Runnable onClick, boolean rebuilds) {
        this(label, () -> style, onClick, rebuilds);
    }

    public Button(Supplier<String> label, Supplier<Style> style, Runnable onClick, boolean rebuilds) {
        this.label = label;
        this.style = style;
        this.onClick = onClick;
        this.rebuilds = rebuilds;
    }

    @Override
    public int height() {
        return 30;
    }

    @Override
    public void draw(Brush b, double mx, double my) {
        float rad = h / 2f;
        boolean hov = over(mx, my);
        switch (style.get()) {
            case PRIMARY -> {
                b.round(x, y, w, h, rad, hov ? 0xFFF5F5F5 : SkDesign.WHITE);
                center(b, 0xFF0F0F0F);
            }
            case DANGER -> {
                b.round(x, y, w, h, rad, hov ? 0x33E05252 : chipBg());
                b.roundStroke(x + 0.5f, y + 0.5f, w - 1, h - 1, rad, 0x66E05252, 1);
                center(b, SkDesign.DANGER);
            }
            case GHOST -> {
                if (hov) b.round(x, y, w, h, rad, SkDesign.HOVER);
                b.roundStroke(x + 0.5f, y + 0.5f, w - 1, h - 1, rad,
                        hov ? SkDesign.BRAND : edge(), 1);
                center(b, SkDesign.BRAND_300);
            }
            default -> {
                b.round(x, y, w, h, rad, hov ? SkDesign.ROW_ON : chipBg());
                b.roundStroke(x + 0.5f, y + 0.5f, w - 1, h - 1, rad, edge(), 1);
                center(b, SkDesign.TEXT);
            }
        }
    }

    private void center(Brush b, int argb) {
        String s = b.ellipsize(10, label.get(), w - 14);
        b.textV(s, x + (w - b.width(10, s)) / 2, y, h, 10, argb);
    }

    @Override
    public boolean click(double mx, double my) {
        onClick.run();
        if (rebuilds) requestRebuild();
        return true;
    }
}
