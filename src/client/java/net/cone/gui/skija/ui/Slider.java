package net.cone.gui.skija.ui;

import net.cone.gui.skija.SkDesign;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoubleSupplier;

public final class Slider extends Widget {
    private static final int TRACK_W = 168, TRACK_H = 6;

    private final double min, max, step;
    private final DoubleSupplier get;
    private final DoubleConsumer set;
    private final DoubleFunction<String> fmt;
    private boolean dragging;

    public Slider(String label, double min, double max, double step,
                  DoubleSupplier get, DoubleConsumer set, DoubleFunction<String> fmt) {
        this.searchLabel = label;
        this.min = min;
        this.max = max;
        this.step = step;
        this.get = get;
        this.set = set;
        this.fmt = fmt;
    }

    private float trackX() {
        return x + w - (hero ? 10 : 0) - TRACK_W;
    }

    private float trackY() {
        return y + (h - TRACK_H) / 2f;
    }

    @Override
    public void draw(Brush b, double mx, double my) {
        rowChrome(b);
        drawLabel(b, w - TRACK_W - 60);
        double v = get.getAsDouble();
        float frac = (float) ((v - min) / (max - min));
        frac = Math.max(0, Math.min(1, frac));
        float tx = trackX(), ty = trackY();
        String val = fmt.apply(v);
        b.textV(val, tx - 8 - b.width(10, val), y, h, 10, SkDesign.BRAND_300);
        b.round(tx, ty, TRACK_W, TRACK_H, TRACK_H / 2f, trackOff());
        b.round(tx, ty, TRACK_W * frac, TRACK_H, TRACK_H / 2f, SkDesign.BRAND);
        b.round(tx + TRACK_W * frac - 5, ty + TRACK_H / 2f - 5, 10, 10, 5, SkDesign.WHITE);
    }

    @Override
    public boolean click(double mx, double my) {
        if (!in(mx, my, trackX() - 8, y, TRACK_W + 16, h)) return false;
        dragging = true;
        apply(mx);
        return true;
    }

    @Override
    public boolean drag(double mx, double my) {
        if (!dragging) return false;
        apply(mx);
        return true;
    }

    @Override
    public void release() {
        dragging = false;
    }

    @Override
    public void blur() {
        dragging = false;
    }

    private void apply(double mx) {
        double frac = Math.max(0, Math.min(1, (mx - trackX()) / TRACK_W));
        double v = min + frac * (max - min);
        if (step > 0) v = Math.round(v / step) * step;
        set.accept(Math.max(min, Math.min(max, v)));
    }
}
