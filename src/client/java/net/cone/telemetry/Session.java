package net.cone.telemetry;

public final class Session {
    public long startMillis = System.currentTimeMillis();
    public int failsafes;
    public String lastFailsafe = "-";

    transient long lastReportMillis = System.currentTimeMillis();

    public long runtimeMs() {
        return System.currentTimeMillis() - startMillis;
    }

    public String runtime() {
        long s = runtimeMs() / 1000;
        return String.format("%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60);
    }
}
