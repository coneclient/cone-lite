package net.cone.gui.skija;

import io.github.humbleui.skija.impl.Library;

final class SkijaNative {
    private SkijaNative() {}

    static void load() {
        Library.staticLoad();
    }
}
