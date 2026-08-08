package net.cone.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
import net.cone.ConeClient;
import net.cone.core.ConeCore;
import net.cone.core.event.TickBus;
import net.cone.skyblock.SkyblockData.Region;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class ConfigProfileManager implements TickBus.Listener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final class Settings {
        public boolean autoSwitch = false;
        public String current = "";
        public Map<String, String> regionProfiles = new HashMap<>();
    }

    private Settings settings = new Settings();
    private Region lastRegion;

    public void init() {
        loadSettings();
    }

    private Path profilesDir() { return ConfigManager.coneDir().resolve("profiles"); }
    private Path profileFile(String name) { return profilesDir().resolve(sanitize(name) + ".json"); }
    private Path settingsFile() { return ConfigManager.coneDir().resolve("profiles.json"); }

    private static String sanitize(String name) {
        return name.trim().replaceAll("[^a-zA-Z0-9 _-]", "").trim();
    }

    public List<String> list() {
        Path dir = profilesDir();
        if (!Files.isDirectory(dir)) return List.of();
        try (Stream<Path> s = Files.list(dir)) {
            return s.filter(p -> p.toString().endsWith(".json"))
                    .map(p -> p.getFileName().toString().replaceFirst("\\.json$", ""))
                    .sorted()
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return List.of();
        }
    }

    public String current() { return settings.current; }
    public boolean autoSwitch() { return settings.autoSwitch; }

    public void setAutoSwitch(boolean on) { settings.autoSwitch = on; saveSettings(); }

    public void save(String name) {
        String n = sanitize(name);
        if (n.isEmpty()) return;
        try {
            Files.createDirectories(profilesDir());
            Files.writeString(profileFile(n), GSON.toJson(ConfigManager.get()));
            settings.current = n;
            saveSettings();
            ConeClient.LOG.info("[Cone] saved config profile '{}'", n);
        } catch (IOException e) {
            ConeClient.LOG.error("[Cone] failed to save profile '{}'", n, e);
        }
    }

    public boolean load(String name) {
        Path f = profileFile(name);
        if (!Files.exists(f)) return false;
        try {
            ConeConfig cfg = GSON.fromJson(Files.readString(f), ConeConfig.class);
            if (cfg == null) return false;
            ConfigManager.replaceWith(cfg);
            settings.current = sanitize(name);
            saveSettings();
            ConeClient.LOG.info("[Cone] loaded config profile '{}'", name);
            return true;
        } catch (IOException e) {
            ConeClient.LOG.error("[Cone] failed to load profile '{}'", name, e);
            return false;
        }
    }

    public void delete(String name) {
        try {
            Files.deleteIfExists(profileFile(name));
            if (sanitize(name).equals(settings.current)) settings.current = "";
            settings.regionProfiles.values().removeIf(v -> v.equals(sanitize(name)));
            saveSettings();
        } catch (IOException e) {
            ConeClient.LOG.error("[Cone] failed to delete profile '{}'", name, e);
        }
    }

    public String regionProfile(Region region) {
        return settings.regionProfiles.getOrDefault(region.name(), "");
    }

    public void setRegionProfile(Region region, String profile) {
        if (profile == null || profile.isEmpty()) settings.regionProfiles.remove(region.name());
        else settings.regionProfiles.put(region.name(), sanitize(profile));
        saveSettings();
    }

    @Override
    public void onTick(Minecraft mc) {
        if (!settings.autoSwitch) return;
        Region region = ConeCore.skyblock().region();
        if (region == lastRegion) return;
        lastRegion = region;
        String profile = settings.regionProfiles.get(region.name());
        if (profile != null && !profile.isEmpty() && !profile.equals(settings.current)) {
            if (load(profile)) {
                ConeClient.LOG.info("[Cone] auto-switched to profile '{}' for region {}",
                        profile, region.label);
            }
        }
    }

    private void loadSettings() {
        try {
            Path f = settingsFile();
            if (Files.exists(f)) {
                Settings s = GSON.fromJson(Files.readString(f), Settings.class);
                if (s != null) {
                    settings = s;
                    if (settings.regionProfiles == null) settings.regionProfiles = new HashMap<>();
                }
            }
        } catch (IOException e) {
            ConeClient.LOG.error("[Cone] failed to load profile settings", e);
        }
    }

    private void saveSettings() {
        try {
            Files.createDirectories(ConfigManager.coneDir());
            Files.writeString(settingsFile(), GSON.toJson(settings));
        } catch (IOException e) {
            ConeClient.LOG.error("[Cone] failed to save profile settings", e);
        }
    }
}
