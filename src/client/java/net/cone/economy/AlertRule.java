package net.cone.economy;

public final class AlertRule {
    public enum Metric { SELL, BUY, SPREAD, SPIKE, DIP }

    public String id = "";

    public String name = "";
    public Metric metric = Metric.SELL;

    public boolean above = true;

    public double value;

    public boolean once;

    public transient boolean met;

    public transient long firedAt;

    public AlertRule() {}

    public AlertRule(String id, String name, Metric metric, boolean above, double value, boolean once) {
        this.id = id;
        this.name = name;
        this.metric = metric;
        this.above = above;
        this.value = value;
        this.once = once;
    }

    public String condition() {
        return switch (metric) {
            case SELL, BUY -> metric.name().toLowerCase() + (above ? " > " : " < ") + PriceClient.comma(value);
            case SPREAD -> "spread > " + trim(value) + "%";
            case SPIKE -> "spike > " + trim(value) + "%";
            case DIP -> "dip > " + trim(value) + "%";
        };
    }

    public double read(PriceClient.Entry e) {
        return switch (metric) {
            case SELL -> e.instaSell();
            case BUY -> e.instaBuy();
            case SPREAD -> e.flipPct();
            case SPIKE, DIP -> move(e);
        };
    }

    private static double move(PriceClient.Entry e) {
        if (e.avgSell() <= 0) return 0;
        return (e.instaSell() - e.avgSell()) / e.avgSell() * 100;
    }

    public boolean holds(PriceClient.Entry e) {
        double v = read(e);
        return switch (metric) {
            case SELL, BUY -> above ? v >= value : v <= value;
            case SPREAD, SPIKE -> v >= value;
            case DIP -> v <= -value;
        };
    }

    public String format(double v) {
        return switch (metric) {
            case SELL, BUY -> PriceClient.comma(v);
            default -> trim(v) + "%";
        };
    }

    private static String trim(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.format("%.2f", v);
    }
}
