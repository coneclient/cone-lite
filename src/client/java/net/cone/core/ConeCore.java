package net.cone.core;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.cone.core.event.TickBus;
import net.cone.skyblock.SkyblockData;
import net.cone.telemetry.SessionManager;
import net.cone.telemetry.StatsTracker;

public final class ConeCore {
    private static final SkyblockData SKYBLOCK = new SkyblockData();
    private static final SessionManager SESSION = new SessionManager();
    private static final StatsTracker STATS = new StatsTracker();
    private static final net.cone.core.net.ReconnectManager RECONNECT = new net.cone.core.net.ReconnectManager();
    private static final net.cone.core.net.AutoSkyblock AUTO_SB = new net.cone.core.net.AutoSkyblock();
    private static final net.cone.config.ConfigProfileManager PROFILES =
            new net.cone.config.ConfigProfileManager();
    private static final TickBus TICKS = new TickBus();

    private ConeCore() {}

    public static SkyblockData skyblock() {
        return SKYBLOCK;
    }

    public static SessionManager session() {
        return SESSION;
    }

    public static StatsTracker stats() {
        return STATS;
    }

    public static boolean tasksRunning() {
        return false;
    }

    public static String activeTaskName() {
        return null;
    }

    public static void stopTasks() {
    }

    public static TickBus ticks() {
        return TICKS;
    }

    public static net.cone.core.net.ReconnectManager reconnect() {
        return RECONNECT;
    }

    public static net.cone.config.ConfigProfileManager profiles() {
        return PROFILES;
    }

    public static void init() {
        TICKS.register(SKYBLOCK);
        TICKS.register(AUTO_SB);
        TICKS.register(SESSION);
        STATS.load();
        TICKS.register(STATS);

        TICKS.register(mc -> KeepAwake.set(
                net.cone.config.ConfigManager.get().flipKeepAwake && tasksRunning()));
        PROFILES.init();
        TICKS.register(PROFILES);

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.player == null || mc.level == null) return;
            TICKS.dispatch(mc);
        });

        ClientTickEvents.END_CLIENT_TICK.register(RECONNECT::onClientTick);
        ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> STATS.save());
    }
}
