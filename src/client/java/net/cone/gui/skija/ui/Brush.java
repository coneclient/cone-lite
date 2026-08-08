package net.cone.gui.skija.ui;

public interface Brush {
    void rect(float x, float y, float w, float h, int argb);

    void round(float x, float y, float w, float h, float r, int argb);

    void roundStroke(float x, float y, float w, float h, float r, int argb, float stroke);

    void leaf(float x, float y, float w, float h, float big, float small, int argb);

    void leafStroke(float x, float y, float w, float h, float big, float small, int argb, float stroke);

    void line(float x1, float y1, float x2, float y2, int argb, float stroke);

    void tri(float x1, float y1, float x2, float y2, float x3, float y3, int argb);

    void text(String s, float x, float topY, int size, int argb);

    void textBold(String s, float x, float topY, int size, int argb);

    void textV(String s, float x, float y, float h, int size, int argb);

    void textVBold(String s, float x, float y, float h, int size, int argb);

    float width(int size, String s);

    String ellipsize(int size, String s, float maxW);

    void clip(float x, float y, float w, float h);

    void unclip();
}
