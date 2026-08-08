package net.cone.gui.skija.ui;

import net.cone.gui.skija.SkDesign;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class Select extends Widget {
    private static final int CHIP_W = 76, CHIP_H = 18;

    private final List<String> options;
    private final Supplier<String> get;
    private final Consumer<String> set;

    public Select(String label, List<String> options, Supplier<String> get, Consumer<String> set) {
        this.searchLabel = label;
        this.options = options;
        this.get = get;
        this.set = set;
    }

    private float chipW() {
        return Math.min(Math.max(CHIP_W, w / 3f), w / 2f);
    }

    @Override
    public void draw(Brush b, double mx, double my) {
        rowChrome(b);
        float cw = chipW();
        drawLabel(b, w - cw - 16);
        float cx = x + w - (hero ? 10 : 0) - cw, cy = y + (h - CHIP_H) / 2f;
        float rad = CHIP_H / 2f;
        b.round(cx, cy, cw, CHIP_H, rad, chipBg());
        b.roundStroke(cx + 0.5f, cy + 0.5f, cw - 1, CHIP_H - 1, rad, edge(), 1);
        String val = b.ellipsize(10, String.valueOf(get.get()), cw - 12);
        b.textV(val, cx + (cw - b.width(10, val)) / 2, cy, CHIP_H, 10, SkDesign.TEXT);
    }

    @Override
    public boolean click(double mx, double my) {
        if (options.isEmpty()) return true;
        int i = options.indexOf(get.get());
        set.accept(options.get((i + 1) % options.size()));
        return true;
    }
}
