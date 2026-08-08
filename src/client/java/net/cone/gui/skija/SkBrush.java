package net.cone.gui.skija;

import io.github.humbleui.skija.Canvas;
import net.cone.gui.skija.ui.Brush;

final class SkBrush implements Brush {
    private Canvas c;

    SkBrush with(Canvas canvas) {
        this.c = canvas;
        return this;
    }

    @Override
    public void rect(float x, float y, float w, float h, int argb) {
        Sk.fillRect(c, x, y, w, h, argb);
    }

    @Override
    public void round(float x, float y, float w, float h, float r, int argb) {
        Sk.fillRound(c, x, y, w, h, r, argb);
    }

    @Override
    public void roundStroke(float x, float y, float w, float h, float r, int argb, float stroke) {
        Sk.strokeRound(c, x, y, w, h, r, argb, stroke);
    }

    @Override
    public void leaf(float x, float y, float w, float h, float big, float small, int argb) {
        Sk.fillLeaf(c, x, y, w, h, big, small, argb);
    }

    @Override
    public void leafStroke(float x, float y, float w, float h, float big, float small, int argb, float stroke) {
        Sk.strokeLeaf(c, x, y, w, h, big, small, argb, stroke);
    }

    @Override
    public void line(float x1, float y1, float x2, float y2, int argb, float stroke) {
        Sk.line(c, x1, y1, x2, y2, argb, stroke);
    }

    @Override
    public void tri(float x1, float y1, float x2, float y2, float x3, float y3, int argb) {
        Sk.fillTriangle(c, x1, y1, x2, y2, x3, y3, argb);
    }

    @Override
    public void text(String s, float x, float topY, int size, int argb) {
        Sk.text(c, s, x, topY, size, argb);
    }

    @Override
    public void textBold(String s, float x, float topY, int size, int argb) {
        Sk.textBold(c, s, x, topY, size, argb);
    }

    @Override
    public void textV(String s, float x, float y, float h, int size, int argb) {
        Sk.textVCenter(c, s, x, y, h, size, argb);
    }

    @Override
    public void textVBold(String s, float x, float y, float h, int size, int argb) {
        Sk.textVCenterBold(c, s, x, y, h, size, argb);
    }

    @Override
    public float width(int size, String s) {
        return Sk.textWidth(size, s);
    }

    @Override
    public String ellipsize(int size, String s, float maxW) {
        return Sk.ellipsize(size, s, maxW);
    }

    @Override
    public void clip(float x, float y, float w, float h) {
        Sk.clip(c, x, y, w, h);
    }

    @Override
    public void unclip() {
        Sk.unclip(c);
    }
}
