package net.cone.gui.skija.ui;

import net.cone.gui.skija.SkDesign;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public final class Checkbox extends Widget {
    private static final int BOX = 14;

    private final BooleanSupplier get;
    private final Consumer<Boolean> set;

    public Checkbox(String label, BooleanSupplier get, Consumer<Boolean> set) {
        this.searchLabel = label;
        this.get = get;
        this.set = set;
    }

    @Override
    public int height() {
        return 24;
    }

    @Override
    public void draw(Brush b, double mx, double my) {
        rowChrome(b);
        drawLabel(b, w - BOX - 16);
        boolean on = get.getAsBoolean();
        float bx = x + w - (hero ? 10 : 0) - BOX;
        float by = y + (h - BOX) / 2f;
        b.round(bx, by, BOX, BOX, 4, on ? SkDesign.BRAND : chipBg());
        if (!on) b.roundStroke(bx + 0.5f, by + 0.5f, BOX - 1, BOX - 1, 4, edge(), 1);
        if (on) {
            int ink = 0xFF0F0F0F;
            b.line(bx + 3.5f, by + 7.2f, bx + 6, by + 10, ink, 1.6f);
            b.line(bx + 6, by + 10, bx + 10.6f, by + 4.4f, ink, 1.6f);
        }
    }

    @Override
    public boolean click(double mx, double my) {
        set.accept(!get.getAsBoolean());
        return true;
    }
}
