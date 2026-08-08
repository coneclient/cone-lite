package net.cone.gui.skija;

import net.cone.config.ConfigManager;
import net.cone.gui.Theme;
import net.cone.gui.Ui;
import net.cone.gui.skija.ui.Widget;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class SkijaScreen extends Screen {
    private final SkijaState state = new SkijaState();

    public SkijaScreen() {
        super(Component.literal("Cone"));
        SkijaLib.ensureStarted();
        SkDesign.refresh(ConfigManager.get());
        SkijaRender.invalidate();
    }

    SkijaState state() {
        return state;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
        switch (SkijaLib.state()) {
            case READY -> renderReady(g, mouseX, mouseY);
            case FAILED -> renderFallback(g, "Renderer unavailable", SkijaLib.message());
            default -> {
                SkijaLib.ensureStarted();
                renderFallback(g, "Loading renderer…", SkijaLib.message());
            }
        }
    }

    private void renderReady(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        if (SkDesign.FROST) {
            try {
                extractBlurredBackground(g);
            } catch (IllegalStateException e) {
            }
        }

        g.fill(0, 0, width, height, SkDesign.SCRIM);
        SkijaRender.render(g, this, mouseX, mouseY);
    }

    @Override
    protected void init() {
        SkijaRender.invalidate();
    }

    private float dens() {
        return SkDesign.density(net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScale());
    }

    private SkijaLayout.Frame frame() {
        float d = dens();
        return SkijaLayout.compute(state, (int) Math.ceil(width * d), (int) Math.ceil(height * d));
    }

    private void renderFallback(GuiGraphicsExtractor g, String title, String detail) {
        Font font = getFont();
        g.fill(0, 0, width, height, SkDesign.SCRIM);
        int w = Math.min(420, width - 40), h = 120;
        int x = (width - w) / 2, y = (height - h) / 2;
        Ui.shadow(g, x, y, w, h, 6);
        Ui.roundRect(g, x, y, w, h, 6, SkDesign.CARD);
        Theme.border(g, x, y, w, h, SkDesign.CARD_EDGE);
        Ui.text(g, font, title, x + 18, y + 22, SkDesign.TEXT);
        for (String line : Ui.wrap(font, detail, w - 36)) {
            Ui.text(g, font, line, x + 18, y + 44, SkDesign.SUB);
            break;
        }
        Ui.text(g, font, "Press Esc to close", x + 18, y + h - 24, SkDesign.MUTED);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        SkijaRender.invalidate();
        if (SkijaLib.state() != SkijaLib.State.READY) return super.mouseClicked(event, doubleClick);
        float d = dens();
        double mx = event.x() * d, my = event.y() * d;
        int button = event.button();
        SkijaLayout.Frame f = frame();

        for (SkijaModel.Module m : state.visibleModules()) {
            if (m.body.listening()) {
                m.body.mouseWhileListening(button);
                afterClick();
                return true;
            }
        }

        if ((button == 1 || button == 2) && f.content.has(mx, my) && !f.dash) {
            if (qolClick(f, mx, my, button)) return true;
        }
        if (button != 0) return super.mouseClicked(event, doubleClick);

        if (f.search.has(mx, my)) {
            state.blurAll();
            state.search.click(mx, my);
            return true;
        }
        state.search.blur();

        for (SkijaLayout.SideRow row : f.sideRows) {
            if (row.box.has(mx, my)) {
                state.selectCategory(row.index);
                return true;
            }
        }

        if (f.appearanceBtn.has(mx, my)) {
            state.selectCategory(state.categories.size() - 1);
            return true;
        }

        if (f.stopPill.has(mx, my) && net.cone.core.ConeCore.activeTaskName() != null) {
            net.cone.core.ConeCore.stopTasks();
            return true;
        }

        if (f.content.has(mx, my)) {
            if (f.dash) {
                if (f.dashSurprise != null && f.dashSurprise.has(mx, my)) {
                    SkijaDash.surprise();
                    return true;
                }
                for (SkijaLayout.DashBtn b : f.dashBtns) {
                    if (b.box.has(mx, my)) {
                        b.btn.run().run();
                        state.rebuild();
                        return true;
                    }
                }
            } else {
                for (SkijaLayout.Card card : f.cards) {
                    if (clickCard(card, mx, my)) {
                        afterClick();
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private boolean qolClick(SkijaLayout.Frame f, double mx, double my, int button) {
        for (SkijaLayout.Card card : f.cards) {
            if (!card.expanded || card.module.body.isEmpty()) continue;
            java.util.List<Widget> all = new java.util.ArrayList<>();
            card.module.body.collect(all);
            for (Widget w : all) {
                if (!w.over(mx, my)) continue;
                if (button == 1 && w.reset != null) {
                    w.reset.run();
                    return true;
                }
                if (button == 2 && w.pinId != null) {
                    var pins = ConfigManager.get().uiPinned;
                    if (!pins.remove(w.pinId)) pins.add(w.pinId);
                    ConfigManager.save();
                    state.rebuild();
                    return true;
                }
            }
        }
        return false;
    }

    private boolean clickCard(SkijaLayout.Card card, double mx, double my) {
        if (card.pill != null && grow(card.pill, 3).has(mx, my)) {
            card.module.toggle.run();

            SkDesign.refresh(ConfigManager.get());
            state.rebuild();
            return true;
        }
        if (card.expanded && !card.module.body.isEmpty() && card.module.body.over(mx, my)) {
            state.blurAll();
            if (card.module.body.click(mx, my)) {
                state.dragging = card.module.body;
                state.dragDens = dens();
                return true;
            }
        }
        if (card.head.has(mx, my)) {
            if (card.module.body.isEmpty()) {
                if (card.module.toggle != null) {
                    card.module.toggle.run();
                    state.rebuild();
                }
                return true;
            }
            state.expandedId = card.expanded ? null : card.module.id;
            return true;
        }
        return false;
    }

    private void afterClick() {
        if (Widget.consumeRebuild()) state.rebuild();
    }

    private static SkijaLayout.R grow(SkijaLayout.R r, int by) {
        return new SkijaLayout.R(r.x() - by, r.y() - by, r.w() + 2 * by, r.h() + 2 * by);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        SkijaRender.invalidate();
        if (state.dragging != null) {
            state.dragging.drag(event.x() * state.dragDens, event.y() * state.dragDens);
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        SkijaRender.invalidate();
        if (state.dragging != null) {
            state.dragging.release();
            state.dragging = null;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        SkijaRender.invalidate();
        if (SkijaLib.state() != SkijaLib.State.READY) return false;
        float d = dens();
        SkijaLayout.Frame f = frame();
        if (f.main.has(mx * d, my * d)) {
            state.scroll = (float) Math.max(0, Math.min(f.scrollMax, state.scroll - dy * 32));
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        SkijaRender.invalidate();
        int key = event.key();

        long win = minecraft.getWindow().handle();
        boolean ctrl = GLFW.glfwGetKey(win, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(win, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(win, GLFW.GLFW_KEY_LEFT_SUPER) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(win, GLFW.GLFW_KEY_RIGHT_SUPER) == GLFW.GLFW_PRESS;
        if (ctrl && key == GLFW.GLFW_KEY_F) {
            state.blurAll();
            state.search.focus();
            return true;
        }

        if (state.search.key(key)) return true;
        for (SkijaModel.Module m : state.visibleModules()) {
            if ((m.body.typing() || m.body.listening()) && m.body.key(key)) {
                afterClick();
                return true;
            }
        }

        if (!state.anyTyping() && SkijaLib.state() == SkijaLib.State.READY) {
            SkijaLayout.Frame f = frame();
            switch (key) {
                case GLFW.GLFW_KEY_UP -> { nudgeScroll(f, -32); return true; }
                case GLFW.GLFW_KEY_DOWN -> { nudgeScroll(f, 32); return true; }
                case GLFW.GLFW_KEY_PAGE_UP -> { nudgeScroll(f, -f.content.h()); return true; }
                case GLFW.GLFW_KEY_PAGE_DOWN -> { nudgeScroll(f, f.content.h()); return true; }
                case GLFW.GLFW_KEY_HOME -> { state.scroll = 0; return true; }
                case GLFW.GLFW_KEY_END -> { state.scroll = f.scrollMax; return true; }
                default -> { }
            }
        }
        return super.keyPressed(event);
    }

    private void nudgeScroll(SkijaLayout.Frame f, int by) {
        state.scroll = Math.max(0, Math.min(f.scrollMax, state.scroll + by));
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        SkijaRender.invalidate();
        String chars = event.codepointAsString();
        if (state.search.typed(chars)) return true;
        for (SkijaModel.Module m : state.visibleModules()) {
            if (m.body.typing() && m.body.typed(chars)) return true;
        }
        return super.charTyped(event);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return !state.anyTyping();
    }
}
