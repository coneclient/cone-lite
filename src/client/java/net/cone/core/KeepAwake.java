package net.cone.core;

import net.cone.ConeClient;

import java.util.Locale;

public final class KeepAwake {
    private static Process proc;
    private static boolean desired;

    private static final boolean MAC =
            System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac");

    private KeepAwake() {}

    public static void set(boolean active) {
        if (active == desired) return;
        desired = active;
        if (active) acquire();
        else release();
    }

    private static void acquire() {
        if (!MAC) return;
        if (proc != null && proc.isAlive()) return;
        try {
            String pid = String.valueOf(ProcessHandle.current().pid());
            proc = new ProcessBuilder("caffeinate", "-di", "-w", pid).redirectErrorStream(true).start();
            ConeClient.LOG.info("[Cone] keep-awake on (caffeinate pid {}, tied to jvm {})", proc.pid(), pid);
        } catch (Exception e) {
            proc = null;
            ConeClient.LOG.warn("[Cone] keep-awake failed to start caffeinate", e);
        }
    }

    private static void release() {
        if (proc == null) return;
        try {
            proc.destroy();
            ConeClient.LOG.info("[Cone] keep-awake off");
        } catch (Exception ignored) {
        } finally {
            proc = null;
        }
    }
}
