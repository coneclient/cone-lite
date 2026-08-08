package net.cone.gui.skija.ui;

import net.cone.gui.skija.SkDesign;
import org.lwjgl.glfw.GLFW;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoubleSupplier;

public final class NumberBox extends Widget {
    private static final int STEP_W = 18, VAL_W = 46, BH = 18;

    private final double min, max, step;
    private final DoubleSupplier get;
    private final DoubleConsumer set;
    private final DoubleFunction<String> fmt;
    private String buffer;

    public NumberBox(String label, double min, double max, double step,
                     DoubleSupplier get, DoubleConsumer set, DoubleFunction<String> fmt) {
        this.searchLabel = label;
        this.min = min;
        this.max = max;
        this.step = step;
        this.get = get;
        this.set = set;
        this.fmt = fmt;
    }

    private float groupX() {
        return x + w - (hero ? 10 : 0) - (2 * STEP_W + VAL_W + 4);
    }

    @Override
    public void draw(Brush b, double mx, double my) {
        rowChrome(b);
        drawLabel(b, w - (2 * STEP_W + VAL_W + 24));
        float gx = groupX(), gy = y + (h - BH) / 2f;

        stepper(b, gx, gy, "-", mx, my);
        float vx = gx + STEP_W + 2;
        boolean editing = buffer != null;
        b.round(vx, gy, VAL_W, BH, 5, chipBg());
        b.roundStroke(vx + 0.5f, gy + 0.5f, VAL_W - 1, BH - 1, 5, editing ? SkDesign.BRAND : edge(), 1);
        String shown = editing ? buffer + "_" : fmt.apply(get.getAsDouble());
        b.textV(b.ellipsize(10, shown, VAL_W - 10),
                vx + (VAL_W - Math.min(VAL_W - 10, b.width(10, shown))) / 2, gy, BH, 10,
                editing ? SkDesign.TEXT : SkDesign.BRAND_300);
        stepper(b, vx + VAL_W + 2, gy, "+", mx, my);
    }

    private void stepper(Brush b, float sx, float sy, String sign, double mx, double my) {
        boolean hov = in(mx, my, sx, sy, STEP_W, BH);
        b.round(sx, sy, STEP_W, BH, 5, hov ? SkDesign.ROW_ON : chipBg());
        b.roundStroke(sx + 0.5f, sy + 0.5f, STEP_W - 1, BH - 1, 5, edge(), 1);
        b.textV(sign, sx + (STEP_W - b.width(11, sign)) / 2, sy, BH, 11, SkDesign.TEXT);
    }

    @Override
    public boolean click(double mx, double my) {
        float gx = groupX(), gy = y + (h - BH) / 2f;
        if (in(mx, my, gx, gy, STEP_W, BH)) {
            commit();
            nudge(-step);
            return true;
        }
        if (in(mx, my, gx + STEP_W + VAL_W + 4, gy, STEP_W, BH)) {
            commit();
            nudge(step);
            return true;
        }
        if (in(mx, my, gx + STEP_W + 2, gy, VAL_W, BH)) {
            if (buffer == null) buffer = "";
            return true;
        }
        commit();
        return false;
    }

    private void nudge(double by) {
        set.accept(Math.max(min, Math.min(max, get.getAsDouble() + by)));
    }

    @Override
    public boolean typing() {
        return buffer != null;
    }

    @Override
    public boolean key(int code) {
        if (buffer == null) return false;
        switch (code) {
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (!buffer.isEmpty()) buffer = buffer.substring(0, buffer.length() - 1);
                return true;
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                commit();
                return true;
            }
            case GLFW.GLFW_KEY_ESCAPE -> {
                buffer = null;
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    @Override
    public boolean typed(String chars) {
        if (buffer == null) return false;
        for (char ch : chars.toCharArray()) {
            if ((ch >= '0' && ch <= '9') || ch == '.' || ch == '-'
                    || ch == 'k' || ch == 'K' || ch == 'm' || ch == 'M' || ch == 'b' || ch == 'B') {
                buffer += ch;
            }
        }
        return true;
    }

    private static double parseSuffixed(String raw) {
        String t = raw.trim();
        if (t.isEmpty()) throw new NumberFormatException("empty");
        double mult = 1;
        char last = t.charAt(t.length() - 1);
        if (last == 'k' || last == 'K') mult = 1_000;
        else if (last == 'm' || last == 'M') mult = 1_000_000;
        else if (last == 'b' || last == 'B') mult = 1_000_000_000;
        if (mult != 1) t = t.substring(0, t.length() - 1);
        if (t.isEmpty() || t.equals("-")) throw new NumberFormatException("no digits");
        return Double.parseDouble(t) * mult;
    }

    private void commit() {
        if (buffer == null) return;
        try {
            double v = parseSuffixed(buffer);
            set.accept(Math.max(min, Math.min(max, v)));
        } catch (NumberFormatException ignored) {
        }
        buffer = null;
    }

    @Override
    public void blur() {
        commit();
    }
}
