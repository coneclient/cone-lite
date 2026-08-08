package net.cone.telemetry;

public final class Stats {
    public long firstSeen = System.currentTimeMillis();

    public long failsafesTripped;

    public long playtimeMs;
}
