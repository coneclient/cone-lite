package net.cone.gui.skija.ui;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public final class Toggle extends Widget {
    private static final int PW = 44, PH = 22;

    private final BooleanSupplier get;
    private final Consumer<Boolean> set;

    private final boolean rebuilds;

    public Toggle(String label, BooleanSupplier get, Consumer<Boolean> set) {
        this(label, get, set, false);
    }

    public Toggle(String label, BooleanSupplier get, Consumer<Boolean> set, boolean rebuilds) {
        this.searchLabel = label;
        this.get = get;
        this.set = set;
        this.rebuilds = rebuilds;
    }

    @Override
    public void draw(Brush b, double mx, double my) {
        rowChrome(b);
        drawLabel(b, w - PW - 16);
        pill(b, x + w - (hero ? 10 : 0) - PW, y + (h - PH) / 2f, PW, PH, get.getAsBoolean());
    }

    @Override
    public boolean click(double mx, double my) {
        set.accept(!get.getAsBoolean());
        if (rebuilds) requestRebuild();
        return true;
    }
}
