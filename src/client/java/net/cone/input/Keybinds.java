package net.cone.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.cone.ConeClient;
import net.cone.config.ConfigManager;
import net.cone.core.ConeCore;
import net.cone.gui.skija.SkijaScreen;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class Keybinds {
    public record Bind(String label, KeyMapping mapping) {}

    private static final List<Bind> BINDS = new ArrayList<>();

    private static KeyMapping openMenu;
    private static KeyMapping dumpSidebarKey;

    private static final java.util.concurrent.atomic.AtomicBoolean openMenuRequest =
            new java.util.concurrent.atomic.AtomicBoolean();

    private Keybinds() {}

    public static void requestOpenMenu() {
        openMenuRequest.set(true);
    }

    public static List<Bind> binds() {
        return BINDS;
    }

    public static void rebind(KeyMapping mapping, InputConstants.Key key) {
        mapping.setKey(key);
        KeyMapping.resetMapping();
        Minecraft.getInstance().options.save();
    }

    public static void register() {
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(ConeClient.MOD_ID, "main"));

        openMenu = reg("Open menu", "key.cone.open_menu", GLFW.GLFW_KEY_RIGHT_SHIFT, category);
        dumpSidebarKey = reg("Dump sidebar (debug)", "key.cone.dump_sidebar", GLFW.GLFW_KEY_UNKNOWN, category);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            boolean open = openMenuRequest.compareAndSet(true, false);
            while (openMenu.consumeClick()) open = true;

            if (open) client.setScreen(new SkijaScreen());

            while (dumpSidebarKey.consumeClick()) dumpSidebar(client);
        });
    }

    private static void dumpSidebar(Minecraft mc) {
        java.util.List<String> lines = ConeCore.skyblock().debugDump(mc);
        for (String l : lines) ConeClient.LOG.info("[Cone][dump] {}", l);
        try {
            java.nio.file.Path dir =
                    net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("cone");
            java.nio.file.Files.createDirectories(dir);
            java.nio.file.Path f = dir.resolve("debug_dump.txt");
            java.nio.file.Files.write(f, lines);
            chat(mc, "§b[Cone] §7dumped " + lines.size() + " lines → config/cone/debug_dump.txt");
        } catch (Exception e) {
            chat(mc, "§c[Cone] dump failed: " + e.getMessage());
        }
    }

    private static void chat(Minecraft mc, String msg) {
        if (mc.player != null) mc.player.sendSystemMessage(Component.literal(msg));
    }

    private static KeyMapping reg(String label, String key, int glfwKey, KeyMapping.Category category) {
        KeyMapping km = KeyMappingHelper.registerKeyMapping(
                new KeyMapping(key, InputConstants.Type.KEYSYM, glfwKey, category));
        BINDS.add(new Bind(label, km));
        return km;
    }
}
