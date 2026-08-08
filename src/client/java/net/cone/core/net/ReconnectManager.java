package net.cone.core.net;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.cone.ConeClient;
import net.cone.config.ConfigManager;
import net.cone.config.ConeConfig;
import net.cone.core.ConeCore;

import java.util.Random;

public final class ReconnectManager {
    private enum State { IDLE, WAIT, CONNECTING }

    private final Random rng = new Random();

    private State state = State.IDLE;
    private ServerData lastServer;
    private long reconnectAtMs;
    private long connectTimeoutMs;
    private int tries;

    private long sessionEndMs;
    private long restBreakMs;
    private boolean restDisconnect;

    public void onDisconnect(boolean voluntary) {
        ConeConfig c = ConfigManager.get();

        if (lastServer == null) return;

        if (restDisconnect) {
            restDisconnect = false;
            reconnectAtMs = System.currentTimeMillis() + restBreakMs;
            sessionEndMs = 0;
            tries = 0;
            state = State.WAIT;
            ConeClient.LOG.info("[Cone] dynamic rest: back in {} min", Math.round(restBreakMs / 60000.0));
            return;
        }

        if (voluntary) { state = State.IDLE; tries = 0; return; }

        if (!c.autoReconnect) return;

        if (state == State.IDLE) tries = 0; else tries++;
        schedule(c);
        ConeClient.LOG.info("[Cone] auto-reconnect: {} in {}s", lastServer.ip, c.reconnectDelaySec);
    }

    private void schedule(ConeConfig c) {
        long base = Math.max(2, c.reconnectDelaySec) * 1000L;
        long delay = Math.min(120_000L, base * (1L << Math.min(3, tries)));
        reconnectAtMs = System.currentTimeMillis() + delay + rng.nextInt(2000);
        state = State.WAIT;
    }

    public void onClientTick(Minecraft mc) {
        boolean connected = mc.level != null && mc.getConnection() != null;
        if (connected) {
            ServerData s = mc.getCurrentServer();
            if (s != null) lastServer = s;
            tickRest(mc);
        }

        switch (state) {
            case WAIT -> {
                if (connected) { state = State.IDLE; tries = 0; return; }
                if (System.currentTimeMillis() >= reconnectAtMs) {
                    if (startReconnect(mc)) {
                        connectTimeoutMs = System.currentTimeMillis() + 60_000L;
                        state = State.CONNECTING;
                    } else {
                        retryOrGiveUp("could not start the connect");
                    }
                }
            }
            case CONNECTING -> {
                if (connected) {
                    tries = 0;
                    state = State.IDLE;
                } else if (System.currentTimeMillis() >= connectTimeoutMs) {
                    retryOrGiveUp("connect attempt timed out");
                }
            }
            default -> {}
        }
    }

    private void retryOrGiveUp(String why) {
        ConeConfig c = ConfigManager.get();
        tries++;
        if (c.reconnectMaxTries > 0 && tries >= c.reconnectMaxTries) {
            ConeClient.LOG.warn("[Cone] auto-reconnect: giving up after {} tries ({})", tries, why);
            state = State.IDLE;
            tries = 0;
            return;
        }
        schedule(c);
        ConeClient.LOG.info("[Cone] auto-reconnect: retry #{} ({})", tries, why);
    }

    private void tickRest(Minecraft mc) {
        ConeConfig c = ConfigManager.get();
        if (!c.dynamicRest || !ConeCore.tasksRunning()) { sessionEndMs = 0; return; }
        long now = System.currentTimeMillis();
        if (sessionEndMs == 0) {
            double hrs = rand(c.restSessionMinHours, c.restSessionMaxHours);
            sessionEndMs = now + (long) (hrs * 3600_000L);
            ConeClient.LOG.info("[Cone] dynamic rest: session ~{} min", Math.round(hrs * 60));
        } else if (now >= sessionEndMs) {
            beginRest(mc, c);
        }
    }

    private void beginRest(Minecraft mc, ConeConfig c) {
        double mins = rand(c.restBreakMinMinutes, c.restBreakMaxMinutes);
        restBreakMs = (long) (mins * 60_000L);
        restDisconnect = true;
        sessionEndMs = 0;
        ConeClient.LOG.info("[Cone] dynamic rest: logging off for ~{} min", Math.round(mins));
        mc.disconnect(new TitleScreen(), false);
    }

    private double rand(double lo, double hi) {
        if (hi <= lo) return lo;
        return lo + rng.nextDouble() * (hi - lo);
    }

    private boolean startReconnect(Minecraft mc) {
        if (mc.level != null) return false;
        try {
            ServerAddress addr = ServerAddress.parseString(lastServer.ip);
            ConnectScreen.startConnecting(new TitleScreen(), mc, addr, lastServer, false, null);
            ConeClient.LOG.info("[Cone] reconnecting to {}", lastServer.ip);
            return true;
        } catch (Exception e) {
            ConeClient.LOG.error("[Cone] reconnect failed", e);
            return false;
        }
    }
}
