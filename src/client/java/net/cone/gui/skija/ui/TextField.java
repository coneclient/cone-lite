package net.cone.gui.skija.ui;

import net.cone.gui.skija.SkDesign;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class TextField extends Widget {
    private static final int CHIP_W = 148, CHIP_H = 18;

    private final String placeholder;
    private final Supplier<String> get;
    private final Consumer<String> set;
    private final boolean fill;
    private boolean editing;

    public TextField(String label, String placeholder, Supplier<String> get, Consumer<String> set) {
        this(label, placeholder, get, set, false);
    }

    public TextField(String label, String placeholder, Supplier<String> get, Consumer<String> set, boolean fill) {
        this.searchLabel = label;
        this.placeholder = placeholder;
        this.get = get;
        this.set = set;
        this.fill = fill;
    }

    private float chipX() {
        return fill ? labelX() : x + w - (hero ? 10 : 0) - chipW();
    }

    private float chipW() {
        return fill ? w - (hero ? 20 : 0) : Math.min(CHIP_W, w / 2f);
    }

    @Override
    public void draw(Brush b, double mx, double my) {
        rowChrome(b);
        if (!fill) drawLabel(b, w - chipW() - 16);
        float cx = chipX(), cw = chipW(), cy = y + (h - CHIP_H) / 2f;
        float rad = CHIP_H / 2f;
        b.round(cx, cy, cw, CHIP_H, rad, chipBg());
        b.roundStroke(cx + 0.5f, cy + 0.5f, cw - 1, CHIP_H - 1, rad,
                editing ? SkDesign.BRAND : edge(), 1);
        String val = get.get();
        boolean empty = val == null || val.isEmpty();
        String shown = empty ? placeholder : val;

        while (editing && b.width(10, shown + "_") > cw - 16 && shown.length() > 1) {
            shown = shown.substring(1);
        }
        if (!editing) shown = b.ellipsize(10, shown, cw - 16);
        if (editing) shown = (empty ? "" : shown) + "_";
        b.textV(shown, cx + 8, cy, CHIP_H, 10, empty && !editing ? SkDesign.MUTED : SkDesign.TEXT);
    }

    @Override
    public boolean click(double mx, double my) {
        editing = in(mx, my, chipX(), y, chipW(), h) || fill;
        return editing;
    }

    @Override
    public boolean typing() {
        return editing;
    }

    @Override
    public boolean key(int code) {
        if (!editing) return false;
        switch (code) {
            case GLFW.GLFW_KEY_BACKSPACE -> {
                String v = get.get();
                if (v != null && !v.isEmpty()) set.accept(v.substring(0, v.length() - 1));
                return true;
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER, GLFW.GLFW_KEY_ESCAPE -> {
                editing = false;
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    @Override
    public boolean typed(String chars) {
        if (!editing) return false;
        String v = get.get();
        set.accept((v == null ? "" : v) + chars);
        return true;
    }

    @Override
    public void blur() {
        editing = false;
    }
}
