package net.cone.gui.skija.ui;

import net.cone.gui.skija.SkDesign;

import java.util.Locale;

public final class Header extends Widget {
    private final String text;

    public Header(String text) {
        this.text = text;
    }

    @Override
    public int height() {
        return 32;
    }

    @Override
    public void draw(Brush b, double mx, double my) {
        String s = text.toUpperCase(Locale.ROOT);
        float lx = labelX();
        b.textV(s, lx, y, h, 9, hero ? SkDesign.DEEP_TEXT : SkDesign.MUTED);
        float lineX = lx + b.width(9, s) + 10;
        float lineEnd = x + w - (hero ? 10 : 0);
        if (lineEnd > lineX) b.rect(lineX, y + h / 2f, lineEnd - lineX, 1, edge());
    }
}
