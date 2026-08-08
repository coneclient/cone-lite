package net.cone.telemetry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.cone.ConeClient;
import net.cone.core.ConeCore;
import net.cone.core.event.TickBus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class StatsTracker implements TickBus.Listener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final long SAVE_INTERVAL_MS = 20_000;

    private Stats stats = new Stats();
    private boolean dirty;
    private long lastSaveMillis;
    private long lastTickMillis;

    public Stats stats() {
        return stats;
    }

    private Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("cone").resolve("stats.json");
    }

    public void load() {
        Path f = file();
        try {
            if (Files.exists(f)) {
                Stats s = GSON.fromJson(Files.readString(f), Stats.class);
                if (s != null) stats = s;
            }
        } catch (Exception e) {
            ConeClient.LOG.warn("[Cone] failed to read stats: {}", e.getMessage());
        }
    }

    public void failsafe()     { stats.failsafesTripped++; dirty = true; }

    @Override
    public void onTick(Minecraft mc) {
        long now = System.currentTimeMillis();
        if (lastTickMillis != 0 && ConeCore.tasksRunning()) {
            long delta = now - lastTickMillis;
            if (delta > 0 && delta < 5_000) {
                stats.playtimeMs += delta;
                dirty = true;
            }
        }
        lastTickMillis = now;

        if (dirty && now - lastSaveMillis >= SAVE_INTERVAL_MS) {
            save();
        }
    }

    public void save() {
        if (!dirty) return;
        try {
            Path f = file();
            Files.createDirectories(f.getParent());
            Files.writeString(f, GSON.toJson(stats));
            dirty = false;
            lastSaveMillis = System.currentTimeMillis();
        } catch (IOException e) {
            ConeClient.LOG.warn("[Cone] failed to save stats: {}", e.getMessage());
        }
    }

    /**
     * Fold a remote copy into the local one, keeping the larger of each running total. Call it on
     * the client thread. The stats stay a monotonic union, so no run ever loses time or a count.
     */
    public void mergeRemote(long firstSeen, long failsafesTripped, long playtimeMs) {
        stats.firstSeen        = Math.min(stats.firstSeen, firstSeen);
        stats.failsafesTripped = Math.max(stats.failsafesTripped, failsafesTripped);
        stats.playtimeMs       = Math.max(stats.playtimeMs, playtimeMs);
        dirty = true;
    }
}
