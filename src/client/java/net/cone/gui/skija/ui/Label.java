package net.cone.gui.skija.ui;

import net.cone.gui.skija.SkDesign;

public final class Label extends Widget {
    public enum Tone { TEXT, SUB, MUTED }

    private final String text;
    private final Tone tone;

    public Label(String text, Tone tone) {
        this.text = text;
        this.tone = tone;
    }

    @Override
    public int height() {
        return 20;
    }

    @Override
    public void draw(Brush b, double mx, double my) {
        int color = switch (tone) {
            case TEXT -> SkDesign.TEXT;
            case SUB -> hero ? SkDesign.DEEP_TEXT : SkDesign.SUB;
            case MUTED -> hero ? SkDesign.DEEP_TEXT : SkDesign.MUTED;
        };
        b.textV(b.ellipsize(10, text, w - (hero ? 20 : 0)), labelX(), y, h, 10, color);
    }
}
