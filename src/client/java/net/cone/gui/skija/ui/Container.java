package net.cone.gui.skija.ui;

import java.util.ArrayList;
import java.util.List;

public class Container extends Widget {
    public static final int GAP = 6;

    protected final List<Widget> children = new ArrayList<>();

    public Container add(Widget w) {
        children.add(w);
        return this;
    }

    public List<Widget> children() {
        return children;
    }

    public boolean isEmpty() {
        return children.isEmpty();
    }

    public int size() {
        return children.size();
    }

    public void collect(List<Widget> out) {
        for (Widget c : children) {
            out.add(c);
            if (c instanceof Container ct) ct.collect(out);
        }
    }

    public void setHero(boolean hero) {
        this.hero = hero;
        for (Widget c : children) {
            c.hero = hero;
            if (c instanceof Container ct) ct.setHero(hero);
        }
    }

    @Override
    public int height() {
        int total = 0;
        for (Widget c : children) total += c.height() + GAP;
        return Math.max(0, total - GAP);
    }

    @Override
    public void bounds(int x, int y, int w, int h) {
        super.bounds(x, y, w, h);
        int cy = y;
        for (Widget c : children) {
            c.bounds(x, cy, w, c.height());
            cy += c.height() + GAP;
        }
    }

    @Override
    public void draw(Brush b, double mx, double my) {
        for (Widget c : children) c.draw(b, mx, my);
    }

    @Override
    public boolean click(double mx, double my) {
        for (Widget c : children) {
            if (c.over(mx, my) || c.listening()) {
                if (c.click(mx, my)) return true;
            }
        }
        return false;
    }

    @Override
    public boolean drag(double mx, double my) {
        for (Widget c : children) if (c.drag(mx, my)) return true;
        return false;
    }

    @Override
    public void release() {
        for (Widget c : children) c.release();
    }

    @Override
    public boolean key(int code) {
        for (Widget c : children) {
            if ((c.typing() || c.listening()) && c.key(code)) return true;
        }
        return false;
    }

    @Override
    public boolean typed(String chars) {
        for (Widget c : children) {
            if (c.typing() && c.typed(chars)) return true;
        }
        return false;
    }

    @Override
    public boolean typing() {
        for (Widget c : children) if (c.typing()) return true;
        return false;
    }

    @Override
    public boolean listening() {
        for (Widget c : children) if (c.listening()) return true;
        return false;
    }

    @Override
    public boolean mouseWhileListening(int button) {
        for (Widget c : children) {
            if (c.listening() && c.mouseWhileListening(button)) return true;
        }
        return false;
    }

    @Override
    public void blur() {
        for (Widget c : children) c.blur();
    }
}
