package net.cone.gui.skija;

import net.cone.config.ConeConfig;
import net.cone.config.ConfigManager;
import net.cone.core.ConeCore;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class SkijaDash {
    private SkijaDash() {}

    public enum Style { NORMAL, PRIMARY, DANGER }

    public record Btn(String label, Style style, Runnable run) {}

    private static final Random RNG = new Random();
    private static final String[] FLAVORS = {
        "Buy low, list high, repeat.",
        "The spread won't flip itself.",
        "Every order book has a gap. Go find it.",
        "Coins compound. So does patience.",
        "Somewhere, an underpriced book is waiting.",
        "Stay liquid, stay legit.",
        "Margins are made, not found.",
        "Flip clean, rest often.",
        "The market rewards the patient.",
        "Numbers up. That's the whole job.",
    };

    static String flavor = "";

    static void surprise() {
        flavor = FLAVORS[RNG.nextInt(FLAVORS.length)];
        cfg().skijaAccentHue = RNG.nextInt(360);
        SkDesign.refresh(cfg());
        ConfigManager.save();
    }

    static Btn updateBanner() {
        return null;
    }

    static List<Btn> quickActions() {
        List<Btn> out = new ArrayList<>();
        out.add(new Btn("Show flips", Style.NORMAL,
                () -> net.cone.economy.PriceClient.printFlips()));
        out.add(new Btn("Show book chains", Style.NORMAL,
                () -> net.cone.economy.PriceClient.printEnchantFlips("")));
        return out;
    }

    static List<Btn> profiles() {
        List<Btn> out = new ArrayList<>();
        var pm = ConeCore.profiles();
        String cur = pm.current();
        for (String nm : pm.list()) {
            boolean active = nm.equals(cur);
            out.add(new Btn(nm, active ? Style.PRIMARY : Style.NORMAL, () -> pm.load(nm)));
        }
        return out;
    }

    static String heroProfit() {
        return "Cone Lite";
    }

    static double heroProfitValue() {
        return 0;
    }

    static String heroRate() {
        return null;
    }

    static String heroIdentity() {
        return displayName() + "  ·  " + membershipShort();
    }

    static String heroLive() {
        return plain(statusLines()[0]);
    }

    static int heroLiveAccent() {
        return statusAccent();
    }

    static List<String[]> openPositions() {
        return List.of();
    }

    static boolean firstRun() {
        return true;
    }

    static List<String[]> firstSteps() {
        List<String[]> out = new ArrayList<>();
        out.add(new String[] { "Type /cone help in chat",
                "every command, grouped, with examples you can click" });
        out.add(new String[] { "Run /flips",
                "ranks what the bazaar pays right now, by coins per hour" });
        out.add(new String[] { "Set an alert with /alert <item> sell>1.2m",
                "Cone tells you the moment the price crosses your line" });
        return out;
    }

    static String greetingLine() {
        return greeting() + ", " + displayName();
    }

    static String rankLine() {
        return "Lite";
    }

    static String sinceLine() {
        return "member since " + java.time.Instant.ofEpochMilli(ConeCore.stats().stats().firstSeen)
                .atZone(java.time.ZoneId.systemDefault()).toLocalDate();
    }

    static String[] bigStat() {
        return new String[] { "-", "coins profit" };
    }

    static String[][] smallStats() {
        long playMs = ConeCore.stats().stats().playtimeMs;
        return new String[][] {
            { "-", "flips cleared" },
            { "-", "coins / hour" },
            { "-", "win rate" },
            { hoursMinutes(playMs), "playtime" },
        };
    }

    static String stripLine() {
        long n = ConeCore.stats().stats().failsafesTripped;
        return n == 0 ? "no failsafes ever tripped, clean record"
                : fmt(n) + " failsafe" + (n == 1 ? "" : "s") + " tripped, lifetime";
    }

    static String sessionLine() {
        return null;
    }

    static String[] statusLines() {
        String state = "Live bazaar prices. Alerts armed";
        String line2 = "Cone Lite tracks the bazaar in real time";

        var sky = ConeCore.skyblock();
        String line3 = sky.onSkyblock()
                ? sky.region().label + (sky.subLocation().isBlank() ? "" : "  ·  " + sky.subLocation())
                : "Not on Skyblock";
        return new String[] { state, line2, line3 };
    }

    static int statusAccent() {
        return SkDesign.MUTED;
    }

    static String failsafeNote() {
        return null;
    }

    private static ConeConfig cfg() {
        return ConfigManager.get();
    }

    static String displayName() {
        ConeConfig c = cfg();
        if (c.nickEnabled && c.nickName != null && !c.nickName.isBlank()) return c.nickName.trim();
        var profile = Minecraft.getInstance().getGameProfile();
        return profile != null ? profile.name() : "miner";
    }

    private static String greeting() {
        int h = java.time.LocalTime.now().getHour();
        if (h < 5)  return "Still up";
        if (h < 12) return "Good morning";
        if (h < 18) return "Good afternoon";
        return "Good evening";
    }

    private static String fmt(long n) {
        return String.format("%,d", n);
    }

    static String signed(double v) {
        long r = Math.round(v);
        if (r == 0) return "0";
        return (r > 0 ? "+" : "-") + String.format("%,d", Math.abs(r));
    }

    private static String membershipShort() {
        long first = ConeCore.stats().stats().firstSeen;
        if (first <= 0) return "new here";
        long days = Math.max(0, (System.currentTimeMillis() - first) / 86_400_000L);
        if (days < 1) return "joined today";
        if (days < 60) return "member " + days + "d";
        if (days < 730) return "member " + (days / 30) + "mo";
        return "member " + (days / 365) + "y";
    }

    static String plain(String s) {
        return s == null ? "" : s.replaceAll("§.", "").trim();
    }

    private static String hoursMinutes(long ms) {
        long m = ms / 60_000;
        if (m < 60) return m + "m";
        return (m / 60) + "h " + (m % 60) + "m";
    }
}
