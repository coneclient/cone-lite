package net.cone.economy;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static net.cone.command.ConeCommands.cmd;

/**
 * Bazaar prices, flip finders and enchant-chain finders, all computed on the client from a live
 * Hypixel snapshot ({@link HypixelBazaar}). Nothing here talks to a private backend: the numbers
 * come straight from the public bazaar and the guards run in the jar.
 */
public final class PriceClient {
    public static final double TAX = HypixelBazaar.TAX;

    private PriceClient() {}

    public record Entry(String id, String name, double instaBuy, double instaSell, double avgBuy, double avgSell,
                        double netSell, long buyMovingWk, long sellMovingWk,
                        boolean flagged, String reasons) {
        public double flipMargin() { return instaBuy * (1 - TAX) - instaSell; }
        public double flipPct() { return instaSell > 0 ? flipMargin() / instaSell * 100 : 0; }
    }

    public static final class RateLimited extends RuntimeException {}

    // ---- price lookups ----

    public static List<Entry> fetch(List<String> ids) throws Exception {
        if (ids.isEmpty()) return List.of();
        Map<String, HypixelBazaar.Book> books = HypixelBazaar.books();
        List<Entry> out = new ArrayList<>();
        for (String id : ids) {
            HypixelBazaar.Book b = books.get(id.trim().toUpperCase());
            if (b != null) out.add(toEntry(b));
        }
        return out;
    }

    public static List<Entry> search(String query) throws Exception {
        String q = query.trim().toLowerCase();
        if (q.isEmpty()) return List.of();
        String idGuess = q.toUpperCase().replace(' ', '_');
        Map<String, HypixelBazaar.Book> books = HypixelBazaar.books();

        record Scored(Entry entry, int score) {}
        List<Scored> scored = new ArrayList<>();
        for (HypixelBazaar.Book b : books.values()) {
            int score = matchScore(q, idGuess, b.id(), prettyId(b.id()).toLowerCase());
            if (score > 0) scored.add(new Scored(toEntry(b), score));
        }
        scored.sort((a, c) -> Integer.compare(c.score(), a.score()));
        List<Entry> out = new ArrayList<>();
        for (int i = 0; i < Math.min(scored.size(), 25); i++) out.add(scored.get(i).entry());
        return out;
    }

    private static int matchScore(String q, String idGuess, String id, String name) {
        if (id.equals(idGuess)) return 1000;
        if (name.equals(q)) return 900;
        if (name.startsWith(q)) return 600 - name.length();
        if (id.contains(idGuess)) return 400;
        String[] tokens = q.split("\\s+");
        boolean all = true;
        for (String t : tokens) {
            if (t.isEmpty()) continue;
            if (!name.contains(t)) { all = false; break; }
        }
        if (all) return 200 - name.length();
        if (name.contains(q)) return 50;
        return 0;
    }

    private static Entry toEntry(HypixelBazaar.Book b) {
        List<String> reasons = HypixelBazaar.guard(b);
        return new Entry(b.id(), prettyId(b.id()),
                b.instaBuy(), b.instaSell(), b.avgBuy(), b.avgSell(), b.netSell(),
                b.buyMovingWk(), b.sellMovingWk(),
                !reasons.isEmpty(), String.join(", ", reasons));
    }

    public static void lookup(String item) {
        String query = item.trim();
        if (query.isEmpty()) { show("§8[§6PRICE§8] §7usage: /price <name or id>"); return; }

        Thread t = new Thread(() -> {
            try {
                List<Entry> found = search(query);
                if (found.isEmpty()) { show("§8[§6PRICE§8] §7no bazaar match for §f" + query); return; }
                Entry p = found.get(0);
                show("§8[§6PRICE§8] §f" + p.name());
                show("  §7insta-buy §a" + comma(p.instaBuy()) + " §8| §7insta-sell §a" + comma(p.instaSell())
                        + " §8| §7net §a" + comma(p.netSell()));
                if (p.flagged()) show("  §c⚠ flagged: §7" + p.reasons());
                if (found.size() > 1) {
                    MutableComponent also = Component.literal("  §8also: ");
                    for (int i = 1; i < Math.min(found.size(), 4); i++) {
                        Entry alt = found.get(i);
                        if (i > 1) also.append(Component.literal("§8, "));
                        also.append(Component.literal("§7" + alt.name()).withStyle(s -> s
                                .withClickEvent(new ClickEvent.RunCommand(cmd("price") + " " + alt.id()))
                                .withHoverEvent(new HoverEvent.ShowText(Component.literal("§7show §f" + alt.name())))));
                    }
                    showComp(also);
                }
            } catch (RateLimited e) {
                show("§8[§6PRICE§8] §7Hypixel is throttling - try again in a few seconds.");
            } catch (Exception e) {
                net.cone.ConeClient.LOG.warn("[Cone] /price failed", e);
                show("§8[§6PRICE§8] §7lookup failed §8(" + e.getClass().getSimpleName() + ")");
            }
        }, "cone-price");
        t.setDaemon(true);
        t.start();
    }

    // ---- order flips ----

    public record Flip(String id, String name, double buyOrder, double sellOffer, double margin, double pct,
                       double coinsPerHour, long buyMovingWk, long sellMovingWk) {}

    public static List<Flip> fetchFlips(double minPct, long minVol, double minPrice, double minMargin, int limit)
            throws Exception {
        Map<String, HypixelBazaar.Book> books = HypixelBazaar.books();
        List<Flip> out = new ArrayList<>();
        for (HypixelBazaar.Book b : books.values()) {
            if (b.buyMovingWk() < minVol || b.sellMovingWk() < minVol) continue;
            if (b.instaSell() < minPrice) continue;
            double margin = b.instaBuy() * (1 - TAX) - b.instaSell();
            if (margin < minMargin) continue;
            double pct = b.instaSell() > 0 ? margin / b.instaSell() * 100 : 0;
            if (pct < minPct) continue;
            if (!HypixelBazaar.guard(b).isEmpty()) continue;
            // coins/hour = margin x hourly book turnover (min side of the 7-day volume). It's the
            // profit ceiling if you captured every fill, but ranking by it surfaces real money.
            double vol = Math.min(b.buyMovingWk(), b.sellMovingWk());
            double coinsPerHour = margin * (vol / 168.0);
            // buyOrder = the best buy order to outbid; sellOffer = the best sell offer to undercut.
            out.add(new Flip(b.id(), prettyId(b.id()), b.instaSell(), b.instaBuy(),
                    margin, pct, coinsPerHour, b.buyMovingWk(), b.sellMovingWk()));
        }
        out.sort((a, c) -> Double.compare(c.coinsPerHour(), a.coinsPerHour()));
        return out.size() > limit ? new ArrayList<>(out.subList(0, limit)) : out;
    }

    private static volatile boolean flipsInFlight;
    private static volatile long lastFlipsMs;

    public static void printFlips() { printFlips(""); }

    public static void printFlips(String args) {
        var cfg = net.cone.config.ConfigManager.get();
        boolean changed = false;
        for (String tok : args.trim().toLowerCase().split("\\s+")) {
            if (tok.equals("reset")) {
                cfg.flipMinPct = 3.0; cfg.flipMinVol = 30_000; cfg.flipMinPrice = 10.0;
                cfg.flipMinMargin = 10.0; cfg.flipLimit = 8;
                changed = true;
                continue;
            }
            int sep = Math.max(tok.indexOf(':'), tok.indexOf('='));
            if (sep <= 0) continue;
            String k = tok.substring(0, sep), v = tok.substring(sep + 1);
            try {
                switch (k) {
                    case "minvol", "vol", "volume" -> { cfg.flipMinVol = (long) parseNum(v); changed = true; }
                    case "minpct", "pct", "profit" -> { cfg.flipMinPct = parseNum(v); changed = true; }
                    case "minprice", "price" -> { cfg.flipMinPrice = parseNum(v); changed = true; }
                    case "minmargin", "margin" -> { cfg.flipMinMargin = parseNum(v); changed = true; }
                    case "limit", "n", "top" -> { cfg.flipLimit = Math.max(1, Math.min(25, (int) parseNum(v))); changed = true; }
                    default -> { }
                }
            } catch (NumberFormatException ignored) {
                show("§8[§6FLIPS§8] §7bad value for §f" + k + "§7: " + v);
            }
        }
        if (changed) net.cone.config.ConfigManager.save();

        double minPct = cfg.flipMinPct;
        long minVol = cfg.flipMinVol;
        double minPrice = cfg.flipMinPrice;
        double minMargin = cfg.flipMinMargin;
        int limit = cfg.flipLimit;

        long now = System.currentTimeMillis();
        if (flipsInFlight || now - lastFlipsMs < 400) return;
        flipsInFlight = true;
        lastFlipsMs = now;
        Thread t = new Thread(() -> {
            try {
                List<Flip> flips = fetchFlips(minPct, minVol, minPrice, minMargin, limit);
                if (flips.isEmpty()) {
                    show("§8[§6FLIPS§8] §7nothing matches - loosen filters below (§c-§7 widens):");
                    showComp(filterBar(minPct, minVol, minPrice, minMargin, limit));
                    return;
                }
                show("§8[§6FLIPS§8] §7top §f" + flips.size()
                        + " §7order flips §8(coins/h ranked - name opens the bazaar):");
                for (Flip f : flips) {
                    String name = f.name();
                    String text = String.format("  §e▶ §f%-22s §6~%s/h §a+%.1f%% §7margin §a%s",
                            name, comma(f.coinsPerHour()), f.pct(), comma(f.margin()));
                    Component tip = Component.literal(
                            "§6" + name + "\n§7Click to open in bazaar\n"
                            + "§7buy order §a" + comma(f.buyOrder()) + "\n"
                            + "§7sell offer §a" + comma(f.sellOffer()) + "\n"
                            + "§7margin/unit §a" + comma(f.margin()) + "\n"
                            + "§7weekly vol §b" + comma(Math.min(f.buyMovingWk(), f.sellMovingWk())) + "\n"
                            + "§8coins/h = margin x hourly turnover (ceiling)");
                    MutableComponent line = Component.literal(text).withStyle(s -> s
                            .withClickEvent(new ClickEvent.RunCommand("/bz " + name))
                            .withHoverEvent(new HoverEvent.ShowText(tip)));

                    showComp(line);
                }
                showComp(filterBar(minPct, minVol, minPrice, minMargin, limit));
            } catch (RateLimited e) {
                show("§8[§6FLIPS§8] §7easy - Hypixel is throttling. Wait a few seconds.");
            } catch (Exception e) {
                show("§8[§6FLIPS§8] §7lookup failed §8(" + e.getMessage() + ")");
            } finally {
                flipsInFlight = false;
            }
        }, "cone-flips");
        t.setDaemon(true);
        t.start();
    }

    private static MutableComponent filterBar(double minPct, long minVol, double minPrice,
                                              double minMargin, int limit) {
        MutableComponent bar = Component.literal("  §8⚙ ");
        bar.append(filterChip("vol", "minvol", arg(minVol), arg(minVol / 2.0), arg(minVol * 2.0)));
        bar.append(Component.literal(" §8| "));
        bar.append(filterChip("pct", "minpct", arg(minPct), arg(minPct / 2), arg(minPct * 2)));
        bar.append(Component.literal(" §8| "));
        bar.append(filterChip("price", "minprice", arg(minPrice), arg(minPrice / 2), arg(minPrice * 2)));
        bar.append(Component.literal(" §8| "));
        bar.append(filterChip("margin", "minmargin", arg(minMargin), arg(minMargin / 2), arg(minMargin * 2)));
        bar.append(Component.literal(" §8| "));
        bar.append(filterChip("top", "limit", String.valueOf(limit),
                String.valueOf(Math.max(1, limit - 5)), String.valueOf(Math.min(25, limit + 5))));
        bar.append(Component.literal(" §8| "));
        bar.append(Component.literal("§e↻").withStyle(s -> s
                .withClickEvent(new ClickEvent.RunCommand(cmd("flips")))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal("§7refresh")))));
        bar.append(Component.literal(" §8"));
        bar.append(Component.literal("§8[reset]").withStyle(s -> s
                .withClickEvent(new ClickEvent.RunCommand(cmd("flips") + " reset"))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal("§7restore default filters")))));
        return bar;
    }

    private static MutableComponent filterChip(String label, String key, String cur, String lower, String higher) {
        MutableComponent c = Component.literal("§7" + label + " ");
        c.append(Component.literal("§c-").withStyle(s -> s
                .withClickEvent(new ClickEvent.RunCommand(cmd("flips") + " " + key + ":" + lower))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal("§7" + label + " → §f" + lower)))));
        c.append(Component.literal("§f" + cur).withStyle(s -> s
                .withClickEvent(new ClickEvent.SuggestCommand(cmd("flips") + " " + key + ":" + cur))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal("§7click to type a custom " + label)))));
        c.append(Component.literal("§a+").withStyle(s -> s
                .withClickEvent(new ClickEvent.RunCommand(cmd("flips") + " " + key + ":" + higher))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal("§7" + label + " → §f" + higher)))));
        return c;
    }

    // ---- enchanted-book anvil chains ----

    public record EnchantFlip(String base, String name, int buyLevel, int sellLevel, int need,
                              double buyUnit, double sellUnit, double cost, double net, double roi,
                              double coinsPerHour, long buyMovingWk, long sellMovingWk) {}

    private static final Pattern ENCHANT_ID = Pattern.compile("^(ENCHANTMENT_.+)_(\\d+)$");

    public static List<EnchantFlip> fetchEnchantFlips(double minRoi, double minNet, long minVol, int limit)
            throws Exception {
        // The caller passes the ROI floor as a percent; compare against the fraction.
        double roiFloor = minRoi / 100.0;
        long minSellVol = 50;
        int maxSteps = 4;

        Map<String, HypixelBazaar.Book> books = HypixelBazaar.books();

        // Group by base enchant id -> level -> book. Ban level 6+ (mostly combine-blocked or too thin).
        Map<String, Map<Integer, HypixelBazaar.Book>> byEnchant = new java.util.HashMap<>();
        for (HypixelBazaar.Book b : books.values()) {
            if (!b.id().startsWith("ENCHANTMENT_")) continue;
            Matcher m = ENCHANT_ID.matcher(b.id());
            if (!m.matches()) continue;
            int lvl = Integer.parseInt(m.group(2));
            if (lvl < 1 || lvl > 5) continue;
            byEnchant.computeIfAbsent(m.group(1), k -> new java.util.HashMap<>()).put(lvl, b);
        }

        List<EnchantFlip> out = new ArrayList<>();
        for (Map.Entry<String, Map<Integer, HypixelBazaar.Book>> e : byEnchant.entrySet()) {
            String base = e.getKey();
            Map<Integer, HypixelBazaar.Book> levels = e.getValue();
            List<Integer> present = new ArrayList<>(levels.keySet());
            java.util.Collections.sort(present);

            int buyLevel = present.get(0);
            HypixelBazaar.Book buyRow = levels.get(buyLevel);
            double buyUnit = buyRow.instaSell(); // source inputs via a buy order (fills near instaSell)
            if (buyUnit <= 0) continue;
            if (buyRow.buyMovingWk() < minVol) continue;

            // Highest level within maxSteps of the base that is liquid + un-flagged. A rare high book
            // is expected to be thin, so illiquid / thin-top are tolerated on the exit leg only.
            int sellLevel = -1;
            for (int L = buyLevel + maxSteps; L > buyLevel; L--) {
                HypixelBazaar.Book r = levels.get(L);
                if (r == null) continue;
                if (r.instaBuy() <= 0 || r.sellMovingWk() < minSellVol) continue;
                boolean fatal = HypixelBazaar.guard(r).stream()
                        .anyMatch(x -> !x.startsWith("illiquid") && !x.startsWith("thin top"));
                if (fatal) continue;
                sellLevel = L;
                break;
            }
            if (sellLevel < 0) continue;

            HypixelBazaar.Book sellRow = levels.get(sellLevel);
            // The entry leg is bought in bulk, so its book has to be clean end to end.
            if (!HypixelBazaar.guard(buyRow).isEmpty()) continue;

            double sellUnit = sellRow.instaBuy(); // sell the crafted book via a sell offer
            int need = 1 << (sellLevel - buyLevel); // base books per one level-S book
            double cost = need * buyUnit;
            double revenue = sellUnit * (1 - TAX);
            double net = revenue - cost;
            double roi = cost > 0 ? net / cost : 0;
            if (net < minNet || roi < roiFloor) continue;

            // Coins/hour capped by how fast one crafter can run the chain, then by market throughput.
            double secsPerCombine = 3, fillOverheadSecs = 120;
            double perChainSecs = fillOverheadSecs + (need - 1) * secsPerCombine;
            double craftChainsPerHour = 3600 / perChainSecs;
            double marketChainsPerHour =
                    Math.min(buyRow.buyMovingWk() / (double) need, sellRow.sellMovingWk()) / 168.0;
            double coinsPerHour = net * Math.min(craftChainsPerHour, marketChainsPerHour);

            out.add(new EnchantFlip(base, enchantName(base), buyLevel, sellLevel, need,
                    buyUnit, sellUnit, cost, net, roi, coinsPerHour,
                    buyRow.buyMovingWk(), sellRow.sellMovingWk()));
        }
        out.sort((a, c) -> Double.compare(c.coinsPerHour(), a.coinsPerHour()));
        return out.size() > limit ? new ArrayList<>(out.subList(0, limit)) : out;
    }

    private static volatile boolean enchInFlight;
    private static volatile long lastEnchMs;

    private static final String[] ROMAN =
            {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};

    public static void printEnchantFlips() { printEnchantFlips(""); }

    public static void printEnchantFlips(String args) {
        var cfg = net.cone.config.ConfigManager.get();
        boolean changed = false;
        for (String tok : args.trim().toLowerCase().split("\\s+")) {
            if (tok.equals("reset")) {
                cfg.enchantMinRoi = 5.0; cfg.enchantMinNet = 10_000; cfg.enchantMinVol = 5_000; cfg.enchantLimit = 8;
                changed = true;
                continue;
            }
            int sep = Math.max(tok.indexOf(':'), tok.indexOf('='));
            if (sep <= 0) continue;
            String k = tok.substring(0, sep), v = tok.substring(sep + 1);
            try {
                switch (k) {
                    case "minroi", "roi" -> { cfg.enchantMinRoi = parseNum(v); changed = true; }
                    case "minnet", "net", "profit" -> { cfg.enchantMinNet = parseNum(v); changed = true; }
                    case "minvol", "vol", "volume" -> { cfg.enchantMinVol = (long) parseNum(v); changed = true; }
                    case "limit", "n", "top" -> { cfg.enchantLimit = Math.max(1, Math.min(25, (int) parseNum(v))); changed = true; }
                    default -> { }
                }
            } catch (NumberFormatException ignored) {
                show("§8[§6ENCHANT§8] §7bad value for §f" + k + "§7: " + v);
            }
        }
        if (changed) net.cone.config.ConfigManager.save();

        double minRoi = cfg.enchantMinRoi, minNet = cfg.enchantMinNet;
        long minVol = cfg.enchantMinVol;
        int limit = cfg.enchantLimit;
        long now = System.currentTimeMillis();
        if (enchInFlight || now - lastEnchMs < 400) return;
        enchInFlight = true;
        lastEnchMs = now;
        Thread t = new Thread(() -> {
            try {
                List<EnchantFlip> flips = fetchEnchantFlips(minRoi, minNet, minVol, limit);
                if (flips.isEmpty()) {
                    show("§8[§6ENCHANT§8] §7no profitable book chains right now - loosen with §f/enchantflips roi:2 net:5k");
                    return;
                }
                show("§8[§6ENCHANT§8] §7top §f" + flips.size()
                        + " §7anvil book flips §8(coins/h ranked):");
                for (EnchantFlip f : flips) {
                    String chain = ROMAN[f.buyLevel()] + "§8→§f" + ROMAN[Math.min(f.sellLevel(), 10)];
                    String text = String.format("  §e▶ §f%-16s §7%s §6~%s/h §a+%s §8(%.0f%%)",
                            f.name(), chain, comma(f.coinsPerHour()), comma(f.net()), f.roi() * 100);
                    Component tip = Component.literal(
                            "§6" + f.name() + " " + ROMAN[f.buyLevel()] + " → " + ROMAN[Math.min(f.sellLevel(), 10)] + "\n"
                            + "§7buy §f" + f.need() + "x §7@ §a" + comma(f.buyUnit()) + "/book\n"
                            + "§7combines §f" + (f.need() - 1) + "\n"
                            + "§7cost §a" + comma(f.cost()) + " §8-> §7sell §a" + comma(f.sellUnit()) + "\n"
                            + "§7net after tax §a" + comma(f.net()) + " §8(" + String.format("%.0f%%", f.roi() * 100) + ")\n"
                            + "§7weekly vol §b" + comma(Math.min(f.buyMovingWk(), f.sellMovingWk())));
                    showComp(Component.literal(text).withStyle(s -> s.withHoverEvent(new HoverEvent.ShowText(tip))));
                }
            } catch (RateLimited e) {
                show("§8[§6ENCHANT§8] §7easy - Hypixel is throttling. Wait a few seconds.");
            } catch (Exception e) {
                show("§8[§6ENCHANT§8] §7lookup failed §8(" + e.getMessage() + ")");
            } finally {
                enchInFlight = false;
            }
        }, "cone-enchantflips");
        t.setDaemon(true);
        t.start();
    }

    // ---- formatting ----

    private static String arg(double v) {
        if (v >= 1_000_000) return trimNum(v / 1_000_000) + "m";
        if (v >= 1_000) return trimNum(v / 1_000) + "k";
        return trimNum(v);
    }

    private static String trimNum(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.format("%.1f", v);
    }

    private static double parseNum(String s) {
        s = s.trim();
        double mult = 1;
        char last = s.isEmpty() ? ' ' : s.charAt(s.length() - 1);
        if (last == 'k' || last == 'K') { mult = 1_000; s = s.substring(0, s.length() - 1); }
        else if (last == 'm' || last == 'M') { mult = 1_000_000; s = s.substring(0, s.length() - 1); }
        else if (last == 'b' || last == 'B') { mult = 1_000_000_000; s = s.substring(0, s.length() - 1); }
        return Double.parseDouble(s) * mult;
    }

    /** ENCHANTMENT_SHARPNESS_5 -> "Sharpness V"; other ids -> title-cased words. */
    private static String prettyId(String id) {
        Matcher m = ENCHANT_ID.matcher(id);
        if (m.matches()) {
            String body = m.group(1).replaceFirst("^ENCHANTMENT_", "").replaceFirst("^ULTIMATE_", "");
            int lvl = Integer.parseInt(m.group(2));
            String rn = lvl >= 0 && lvl < ROMAN.length ? ROMAN[lvl] : String.valueOf(lvl);
            return prettyWords(body) + (rn.isEmpty() ? "" : " " + rn);
        }
        return prettyWords(id);
    }

    private static String enchantName(String base) {
        return prettyWords(base.replaceFirst("^ENCHANTMENT_", "").replaceFirst("^ULTIMATE_", ""));
    }

    private static String prettyWords(String snake) {
        String[] parts = snake.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return sb.toString();
    }

    public static String comma(double v) {
        if (Math.abs(v) >= 1_000_000) return String.format("%.2fM", v / 1_000_000);
        if (Math.abs(v) >= 10_000) return String.format("%.1fk", v / 1_000);
        return String.format("%,.1f", v);
    }

    public static String commaInt(long v) {
        if (Math.abs(v) >= 1_000_000) return String.format("%.2fM", v / 1_000_000.0);
        if (Math.abs(v) >= 10_000) return String.format("%.1fk", v / 1_000.0);
        return String.format("%,d", v);
    }

    private static void show(String line) {
        showComp(Component.literal(net.cone.command.ConeCommands.route(line)));
    }

    private static void showComp(Component comp) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (mc.player != null) mc.player.sendSystemMessage(comp);
        });
    }
}
