package net.cone.gui.hud;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.cone.config.ConfigManager;
import net.cone.config.ConeConfig;
import net.cone.gui.Theme;
import net.cone.gui.Ui;

public final class HudEditScreen extends Screen {
    private static final int SNAP = 8;
    private static final int MARGIN = 4;

    private enum Elem { STATUS, BAZAAR }

    private final Screen parent;
    private final int[] statusWH = new int[2];
    private final int[] bazaarWH = new int[2];
    private Elem dragging;
    private int grabX, grabY;

    public HudEditScreen(Screen parent) {
        super(Component.literal("Move HUD"));
        this.parent = parent;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
        ConeConfig c = ConfigManager.get();
        g.fill(0, 0, width, height, 0xC00A0B0D);

        int[] s = ConeHud.drawScaled(g, getFont(), c.hudX, c.hudY, true);
        statusWH[0] = s[0]; statusWH[1] = s[1];
        outline(g, c.hudX, c.hudY, statusWH, mouseX, mouseY, dragging == Elem.STATUS);

        if (c.bazaarHudEnabled) {
            int[] b = BazaarHud.drawPreview(g, getFont(), c.bazaarHudX, c.bazaarHudY);
            bazaarWH[0] = b[0]; bazaarWH[1] = b[1];
            outline(g, c.bazaarHudX, c.bazaarHudY, bazaarWH, mouseX, mouseY, dragging == Elem.BAZAAR);
        }

        String hint = "Drag the cards   ·   scroll to resize status   ·   snaps to edges   ·   Esc to save";
        int hw = Ui.width(getFont(), hint);
        Ui.roundRect(g, width / 2 - hw / 2 - 8, height - 30, hw + 16, 18, 4, Theme.PANEL_2);
        Ui.text(g, getFont(), hint, width / 2 - hw / 2, height - 25, Theme.SUBTEXT);
    }

    private void outline(GuiGraphicsExtractor g, int x, int y, int[] wh, int mx, int my, boolean active) {
        boolean over = in(x, y, wh, mx, my);
        int col = active ? Theme.ACCENT : (over ? Theme.ACCENT_2 : Theme.LINE);
        Theme.border(g, x - 1, y - 1, wh[0] + 2, wh[1] + 2, col);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        if (dy == 0) return super.mouseScrolled(mx, my, dx, dy);
        ConeConfig c = ConfigManager.get();
        float next = c.hudScale + (float) dy * 0.05f;
        c.hudScale = Math.max(0.5f, Math.min(2.5f, Math.round(next * 100f) / 100f));
        return true;
    }

    private boolean in(int x, int y, int[] wh, double mx, double my) {
        return mx >= x && mx <= x + wh[0] && my >= y && my <= y + wh[1];
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        ConeConfig c = ConfigManager.get();
        if (event.button() == 0) {
            if (c.bazaarHudEnabled && in(c.bazaarHudX, c.bazaarHudY, bazaarWH, event.x(), event.y())) {
                dragging = Elem.BAZAAR;
                grabX = (int) event.x() - c.bazaarHudX;
                grabY = (int) event.y() - c.bazaarHudY;
                return true;
            }
            if (in(c.hudX, c.hudY, statusWH, event.x(), event.y())) {
                dragging = Elem.STATUS;
                grabX = (int) event.x() - c.hudX;
                grabY = (int) event.y() - c.hudY;
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (dragging == null) return super.mouseDragged(event, dragX, dragY);
        ConeConfig c = ConfigManager.get();
        int[] wh = dragging == Elem.BAZAAR ? bazaarWH : statusWH;
        int nx = clampSnap((int) event.x() - grabX, wh[0], width);
        int ny = clampSnap((int) event.y() - grabY, wh[1], height);
        if (dragging == Elem.BAZAAR) { c.bazaarHudX = nx; c.bazaarHudY = ny; }
        else { c.hudX = nx; c.hudY = ny; }
        return true;
    }

    private int clampSnap(int v, int size, int screen) {
        v = Math.max(0, Math.min(v, screen - size));
        if (v <= SNAP) v = MARGIN;
        if (v >= screen - size - SNAP) v = screen - size - MARGIN;
        return v;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        dragging = null;
        return super.mouseReleased(event);
    }

    @Override
    public void onClose() {
        ConfigManager.save();
        if (minecraft != null) minecraft.setScreen(parent);
    }
}
