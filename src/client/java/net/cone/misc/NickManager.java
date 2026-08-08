package net.cone.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.cone.config.ConfigManager;

import java.util.Optional;

public final class NickManager {
    private NickManager() {}

    private static String realName() {
        var profile = Minecraft.getInstance().getGameProfile();
        return profile == null ? null : profile.name();
    }

    public static Component rewrite(Component in) {
        if (in == null) return null;
        var c = ConfigManager.get();
        if (!c.nickEnabled || c.nickName == null || c.nickName.isBlank()) return in;
        String real = realName();
        if (real == null || real.isEmpty()) return in;
        String flat = in.getString();
        if (flat == null || !flat.contains(real)) return in;

        MutableComponent out = Component.empty();
        in.visit((style, str) -> {
            out.append(Component.literal(str.replace(real, c.nickName)).setStyle(style));
            return Optional.empty();
        }, Style.EMPTY);
        return out;
    }
}
