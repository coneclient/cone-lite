package net.cone.gui.skija.ui;

import net.cone.gui.skija.SkDesign;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

public final class SearchField extends Widget {
    private final Consumer<String> onChange;
    private String query = "";
    private boolean focused;

    public SearchField(Consumer<String> onChange) {
        this.onChange = onChange;
    }

    public String query() {
        return query;
    }

    public boolean focused() {
        return focused;
    }

    public void focus() {
        focused = true;
    }

    public void clear() {
        query = "";
        focused = false;
        onChange.accept(query);
    }

    @Override
    public void draw(Brush b, double mx, double my) {
        float rad = h / 2f;
        b.round(x, y, w, h, rad, SkDesign.INSET);
        b.roundStroke(x + 0.5f, y + 0.5f, w - 1, h - 1, rad,
                focused ? SkDesign.BRAND : SkDesign.CARD_EDGE, 1);

        float ix = x + 10, iy = y + h / 2f;
        b.roundStroke(ix, iy - 4, 7, 7, 3.5f, SkDesign.MUTED, 1.2f);
        b.line(ix + 6.5f, iy + 2.5f, ix + 9, iy + 5, SkDesign.MUTED, 1.2f);

        boolean empty = query.isEmpty();
        String shown = empty ? "Search" : query;
        shown = b.ellipsize(11, shown, w - 34);
        if (focused) shown = empty ? "_" : shown + "_";
        b.textV(shown, x + 24, y, h, 11, empty && !focused ? SkDesign.MUTED : SkDesign.TEXT);
    }

    @Override
    public boolean click(double mx, double my) {
        focused = true;
        return true;
    }

    @Override
    public boolean typing() {
        return focused;
    }

    @Override
    public boolean key(int code) {
        if (!focused) return false;
        switch (code) {
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (!query.isEmpty()) {
                    query = query.substring(0, query.length() - 1);
                    onChange.accept(query);
                }
                return true;
            }
            case GLFW.GLFW_KEY_ESCAPE -> {
                clear();
                return true;
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                focused = false;
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    @Override
    public boolean typed(String chars) {
        if (!focused) return false;
        query += chars;
        onChange.accept(query);
        return true;
    }

    @Override
    public void blur() {
        focused = false;
    }
}
