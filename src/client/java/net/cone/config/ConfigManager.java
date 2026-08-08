package net.cone.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.cone.ConeClient;
import net.cone.core.ConeCore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigManager {
    private static final int CURRENT_VERSION = 1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ConeConfig config;

    private ConfigManager() {}

    public static ConeConfig get() {
        if (config == null) config = new ConeConfig();
        return config;
    }

    public static Path coneDir() {
        return FabricLoader.getInstance().getConfigDir().resolve("cone");
    }

    private static Path file() {
        return coneDir().resolve("config.json");
    }

    public static void replaceWith(ConeConfig next) {
        if (next == null) return;
        config = next;
        migrate();
        save();
        apply();
        net.cone.gui.Theme.refresh();
    }

    public static void load() {
        try {
            Path f = file();
            if (Files.exists(f)) {
                config = GSON.fromJson(Files.readString(f), ConeConfig.class);
                if (config == null) config = new ConeConfig();
                migrate();
                ConeClient.LOG.info("[Cone] config loaded (v{})", config.configVersion);
            } else {
                config = new ConeConfig();
                save();
            }
        } catch (IOException e) {
            ConeClient.LOG.error("[Cone] failed to load config, using defaults", e);
            config = new ConeConfig();
        }
    }

    public static void save() {
        try {
            Path f = file();
            Files.createDirectories(f.getParent());
            Files.writeString(f, GSON.toJson(get()));
        } catch (IOException e) {
            ConeClient.LOG.error("[Cone] failed to save config", e);
        }
    }

    private static void migrate() {
        if (config.configVersion < CURRENT_VERSION) {
            config.configVersion = CURRENT_VERSION;
            save();
        }
    }

    public static void apply() {
    }
}
