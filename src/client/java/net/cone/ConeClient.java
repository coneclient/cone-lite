package net.cone;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.cone.config.ConfigManager;
import net.cone.core.ConeCore;
import net.cone.gui.hud.ConeHud;
import net.cone.input.Keybinds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConeClient implements ClientModInitializer {
    public static final String MOD_ID = "cone";
    public static final Logger LOG = LoggerFactory.getLogger("Cone");

    public static final String VERSION = net.fabricmc.loader.api.FabricLoader.getInstance()
            .getModContainer(MOD_ID)
            .map(m -> m.getMetadata().getVersion().getFriendlyString())
            .orElse("dev");

    @Override
    public void onInitializeClient() {
        LOG.info("[Cone] v{} loaded on MC 26.1.2", VERSION);
        ConfigManager.load();
        net.cone.gui.Theme.refresh();
        ConeCore.init();
        ConfigManager.apply();
        Keybinds.register();

        ClientCommandRegistrationCallback.EVENT.register(
                (dispatcher, access) -> net.cone.command.ConeCommands.register(dispatcher));
        net.cone.command.ConeCommands.hintOnce();
        net.cone.gui.skija.FontLib.ensure();
        net.cone.economy.BazaarTracker.init();
        net.cone.economy.PriceAlerts.init();
        ConeHud.register();
        net.cone.gui.hud.BazaarHud.register();
    }
}
