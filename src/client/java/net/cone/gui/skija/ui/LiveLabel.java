package net.cone.gui.skija.ui;

import net.cone.gui.skija.SkDesign;

import java.util.function.IntSupplier;
import java.util.function.Supplier;

public final class LiveLabel extends Widget {
    private final Supplier<String> text;
    private final IntSupplier color;

    public LiveLabel(Supplier<String> text) {
        this(text, null);
    }

    public LiveLabel(Supplier<String> text, IntSupplier color) {
        this.text = text;
        this.color = color;
    }

    @Override
    public int height() {
        return 20;
    }

    @Override
    public void draw(Brush b, double mx, double my) {
        String s = text.get();
        if (s == null) s = "";
        int c = color != null ? color.getAsInt() : (hero ? SkDesign.DEEP_TEXT : SkDesign.SUB);
        b.textV(b.ellipsize(10, s, w - (hero ? 20 : 0)), labelX(), y, h, 10, c);
    }
}
