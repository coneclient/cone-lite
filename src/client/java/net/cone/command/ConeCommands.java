package net.cone.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.cone.config.ConfigManager;
import net.cone.input.Keybinds;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ConeCommands {
    private ConeCommands() {}

    private static final String TAG = "§8[§6CONE§8] ";

    private record Verb(String group, String name, String args, String blurb, String alias,
                        List<String> usage) {
    }

    private static List<Verb> verbs() {
        return VERBS;
    }

    private static final List<Verb> VERBS = List.of(
            new Verb("Market", "price", "<item>",
                    "Show the instant buy and sell price of one item.", "price",
                    List.of("/cone price enchanted lapis")),
            new Verb("Market", "flips", "[key:val]",
                    "Rank bazaar spreads by coins per hour.", "flips",
                    List.of("/cone flips", "/cone flips pct:5 vol:50k n:12", "/cone flips reset")),
            new Verb("Market", "books", "[key:val]",
                    "Rank enchanted-book chains by coins per hour.", "enchantflips",
                    List.of("/cone books", "/cone books roi:8 n:12")),
            new Verb("Market", "alert", "<item> <expr> [once] | list | del <n> | clear | test | on | off",
                    "Say something in chat when a price crosses.", "alert",
                    List.of("/cone alert enchanted lapis sell>1.2m",
                            "/cone alert booster cookie dip>10% once",
                            "/cone alert list",
                            "/cone alert del 2")),
            new Verb("Market", "track", "<item>",
                    "Pin an item to the bazaar HUD.", "track",
                    List.of("/cone track enchanted lapis")),
            new Verb("Market", "untrack", "<item>",
                    "Take an item off the bazaar HUD.", "untrack",
                    List.of("/cone untrack enchanted lapis")),

            new Verb("Client", "menu", "",
                    "Open the Cone menu, same as Right-Shift.", "",
                    List.of("/cone", "/cone menu")),
            new Verb("Client", "help", "[verb]",
                    "This list, or the detail of one verb.", "",
                    List.of("/cone help", "/cone help price"))
    );

    private static final List<String> GROUPS = List.of("Market", "Client");

    public static void register(CommandDispatcher<FabricClientCommandSource> d) {
        LiteralCommandNode<FabricClientCommandSource> root = d.register(tree());
        if (!ConfigManager.get().shortCommands) return;
        for (Verb v : verbs()) {
            if (v.alias().isEmpty()) continue;
            alias(d, v.alias(), root.getChild(v.name()));
        }
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> tree() {
        LiteralArgumentBuilder<FabricClientCommandSource> b = lit("cone")
                .executes(c -> openMenu())
                .then(lit("menu").executes(c -> openMenu()))
                .then(lit("gui").executes(c -> openMenu()))
                .then(lit("help")
                        .executes(c -> { help(""); return 1; })
                        .then(arg("verb").executes(c -> { help(str(c, "verb")); return 1; })))
                .then(price())
                .then(finder("flips", true))
                .then(finder("books", false))
                .then(alert())
                .then(item("track", a -> net.cone.economy.BazaarTracker.track(a)))
                .then(item("untrack", a -> {
                    if (need(a, "untrack")) net.cone.economy.BazaarTracker.untrack(a);
                }))
;

        return b.then(arg("unknown").executes(c -> { unknown(str(c, "unknown")); return 1; }));
    }

    private static void alias(CommandDispatcher<FabricClientCommandSource> d, String name,
                              CommandNode<FabricClientCommandSource> target) {
        if (target == null) return;
        LiteralArgumentBuilder<FabricClientCommandSource> b = lit(name);
        if (target.getCommand() != null) b.executes(target.getCommand());
        d.register(b.redirect(target));
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> price() {
        return lit("price")
                .executes(c -> { net.cone.economy.PriceClient.lookup(""); return 1; })
                .then(arg("item").executes(c -> {
                    net.cone.economy.PriceClient.lookup(str(c, "item"));
                    return 1;
                }));
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> finder(String name, boolean bazaar) {
        return lit(name)
                .executes(c -> { finderRun(bazaar, ""); return 1; })
                .then(lit("reset").executes(c -> { finderRun(bazaar, "reset"); return 1; }))
                .then(arg("filters").executes(c -> { finderRun(bazaar, str(c, "filters")); return 1; }));
    }

    private static void finderRun(boolean bazaar, String filters) {
        if (bazaar) net.cone.economy.PriceClient.printFlips(filters);
        else net.cone.economy.PriceClient.printEnchantFlips(filters);
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> alert() {
        LiteralArgumentBuilder<FabricClientCommandSource> b = lit("alert")
                .executes(c -> { net.cone.economy.PriceAlerts.command("list"); return 1; });
        for (String word : List.of("list", "clear", "test", "on", "off", "help")) {
            b.then(lit(word).executes(c -> { net.cone.economy.PriceAlerts.command(word); return 1; }));
        }
        b.then(lit("del").then(arg("index").executes(c -> {
            net.cone.economy.PriceAlerts.command("del " + str(c, "index").trim());
            return 1;
        })));
        return b.then(arg("arg").executes(c -> {
            net.cone.economy.PriceAlerts.command(str(c, "arg"));
            return 1;
        }));
    }

    public static LiteralArgumentBuilder<FabricClientCommandSource> one(String name, Handler h) {
        return lit(name).executes(c -> { h.run(""); return 1; });
    }

    public static LiteralArgumentBuilder<FabricClientCommandSource> item(String name, Handler h) {
        return lit(name)
                .executes(c -> { h.run(""); return 1; })
                .then(arg("arg").executes(c -> { h.run(str(c, "arg").trim()); return 1; }));
    }

    public interface Handler { void run(String arg); }

    public static boolean need(String arg, String verb) {
        if (!arg.isEmpty()) return true;
        for (Verb v : verbs()) {
            if (v.name().equals(verb)) {
                line(Component.literal(TAG + "§7usage: §f/cone " + verb + " " + v.args()));
                return false;
            }
        }
        return false;
    }

    private static boolean hintDone;
    private static int hintTicks;

    public static void hintOnce() {
        if (ConfigManager.get().helpHintShown) { hintDone = true; return; }
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK
                .register(mc -> {
                    if (hintDone || mc.player == null) return;
                    if (++hintTicks < 100) return;
                    hintDone = true;
                    ConfigManager.get().helpHintShown = true;
                    ConfigManager.save();
                    line(Component.literal(TAG + "§7type ")
                            .append(Component.literal("§f/cone help").withStyle(
                                    hover("§7list every command").withClickEvent(
                                            new ClickEvent.RunCommand("/cone help"))))
                            .append(Component.literal(" §7for every command, or §f/cone §7for the menu.")));
                });
    }

    public static String cmd(String verb) {
        if (ConfigManager.get().shortCommands) {
            for (Verb v : verbs()) {
                if (v.name().equals(verb) && !v.alias().isEmpty()) return "/" + v.alias();
            }
        }
        return "/cone " + verb;
    }

    public static String route(String msg) {
        if (msg == null || msg.isEmpty() || ConfigManager.get().shortCommands) return msg;
        java.util.regex.Matcher m = shortForms().matcher(msg);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(out, java.util.regex.Matcher.quoteReplacement(
                    "/cone " + ALIASES.get(m.group(1))));
        }
        m.appendTail(out);
        return out.toString();
    }

    private static final java.util.Map<String, String> ALIASES = aliasMap();

    private static java.util.regex.Pattern shortPattern;

    private static java.util.Map<String, String> aliasMap() {
        java.util.Map<String, String> m = new java.util.LinkedHashMap<>();
        for (Verb v : verbs()) if (!v.alias().isEmpty()) m.put(v.alias(), v.name());
        return m;
    }

    private static java.util.regex.Pattern shortForms() {
        if (shortPattern == null) {
            List<String> names = new ArrayList<>(ALIASES.keySet());
            names.sort((a, b) -> b.length() - a.length());
            shortPattern = java.util.regex.Pattern.compile("/(" + String.join("|", names) + ")\\b");
        }
        return shortPattern;
    }

    private static void help(String topic) {
        String want = topic == null ? "" : topic.trim().toLowerCase(Locale.ROOT);
        if (!want.isEmpty()) {
            for (Verb v : verbs()) {
                if (v.name().equals(want) || v.alias().equals(want)) { detail(v); return; }
            }
            line(Component.literal(TAG + "§7no verb called §f" + want + "§7."));
        }
        rule();
        line(Component.literal("§6Cone §8- §7type §f/cone §7and press Tab. "
                + (shortOn() ? "§7Short forms still work." : "§8Short forms are off.")));
        for (String group : GROUPS) {
            List<Verb> rows = verbs().stream().filter(v -> v.group().equals(group)).toList();
            if (rows.isEmpty()) continue;
            line(Component.literal(" §6" + group.toLowerCase(Locale.ROOT)));
            for (Verb v : rows) line(row(v));
        }
        line(Component.literal("§8 /cone help <verb> for the detail of one command."));
        rule();
    }

    private static MutableComponent row(Verb v) {
        String head = "  §f/cone " + v.name() + (v.args().isEmpty() ? "" : " §8" + shortArgs(v.args()));
        MutableComponent line = Component.literal(head + " §8- §7" + v.blurb());
        String hover = "§f/cone " + v.name() + (v.args().isEmpty() ? "" : " " + v.args())
                + (showAlias(v) ? "\n§8short form: §7/" + v.alias() : "")
                + "\n§8click to read the detail";
        return line.withStyle(hover(hover).withClickEvent(
                new ClickEvent.RunCommand("/cone help " + v.name())));
    }

    private static String shortArgs(String args) {
        int bar = args.indexOf('|');
        if (bar < 0 || args.length() <= 34) return args;
        String head = args.substring(0, bar).trim();
        return head.startsWith("[") ? head + " ...]" : head + " ...";
    }

    private static void detail(Verb v) {
        rule();
        line(Component.literal("§6/cone " + v.name() + (v.args().isEmpty() ? "" : " §8" + v.args())));
        line(Component.literal(" §7" + v.blurb()));
        if (showAlias(v)) line(Component.literal(" §8short form: §7/" + v.alias()));
        line(Component.literal(" §8examples:"));
        for (String ex : v.usage()) {
            line(Component.literal("  §f" + ex).withStyle(
                    hover("§7click to put this in the chat box")
                            .withClickEvent(new ClickEvent.SuggestCommand(ex))));
        }
        rule();
    }

    private static void unknown(String raw) {
        String word = raw.trim().split("\\s+")[0];
        List<String> near = new ArrayList<>();
        for (Verb v : verbs()) {
            if (v.name().startsWith(word.toLowerCase(Locale.ROOT))) near.add(v.name());
        }

        MutableComponent m = Component.literal(TAG + "§7no verb called §f" + word + "§7. ");
        if (!near.isEmpty()) m.append(Component.literal("§7did you mean §f/cone " + near.get(0) + "§7? "));
        m.append(Component.literal("§8[help]").withStyle(
                hover("§7list every command").withClickEvent(new ClickEvent.RunCommand("/cone help"))));
        line(m);
    }

    private static boolean shortOn() {
        return ConfigManager.get().shortCommands;
    }

    private static boolean showAlias(Verb v) {
        return shortOn() && !v.alias().isEmpty();
    }

    private static Style hover(String text) {
        return Style.EMPTY.withHoverEvent(new HoverEvent.ShowText(Component.literal(text)));
    }

    private static void rule() {
        line(Component.literal("§8§m                                        "));
    }

    private static void line(Component c) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.sendSystemMessage(c);
    }

    private static int openMenu() {
        Keybinds.requestOpenMenu();
        return 1;
    }

    public static LiteralArgumentBuilder<FabricClientCommandSource> lit(String name) {
        return ClientCommands.literal(name);
    }

    public static RequiredArgumentBuilder<FabricClientCommandSource, String> arg(String name) {
        return ClientCommands.argument(name, StringArgumentType.greedyString());
    }

    public static String str(CommandContext<FabricClientCommandSource> c, String name) {
        return StringArgumentType.getString(c, name);
    }
}
