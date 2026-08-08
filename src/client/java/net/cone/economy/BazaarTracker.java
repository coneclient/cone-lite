package net.cone.economy;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.cone.config.ConfigManager;

import java.util.List;

public final class BazaarTracker {
    private static final long REFRESH_MS = 45_000;
    private static volatile List<PriceClient.Entry> snapshot = List.of();
    private static volatile long snapshotAt;
    private static volatile boolean started;
    private static volatile boolean forceRefresh;

    private BazaarTracker() {}

    public static List<PriceClient.Entry> snapshot() {
        return snapshot;
    }

    public static long ageMs() {
        return snapshotAt == 0 ? -1 : System.currentTimeMillis() - snapshotAt;
    }

    public static void init() {
        if (started) return;
        started = true;
        Thread t = new Thread(BazaarTracker::loop, "cone-bazaar-tracker");
        t.setDaemon(true);
        t.start();
    }

    private static void loop() {
        long last = 0;
        while (true) {
            try {
                Thread.sleep(3_000);
                var c = ConfigManager.get();
                if (!c.bazaarHudEnabled || c.bazaarHudItems.isEmpty()) {
                    snapshot = List.of();
                    continue;
                }
                long now = System.currentTimeMillis();
                if (!forceRefresh && now - last < REFRESH_MS) continue;
                forceRefresh = false;
                List<PriceClient.Entry> fresh = PriceClient.fetch(List.copyOf(c.bazaarHudItems));
                if (!fresh.isEmpty()) { snapshot = fresh; snapshotAt = System.currentTimeMillis(); }
                last = now;
            } catch (InterruptedException e) {
                return;
            } catch (Exception e) {
            }
        }
    }

    public static void track(String item) {
        String id = item.trim().toUpperCase().replace(' ', '_');
        if (id.isEmpty()) { echo("§7usage: /track <item id>"); return; }
        var c = ConfigManager.get();
        if (c.bazaarHudItems.contains(id)) { echo("§7already tracking §f" + id); return; }
        c.bazaarHudItems.add(id);
        c.bazaarHudEnabled = true;
        ConfigManager.save();
        forceRefresh = true;
        echo("§7tracking §a" + id + " §8(" + c.bazaarHudItems.size() + ")");
    }

    public static void untrack(String item) {
        String id = item.trim().toUpperCase().replace(' ', '_');
        var c = ConfigManager.get();
        if (c.bazaarHudItems.remove(id)) {
            ConfigManager.save();
            forceRefresh = true;
            echo("§7untracked §f" + id);
        } else {
            echo("§7not tracking §f" + id);
        }
    }

    private static void echo(String msg) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (mc.player != null) mc.player.sendSystemMessage(
                    Component.literal("§8[§6BAZAAR§8] " + net.cone.command.ConeCommands.route(msg)));
        });
    }
}
