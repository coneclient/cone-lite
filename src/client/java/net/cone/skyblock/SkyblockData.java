package net.cone.skyblock;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.cone.core.event.TickBus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class SkyblockData implements TickBus.Listener {
    public enum Region {
        DWARVEN_MINES("Dwarven Mines"), CRYSTAL_HOLLOWS("Crystal Hollows"),
        GLACITE("Glacite Tunnels"), GARDEN("Garden"), HUB("Hub"), ISLAND("Private Island"),
        OTHER("Skyblock"), NONE("-");
        public final String label; Region(String l) { this.label = l; }
    }

    private volatile Region region = Region.NONE;
    private volatile String location = "";
    private volatile String subLocation = "";
    private volatile boolean onSkyblock;
    private volatile long purse = -1;

    private int tickCounter;

    public Region region() { return region; }
    public String location() { return location; }

    public String subLocation() { return subLocation; }
    public boolean onSkyblock() { return onSkyblock; }

    public long purse() { return purse; }

    @Override
    public void onTick(Minecraft mc) {
        if (++tickCounter % 10 != 0) return;
        List<String> tab = readTab(mc);
        onSkyblock = sidebarTitle(mc).contains("SKYBLOCK");
        if (!onSkyblock) { region = Region.NONE; location = ""; purse = -1; return; }

        String area = valueAfter(tab, "Area:");
        location = area.isEmpty() ? "" : area;
        region = classify(area);
        List<String> side = readSidebar(mc);
        subLocation = specificArea(side);
        purse = readPurse(side);
    }

    private static String specificArea(List<String> side) {
        for (int i = 0; i < side.size() - 1; i++) {
            String l = side.get(i);
            if (l.matches(".*\\d{1,2}:\\d{2}\\s?(am|pm).*")) {
                return side.get(i + 1)
                        .replace("⏣", "").replace("✦", "")
                        .replaceAll("\\s*x\\d+\\s*$", "")
                        .trim();
            }
        }
        return "";
    }

    private static long readPurse(List<String> side) {
        for (String l : side) {
            String t = l.trim();
            if (t.startsWith("Purse:") || t.startsWith("Piggy:")) {
                String num = t.substring(6).replaceAll("\\(.*?\\)", "").replace(",", "").trim();
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d[\\d.]*)").matcher(num);
                if (m.find()) {
                    try { return (long) Double.parseDouble(m.group(1)); }
                    catch (NumberFormatException ignored) { return -1; }
                }
            }
        }
        return -1;
    }

    private static Region classify(String area) {
        if (area.contains("Dwarven Mines")) return Region.DWARVEN_MINES;
        if (area.contains("Crystal Hollows")) return Region.CRYSTAL_HOLLOWS;
        if (area.contains("Glacite") || area.contains("Mineshaft")) return Region.GLACITE;
        if (area.contains("Garden")) return Region.GARDEN;
        if (area.contains("Hub") || area.contains("Village")) return Region.HUB;
        if (area.contains("Island")) return Region.ISLAND;
        return area.isEmpty() ? Region.NONE : Region.OTHER;
    }

    private static String valueAfter(List<String> lines, String prefix) {
        for (String l : lines) {
            if (l.startsWith(prefix)) return l.substring(prefix.length()).trim();
        }
        return "";
    }

    private static List<String> readTab(Minecraft mc) {
        if (mc.getConnection() == null) return List.of();
        List<String> out = new ArrayList<>();
        for (PlayerInfo info : mc.getConnection().getOnlinePlayers()) {
            Component d = info.getTabListDisplayName();
            if (d == null) continue;
            String s = strip(d.getString());
            if (!s.isBlank()) out.add(s);
        }
        return out;
    }

    private static String sidebarTitle(Minecraft mc) {
        if (mc.level == null) return "";
        Objective obj = mc.level.getScoreboard().getDisplayObjective(DisplaySlot.SIDEBAR);
        return obj == null ? "" : strip(obj.getDisplayName().getString());
    }

    private static List<String> readSidebar(Minecraft mc) {
        if (mc.level == null) return List.of();
        Scoreboard sb = mc.level.getScoreboard();
        Objective obj = sb.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (obj == null) return List.of();
        List<PlayerScoreEntry> entries = new ArrayList<>(sb.listPlayerScores(obj));
        entries.sort(Comparator.comparingInt(PlayerScoreEntry::value).reversed());
        List<String> lines = new ArrayList<>();
        for (PlayerScoreEntry e : entries) {
            PlayerTeam team = sb.getPlayersTeam(e.owner());
            String text = team != null
                    ? PlayerTeam.formatNameForTeam(team, e.ownerName()).getString()
                    : e.ownerName().getString();
            lines.add(strip(text));
        }
        lines.removeIf(String::isBlank);
        return lines;
    }

    private static String strip(String s) {
        return s.replaceAll("§.", "").trim();
    }

    public List<String> debugDump(Minecraft mc) {
        List<String> out = new ArrayList<>();
        out.add("=== SIDEBAR ===");
        out.add("title='" + sidebarTitle(mc) + "'  onSkyblock=" + onSkyblock
                + "  region=" + region + "  area='" + location + "'");
        int i = 0;
        for (String l : readSidebar(mc)) out.add("sb" + (i++) + ": '" + l + "'");

        var tab = mc.gui.getTabList();
        var acc = (net.cone.mixin.TabListAccessor) tab;
        for (String l : splitComponent(acc.cone$getHeader())) out.add("header: '" + l + "'");
        for (String l : splitComponent(acc.cone$getFooter())) out.add("footer: '" + l + "'");

        out.add("=== TAB ENTRIES ===");
        i = 0;
        for (String l : readTab(mc)) out.add("tab" + (i++) + ": '" + l + "'");
        return out;
    }

    private static List<String> splitComponent(Component c) {
        List<String> out = new ArrayList<>();
        if (c == null) return out;
        for (String line : strip(c.getString()).split("\n")) {
            if (!line.isBlank()) out.add(line.trim());
        }
        return out;
    }
}
