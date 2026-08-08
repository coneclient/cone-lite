package net.cone.gui.skija.ui;

import net.cone.gui.skija.SkDesign;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoubleSupplier;

public final class RangeSlider extends Widget {
    private static final int TRACK_W = 104, TRACK_H = 4;

    private final double min, max, step;
    private final DoubleSupplier getLo, getHi;
    private final DoubleConsumer setLo, setHi;
    private final DoubleFunction<String> fmt;
    private int dragging = -1;

    public RangeSlider(String label, double min, double max, double step,
                       DoubleSupplier getLo, DoubleConsumer setLo,
                       DoubleSupplier getHi, DoubleConsumer setHi,
                       DoubleFunction<String> fmt) {
        this.searchLabel = label;
        this.min = min;
        this.max = max;
        this.step = step;
        this.getLo = getLo;
        this.setLo = setLo;
        this.getHi = getHi;
        this.setHi = setHi;
        this.fmt = fmt;
    }

    private float trackX() {
        return x + w - (hero ? 10 : 0) - TRACK_W;
    }

    private float fracOf(double v) {
        return (float) Math.max(0, Math.min(1, (v - min) / (max - min)));
    }

    @Override
    public void draw(Brush b, double mx, double my) {
        rowChrome(b);
        drawLabel(b, w - TRACK_W - 84);
        double lo = getLo.getAsDouble(), hi = getHi.getAsDouble();
        float tx = trackX(), ty = y + (h - TRACK_H) / 2f;
        float f0 = fracOf(lo), f1 = fracOf(hi);
        String val = fmt.apply(lo) + " - " + fmt.apply(hi);
        b.textV(val, tx - 8 - b.width(10, val), y, h, 10, SkDesign.BRAND_300);
        b.round(tx, ty, TRACK_W, TRACK_H, TRACK_H / 2f, trackOff());
        b.round(tx + TRACK_W * f0, ty, TRACK_W * Math.max(0, f1 - f0), TRACK_H, TRACK_H / 2f, SkDesign.BRAND);
        b.round(tx + TRACK_W * f0 - 4, ty + TRACK_H / 2f - 4, 8, 8, 4, SkDesign.WHITE);
        b.round(tx + TRACK_W * f1 - 4, ty + TRACK_H / 2f - 4, 8, 8, 4, SkDesign.WHITE);
    }

    @Override
    public boolean click(double mx, double my) {
        if (!in(mx, my, trackX() - 8, y, TRACK_W + 16, h)) return false;
        float tx = trackX();
        double lox = tx + TRACK_W * fracOf(getLo.getAsDouble());
        double hix = tx + TRACK_W * fracOf(getHi.getAsDouble());
        dragging = Math.abs(mx - lox) <= Math.abs(mx - hix) ? 0 : 1;
        apply(mx);
        return true;
    }

    @Override
    public boolean drag(double mx, double my) {
        if (dragging < 0) return false;
        apply(mx);
        return true;
    }

    @Override
    public void release() {
        dragging = -1;
    }

    @Override
    public void blur() {
        dragging = -1;
    }

    private void apply(double mx) {
        double frac = Math.max(0, Math.min(1, (mx - trackX()) / TRACK_W));
        double v = min + frac * (max - min);
        if (step > 0) v = Math.round(v / step) * step;
        v = Math.max(min, Math.min(max, v));
        if (dragging == 0) setLo.accept(Math.min(v, getHi.getAsDouble()));
        else setHi.accept(Math.max(v, getLo.getAsDouble()));
    }
}
