package net.cone.gui.skija.ui;

import net.cone.gui.skija.SkDesign;
import org.lwjgl.glfw.GLFW;

import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import java.util.function.Supplier;

public final class KeybindButton extends Widget {
    private static final int CHIP_W = 96, CHIP_H = 18;

    private final Supplier<String> keyName;

    private final IntConsumer assignKey;

    private final IntPredicate assignMouse;
    private boolean listening;

    public KeybindButton(String label, Supplier<String> keyName,
                         IntConsumer assignKey, IntPredicate assignMouse) {
        this.searchLabel = label;
        this.keyName = keyName;
        this.assignKey = assignKey;
        this.assignMouse = assignMouse;
    }

    @Override
    public void draw(Brush b, double mx, double my) {
        rowChrome(b);
        drawLabel(b, w - CHIP_W - 16);
        float cx = x + w - (hero ? 10 : 0) - CHIP_W, cy = y + (h - CHIP_H) / 2f;
        float rad = CHIP_H / 2f;
        b.round(cx, cy, CHIP_W, CHIP_H, rad,
                listening ? (SkDesign.BRAND & 0x00FFFFFF) | 0x2E000000 : chipBg());
        b.roundStroke(cx + 0.5f, cy + 0.5f, CHIP_W - 1, CHIP_H - 1, rad,
                listening ? SkDesign.BRAND : edge(), 1);
        String s = listening ? "press a key..." : keyName.get();
        s = b.ellipsize(10, s, CHIP_W - 12);
        b.textV(s, cx + (CHIP_W - b.width(10, s)) / 2, cy, CHIP_H, 10,
                listening ? SkDesign.BRAND_300 : SkDesign.TEXT);
    }

    @Override
    public boolean click(double mx, double my) {
        listening = true;
        return true;
    }

    @Override
    public boolean listening() {
        return listening;
    }

    @Override
    public boolean key(int code) {
        if (!listening) return false;
        listening = false;
        if (code == GLFW.GLFW_KEY_ESCAPE) return true;
        if (code == GLFW.GLFW_KEY_BACKSPACE || code == GLFW.GLFW_KEY_DELETE) {
            assignKey.accept(-1);
            return true;
        }
        assignKey.accept(code);
        return true;
    }

    @Override
    public boolean mouseWhileListening(int button) {
        listening = false;
        return assignMouse != null && assignMouse.test(button);
    }

    @Override
    public void blur() {
        listening = false;
    }
}
