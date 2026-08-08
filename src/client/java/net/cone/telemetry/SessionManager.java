package net.cone.telemetry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.cone.ConeClient;
import net.cone.config.ConfigManager;
import net.cone.core.event.TickBus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SessionManager implements TickBus.Listener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private Session session;

    public boolean active() {
        return session != null;
    }

    public Session current() {
        return session;
    }

    public void start() {
        session = new Session();
    }

    public void end() {
        if (session == null) return;
        persist(session);
        session = null;
    }

    public void onFailsafe(String name) {
        if (session != null) {
            session.failsafes++;
            session.lastFailsafe = name;
        }
    }

    @Override
    public void onTick(Minecraft mc) {
    }

    private void persist(Session s) {
        try {
            Path dir = FabricLoader.getInstance().getConfigDir().resolve("cone").resolve("sessions");
            Files.createDirectories(dir);
            Files.writeString(dir.resolve(s.startMillis + ".json"), GSON.toJson(s));
        } catch (IOException e) {
            ConeClient.LOG.warn("[Cone] failed to save session log: {}", e.getMessage());
        }
    }
}
