package net.cone.economy;

import net.cone.ConeClient;
import net.cone.config.ConeConfig;
import net.cone.config.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static net.cone.command.ConeCommands.cmd;

public final class PriceAlerts {
    private PriceAlerts() {}

    private static final long TICK_MS = 3_000;

    private static final long BACKOFF_MS = 60_000;
    private static final long MAX_BACKOFF_MS = 300_000;

    private static volatile boolean started;
    private static volatile long lastPoll;
    private static volatile long backoff;

    private static final Pattern EXPR = Pattern.compile(
            "(?i)^(sell|buy|spread|spike|dip)\\s*(>=|<=|>|<)?\\s*(-?[0-9]*\\.?[0-9]+)\\s*([kmb%])?$");

    public static void init() {
        if (started) return;
        started = true;
        Thread t = new Thread(PriceAlerts::loop, "cone-price-alerts");
        t.setDaemon(true);
        t.start();
    }

    private static void loop() {
        while (true) {
            try {
                Thread.sleep(TICK_MS);
                ConeConfig c = ConfigManager.get();
                if (!c.priceAlertsEnabled || c.priceAlertRules.isEmpty()) continue;

                if (Minecraft.getInstance().player == null) continue;

                long now = System.currentTimeMillis();
                long every = clamp(c.priceAlertPollSec, 30, 600) * 1000L + backoff;
                if (now - lastPoll < every) continue;
                lastPoll = now;
                poll(c, now);
            } catch (InterruptedException e) {
                return;
            } catch (PriceClient.RateLimited r) {
                backoff = backoff == 0 ? BACKOFF_MS : Math.min(MAX_BACKOFF_MS, backoff * 2);
                ConeClient.LOG.warn("[Cone] price alerts throttled, backing off {}s", backoff / 1000);
            } catch (Exception e) {
            }
        }
    }

    private static void poll(ConeConfig c, long now) throws Exception {
        List<AlertRule> rules = rules(c);
        if (rules.isEmpty()) return;

        List<String> ids = new ArrayList<>(new LinkedHashSet<>(rules.stream().map(r -> r.id).toList()));
        List<PriceClient.Entry> quotes = PriceClient.fetch(ids);
        if (quotes.isEmpty()) return;
        backoff = 0;

        Map<String, PriceClient.Entry> byId = new HashMap<>();
        for (PriceClient.Entry e : quotes) byId.put(e.id(), e);

        long cooldown = clamp(c.priceAlertCooldownMin, 1, 720) * 60_000L;
        for (AlertRule r : rules) {
            PriceClient.Entry e = byId.get(r.id);
            if (e == null) continue;
            if (!r.holds(e)) {
                r.met = false;
                continue;
            }
            if (r.met) continue;
            if (now - r.firedAt < cooldown) continue;
            r.met = true;
            r.firedAt = now;
            fire(r, e);
            if (r.once) remove(c, r);
        }
    }

    private static void fire(AlertRule r, PriceClient.Entry e) {
        MutableComponent line = Component.literal("§8[§6ALERT§8] §f" + r.name + " §7" + r.condition());
        line.setStyle(line.getStyle()
                .withClickEvent(new ClickEvent.RunCommand("/price " + r.id))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal("§7show §f" + r.name))));
        showComp(line);
        show("  §7now §a" + r.format(r.read(e))
                + " §8| §7insta-buy §f" + PriceClient.comma(e.instaBuy())
                + " §8| §7insta-sell §f" + PriceClient.comma(e.instaSell()));
        if (e.flagged()) show("  §c⚠ flagged: §7" + e.reasons());
        sound();
        ConeClient.LOG.info("[Cone] alert fired: {} {}", r.id, r.condition());
    }

    private static void sound() {
        ConeConfig c = ConfigManager.get();
        if (!c.priceAlertSound) return;
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            try {
                mc.getSoundManager().play(SimpleSoundInstance.forUI(resolve(c.priceAlertSoundId), 1.0f, 1.0f));
            } catch (Exception ignored) {  }
        });
    }

    private static SoundEvent resolve(String id) {
        try {
            var rl = net.minecraft.resources.Identifier.tryParse(id == null ? "" : id.trim());
            if (rl != null) {
                var ev = net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.getOptional(rl);
                if (ev.isPresent()) return ev.get();
            }
        } catch (Exception ignored) {}
        return SoundEvents.NOTE_BLOCK_PLING.value();
    }

    public static void command(String raw) {
        String arg = raw == null ? "" : raw.trim();
        ConeConfig c = ConfigManager.get();

        if (arg.isEmpty() || arg.equalsIgnoreCase("list")) { list(c); return; }
        if (arg.equalsIgnoreCase("help")) { usage(); return; }
        if (arg.equalsIgnoreCase("test")) { test(); return; }
        if (arg.equalsIgnoreCase("on") || arg.equalsIgnoreCase("off")) {
            c.priceAlertsEnabled = arg.equalsIgnoreCase("on");
            ConfigManager.save();
            show("§7price alerts " + (c.priceAlertsEnabled ? "§aon" : "§coff"));
            return;
        }
        if (arg.equalsIgnoreCase("clear")) {
            int n = c.priceAlertRules.size();
            synchronized (c.priceAlertRules) { c.priceAlertRules.clear(); }
            ConfigManager.save();
            show("§7cleared §f" + n + "§7 rule" + (n == 1 ? "" : "s"));
            return;
        }
        String[] parts = arg.split("\\s+");
        if (parts.length == 2 && (parts[0].equalsIgnoreCase("del")
                || parts[0].equalsIgnoreCase("rm") || parts[0].equalsIgnoreCase("remove"))) {
            delete(c, parts[1]);
            return;
        }

        arg = arg.replaceAll("\\s*(>=|<=|>|<)\\s*", "$1");
        boolean once = arg.toLowerCase().endsWith(" once");
        if (once) arg = arg.substring(0, arg.length() - 5).trim();
        int cut = arg.lastIndexOf(' ');
        if (cut <= 0) { usage(); return; }
        String item = arg.substring(0, cut).trim();
        Matcher m = EXPR.matcher(arg.substring(cut + 1).trim());
        if (!m.matches()) { usage(); return; }

        Spec spec = parse(arg.substring(cut + 1).trim());
        if (spec == null) { usage(); return; }
        add(item, spec, once);
    }

    private static final class Spec {
        final AlertRule.Metric metric;
        final boolean above;
        final double value;

        Spec(AlertRule.Metric metric, boolean above, double value) {
            this.metric = metric;
            this.above = above;
            this.value = value;
        }
    }

    private static Spec parse(String expr) {
        Matcher m = EXPR.matcher(expr.trim());
        if (!m.matches()) return null;
        AlertRule.Metric metric = AlertRule.Metric.valueOf(m.group(1).toUpperCase());
        boolean above = m.group(2) == null || m.group(2).startsWith(">");
        double value = Double.parseDouble(m.group(3)) * scale(m.group(4));
        if (metric != AlertRule.Metric.SELL && metric != AlertRule.Metric.BUY) {
            value = Math.abs(value);
        }
        return new Spec(metric, above, value);
    }

    private static double scale(String suffix) {
        if (suffix == null) return 1;
        return switch (Character.toLowerCase(suffix.charAt(0))) {
            case 'k' -> 1_000d;
            case 'm' -> 1_000_000d;
            case 'b' -> 1_000_000_000d;
            default -> 1;
        };
    }

    private static void add(String item, Spec spec, boolean once) {
        Thread t = new Thread(() -> {
            try {
                show("§8[§6ALERT§8] §7" + addRule(item, spec, once));
            } catch (PriceClient.RateLimited r) {
                show("§7price API is throttling - try again in a minute");
            } catch (Exception ex) {
                ConeClient.LOG.warn("[Cone] /alert add failed", ex);
                show("§7could not add that alert §8(" + ex.getClass().getSimpleName() + ")");
            }
        }, "cone-alert-add");
        t.setDaemon(true);
        t.start();
    }

    private static String addRule(String item, Spec spec, boolean once) throws Exception {
        PriceClient.Entry e = resolveItem(item);
        if (e == null) return "no bazaar match for " + item;
        if ((spec.metric == AlertRule.Metric.SPIKE || spec.metric == AlertRule.Metric.DIP)
                && e.avgSell() <= 0) {
            return "no weekly average for " + e.name() + " - spike/dip needs one";
        }
        AlertRule r = new AlertRule(e.id(), e.name(), spec.metric, spec.above, spec.value, once);

        r.met = r.holds(e);
        ConeConfig c = ConfigManager.get();
        synchronized (c.priceAlertRules) { c.priceAlertRules.add(r); }
        Minecraft.getInstance().execute(ConfigManager::save);
        return "alert " + c.priceAlertRules.size() + ": " + r.name + " " + r.condition()
                + (once ? " (once)" : "")
                + (r.met ? " - already true, waiting for the next crossing" : "");
    }

    private static PriceClient.Entry resolveItem(String item) throws Exception {
        String guess = item.trim().toUpperCase().replace(' ', '_');
        if (guess.matches("[A-Z0-9_]+") && guess.contains("_")) {
            List<PriceClient.Entry> exact = PriceClient.fetch(List.of(guess));
            if (!exact.isEmpty()) return exact.get(0);
        }
        List<PriceClient.Entry> found = PriceClient.search(item.trim());
        return found.isEmpty() ? null : found.get(0);
    }

    private static void delete(ConeConfig c, String which) {
        int n;
        try {
            n = Integer.parseInt(which.trim());
        } catch (NumberFormatException e) {
            show("§7usage: /alert del <number>");
            return;
        }
        AlertRule removed = null;
        synchronized (c.priceAlertRules) {
            if (n >= 1 && n <= c.priceAlertRules.size()) removed = c.priceAlertRules.remove(n - 1);
        }
        if (removed == null) { show("§7no rule §f" + n); return; }
        ConfigManager.save();
        show("§7removed §f" + removed.name + " " + removed.condition());
    }

    private static void list(ConeConfig c) {
        List<AlertRule> rules = rules(c);
        if (rules.isEmpty()) { usage(); return; }
        show("§8[§6ALERT§8] §f" + rules.size() + " rule" + (rules.size() == 1 ? "" : "s")
                + (c.priceAlertsEnabled ? "" : " §c(alerts are off)"));
        long now = System.currentTimeMillis();
        long cooldown = clamp(c.priceAlertCooldownMin, 1, 720) * 60_000L;
        for (int i = 0; i < rules.size(); i++) {
            AlertRule r = rules.get(i);
            String state;
            if (r.met) state = "§8(waiting for it to go back)";
            else if (now - r.firedAt < cooldown) state = "§8(cooling down)";
            else state = "§8(armed)";
            MutableComponent line = Component.literal(
                    "  §7" + (i + 1) + ". §f" + r.name + " §7" + r.condition()
                            + (r.once ? " §8once " : " ") + state);
            final int idx = i + 1;
            line.setStyle(line.getStyle()
                    .withClickEvent(new ClickEvent.SuggestCommand(cmd("alert") + " del " + idx))
                    .withHoverEvent(new HoverEvent.ShowText(Component.literal("§7click to remove rule " + idx))));
            showComp(line);
        }
        show("  §8/alert del <n> removes one, /alert clear removes them all");
    }

    private static void usage() {
        show("§8[§6ALERT§8] §7tell me when a price does something:");
        show("  §f/alert <item> sell>1.2m §8- insta-sell goes above 1.2m");
        show("  §f/alert <item> buy<900k §8- insta-buy drops under 900k");
        show("  §f/alert <item> spread>6% §8- the order-flip margin opens past 6%");
        show("  §f/alert <item> spike>10% §8- price runs 10% over its weekly average");
        show("  §f/alert <item> dip>10% §8- price falls 10% under its weekly average");
        show("  §8add \"once\" to delete the rule after it fires. /alert list, /alert del <n>");
    }

    private static void test() {
        AlertRule r = new AlertRule("ENCHANTED_DIAMOND", "Enchanted Diamond",
                AlertRule.Metric.SELL, true, 1_200_000, false);
        showComp(Component.literal("§8[§6ALERT§8] §f" + r.name + " §7" + r.condition() + " §8(test)"));
        show("  §7now §a" + r.format(1_245_000) + " §8| §7this is what a real alert looks like");
        sound();
    }

    public static String remoteList() {
        ConeConfig c = ConfigManager.get();
        List<AlertRule> rules = rules(c);
        long now = System.currentTimeMillis();
        long cooldown = clamp(c.priceAlertCooldownMin, 1, 720) * 60_000L;

        StringBuilder json = new StringBuilder();
        json.append("{\"card\":\"alerts\",\"enabled\":").append(c.priceAlertsEnabled)
                .append(",\"every\":").append(clamp(c.priceAlertPollSec, 30, 600))
                .append(",\"rules\":[");
        for (int i = 0; i < rules.size(); i++) {
            AlertRule r = rules.get(i);
            String state = r.met ? "holding" : (now - r.firedAt < cooldown ? "cooling" : "armed");
            if (i > 0) json.append(',');
            json.append("{\"name\":").append(quote(r.name))
                    .append(",\"cond\":").append(quote(r.condition()))
                    .append(",\"state\":\"").append(state).append('"')
                    .append(",\"once\":").append(r.once)
                    .append('}');
        }
        return json.append("]}").toString();
    }

    public static void remoteAdd(String item, String expr, boolean once,
                                 java.util.function.BiConsumer<Boolean, String> done) {
        String name = item == null ? "" : item.trim();
        if (name.isEmpty()) { done.accept(false, "no item given."); return; }
        Spec spec = parse(expr == null ? "" : expr);
        if (spec == null) {
            done.accept(false, "`" + expr + "` isn't a condition. Try sell>1.2m, buy<900k, "
                    + "spread>6%, spike>10% or dip>10%.");
            return;
        }
        Thread t = new Thread(() -> {
            try {
                done.accept(true, addRule(name, spec, once));
            } catch (PriceClient.RateLimited r) {
                done.accept(false, "the price API is throttling - try again in a minute.");
            } catch (Exception ex) {
                ConeClient.LOG.warn("[Cone] remote alert add failed", ex);
                done.accept(false, "could not add that alert (" + ex.getClass().getSimpleName() + ").");
            }
        }, "cone-alert-remote-add");
        t.setDaemon(true);
        t.start();
    }

    public static String remoteDelete(int n) {
        ConeConfig c = ConfigManager.get();
        AlertRule removed = null;
        synchronized (c.priceAlertRules) {
            if (n >= 1 && n <= c.priceAlertRules.size()) removed = c.priceAlertRules.remove(n - 1);
        }
        if (removed == null) return "there is no rule " + n + ". Run `/alert action:list` first.";
        ConfigManager.save();
        return "removed " + removed.name + " " + removed.condition() + ".";
    }

    public static String remoteClear() {
        ConeConfig c = ConfigManager.get();
        int n;
        synchronized (c.priceAlertRules) {
            n = c.priceAlertRules.size();
            c.priceAlertRules.clear();
        }
        ConfigManager.save();
        return "cleared " + n + " rule" + (n == 1 ? "" : "s") + ".";
    }

    private static String quote(String s) {
        StringBuilder b = new StringBuilder("\"");
        for (char ch : s.toCharArray()) {
            switch (ch) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                case '\n', '\r', '\t' -> b.append(' ');
                default -> {
                    if (ch >= 0x20) b.append(ch);
                }
            }
        }
        return b.append('"').toString();
    }

    private static List<AlertRule> rules(ConeConfig c) {
        synchronized (c.priceAlertRules) {
            return new ArrayList<>(c.priceAlertRules);
        }
    }

    private static void remove(ConeConfig c, AlertRule r) {
        synchronized (c.priceAlertRules) { c.priceAlertRules.remove(r); }
        Minecraft.getInstance().execute(ConfigManager::save);
    }

    public static int count() {
        return ConfigManager.get().priceAlertRules.size();
    }

    public static String summary() {
        ConeConfig c = ConfigManager.get();
        int n = c.priceAlertRules.size();
        if (!c.priceAlertsEnabled) return "Off - " + n + " rule" + (n == 1 ? "" : "s") + " stored";
        if (n == 0) return "No rules yet - add one with /alert";
        return "Watching " + n + " rule" + (n == 1 ? "" : "s")
                + ", every " + clamp(c.priceAlertPollSec, 30, 600) + "s";
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static void show(String msg) {
        showComp(Component.literal(net.cone.command.ConeCommands.route(msg)));
    }

    private static void showComp(Component msg) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (mc.player != null) mc.player.sendSystemMessage(msg);
        });
    }
}
