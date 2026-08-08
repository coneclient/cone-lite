package net.cone.core.net;

import net.minecraft.client.Minecraft;
import net.cone.ConeClient;
import net.cone.config.ConfigManager;
import net.cone.config.ConeConfig;
import net.cone.core.ConeCore;
import net.cone.core.event.TickBus;

public final class AutoSkyblock implements TickBus.Listener {
    private static final long WINDOW_MS = 120_000;
    private static final long LIMBO_GRACE_MS = 20_000;
    private static final int MAX_SENDS = 3;

    private Object lastConnection;
    private long armedAtMs;
    private long nextSendMs;
    private int sends;
    private boolean needLobby;
    private boolean done;
    private boolean wasSkyblock;
    private long leftAtMs;

    @Override
    public void onTick(Minecraft mc) {
        if (mc.player == null || mc.getConnection() == null) return;
        ConeConfig c = ConfigManager.get();
        long now = System.currentTimeMillis();

        if (mc.getConnection() != lastConnection) {
            lastConnection = mc.getConnection();
            arm(now, c);
            wasSkyblock = false;
            leftAtMs = 0;
        }

        if (!c.autoJoinSkyblock || !onHypixel(mc)) return;

        if (ConeCore.skyblock().onSkyblock()) {
            wasSkyblock = true;
            leftAtMs = 0;
            done = true;
            return;
        }

        if (wasSkyblock) {
            if (leftAtMs == 0) leftAtMs = now;
            if (now - leftAtMs < LIMBO_GRACE_MS) return;
            wasSkyblock = false;
            leftAtMs = 0;
            arm(now, c);
        }

        if (done) return;
        if (now - armedAtMs > WINDOW_MS || sends >= MAX_SENDS) {
            ConeClient.LOG.info("[Cone] auto /skyblock: gave up after {} tries", sends);
            done = true;
            return;
        }
        if (now < nextSendMs) return;

        if (needLobby) {
            needLobby = false;
            mc.player.connection.sendCommand("lobby");
            nextSendMs = now + 4000;
            return;
        }
        mc.player.connection.sendCommand("skyblock");
        sends++;
        needLobby = true;
        nextSendMs = now + 15_000;
        ConeClient.LOG.info("[Cone] auto /skyblock: sent (try {})", sends);
    }

    private void arm(long now, ConeConfig c) {
        armedAtMs = now;
        nextSendMs = now + (long) (Math.max(1, c.skyblockJoinDelaySec) * 1000);
        sends = 0;
        needLobby = false;
        done = false;
    }

    private static boolean onHypixel(Minecraft mc) {
        var s = mc.getCurrentServer();
        return s != null && s.ip != null && s.ip.toLowerCase().contains("hypixel");
    }
}
