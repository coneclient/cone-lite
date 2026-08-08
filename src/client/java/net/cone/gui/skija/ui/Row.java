package net.cone.gui.skija.ui;

import java.util.ArrayList;
import java.util.List;

public final class Row extends Container {
    private static final int HGAP = 6;

    private final List<Float> weights = new ArrayList<>();

    public Row add(Widget w, float weight) {
        children.add(w);
        weights.add(weight);
        return this;
    }

    @Override
    public Row add(Widget w) {
        return add(w, 1f);
    }

    @Override
    public int height() {
        int max = 0;
        for (Widget c : children) max = Math.max(max, c.height());
        return max;
    }

    @Override
    public void bounds(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        float total = 0;
        for (float wt : weights) total += wt;
        if (total <= 0) return;
        int avail = w - HGAP * (children.size() - 1);
        int cx = x;
        for (int i = 0; i < children.size(); i++) {
            Widget c = children.get(i);
            int cw = Math.round(avail * (weights.get(i) / total));
            c.bounds(cx, y + (h - c.height()) / 2, cw, c.height());
            cx += cw + HGAP;
        }
    }
}
