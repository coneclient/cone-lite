package net.cone.gui.skija;

import com.mojang.blaze3d.platform.InputConstants;
import net.cone.config.ConeConfig;
import net.cone.config.ConfigManager;
import net.cone.core.ConeCore;
import net.cone.gui.skija.ui.Button;
import net.cone.gui.skija.ui.Checkbox;
import net.cone.gui.skija.ui.ColorPicker;
import net.cone.gui.skija.ui.Container;
import net.cone.gui.skija.ui.Header;
import net.cone.gui.skija.ui.KeybindButton;
import net.cone.gui.skija.ui.Label;
import net.cone.gui.skija.ui.LiveLabel;
import net.cone.gui.skija.ui.NumberBox;
import net.cone.gui.skija.ui.RangeSlider;
import net.cone.gui.skija.ui.Row;
import net.cone.gui.skija.ui.Select;
import net.cone.gui.skija.ui.Slider;
import net.cone.gui.skija.ui.TextField;
import net.cone.gui.skija.ui.Toggle;
import net.cone.gui.skija.ui.Widget;
import net.cone.input.Keybinds;
import net.cone.skyblock.SkyblockData;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleFunction;
import java.util.function.Function;
import java.util.function.Supplier;

final class SkijaModel {
    private SkijaModel() {}

    static final ConeConfig DEFAULTS = new ConeConfig();

    static final class Category {
        final String name;
        final List<Module> modules;
        boolean startsMore;
        Category(String name, List<Module> modules) {
            this.name = name;
            this.modules = modules;
        }
        int onCount() {
            int n = 0;
            for (Module m : modules) if (m.master != null && m.master.getAsBoolean()) n++;
            return n;
        }
    }

    static final class Module {
        final String id;
        final String name;
        final String desc;
        final BooleanSupplier master;
        final Runnable toggle;
        final Container body;

        Module(String id, String name, String desc, BooleanSupplier master, Runnable toggle, Container body) {
            this.id = id;
            this.name = name;
            this.desc = desc;
            this.master = master;
            this.toggle = toggle;
            this.body = body;
            List<Widget> all = new ArrayList<>();
            body.collect(all);
            for (Widget w : all) {
                if (!w.searchLabel.isEmpty() && w.pinId == null) w.pinId = id + "/" + w.searchLabel;
            }
        }

        boolean matches(String q) {
            if (name.toLowerCase(Locale.ROOT).contains(q)) return true;
            if (desc.toLowerCase(Locale.ROOT).contains(q)) return true;
            List<Widget> all = new ArrayList<>();
            body.collect(all);
            for (Widget w : all) {
                if (w.searchLabel.toLowerCase(Locale.ROOT).contains(q)) return true;
                if (w.tooltip.toLowerCase(Locale.ROOT).contains(q)) return true;
            }
            return false;
        }
    }

    static ConeConfig cfg() {
        return ConfigManager.get();
    }

    static void save() {
        ConfigManager.save();
    }

    static Container body(Widget... rows) {
        Container c = new Container();
        for (Widget w : rows) c.add(w);
        return c;
    }

    static final DoubleFunction<String> INT = v -> String.format("%d", Math.round(v));
    static final DoubleFunction<String> F1 = v -> String.format("%.1f", v);
    static final DoubleFunction<String> F2 = v -> String.format("%.2f", v);
    static final DoubleFunction<String> PCT = v -> String.format("%.0f%%", v * 100);

    static final DoubleFunction<String> COMPACT = v -> {
        double a = Math.abs(v);
        if (a >= 1_000_000_000) return trim(v / 1_000_000_000) + "B";
        if (a >= 1_000_000) return trim(v / 1_000_000) + "M";
        if (a >= 1_000) return trim(v / 1_000) + "K";
        return String.format("%d", Math.round(v));
    };

    static String trim(double v) {
        return Math.abs(v - Math.rint(v)) < 0.05
                ? String.format("%d", Math.round(v))
                : String.format("%.1f", v);
    }

    static Toggle t(String label, String desc,
                            Function<ConeConfig, Boolean> get, BiConsumer<ConeConfig, Boolean> set) {
        Toggle w = new Toggle(label, () -> get.apply(cfg()), v -> { set.accept(cfg(), v); save(); });
        meta(w, desc,
                () -> !get.apply(cfg()).equals(get.apply(DEFAULTS)),
                () -> { set.accept(cfg(), get.apply(DEFAULTS)); save(); });
        return w;
    }

    static Checkbox chk(String label, String desc,
                                Function<ConeConfig, Boolean> get, BiConsumer<ConeConfig, Boolean> set) {
        Checkbox w = new Checkbox(label, () -> get.apply(cfg()),
                v -> { set.accept(cfg(), v); save(); ConfigManager.apply(); });
        meta(w, desc,
                () -> !get.apply(cfg()).equals(get.apply(DEFAULTS)),
                () -> { set.accept(cfg(), get.apply(DEFAULTS)); save(); ConfigManager.apply(); });
        return w;
    }

    static Slider sl(String label, String desc, double min, double max, double step,
                             Function<ConeConfig, Double> get, BiConsumer<ConeConfig, Double> set,
                             DoubleFunction<String> fmt) {
        Slider w = new Slider(label, min, max, step,
                () -> get.apply(cfg()), v -> { set.accept(cfg(), v); save(); }, fmt);
        meta(w, desc,
                () -> Math.abs(get.apply(cfg()) - get.apply(DEFAULTS)) > 1e-9,
                () -> { set.accept(cfg(), get.apply(DEFAULTS)); save(); });
        return w;
    }

    static RangeSlider rg(String label, String desc, double min, double max, double step,
                                  Function<ConeConfig, Double> getLo, BiConsumer<ConeConfig, Double> setLo,
                                  Function<ConeConfig, Double> getHi, BiConsumer<ConeConfig, Double> setHi,
                                  DoubleFunction<String> fmt) {
        RangeSlider w = new RangeSlider(label, min, max, step,
                () -> getLo.apply(cfg()), v -> { setLo.accept(cfg(), v); save(); },
                () -> getHi.apply(cfg()), v -> { setHi.accept(cfg(), v); save(); }, fmt);
        meta(w, desc,
                () -> Math.abs(getLo.apply(cfg()) - getLo.apply(DEFAULTS)) > 1e-9
                        || Math.abs(getHi.apply(cfg()) - getHi.apply(DEFAULTS)) > 1e-9,
                () -> {
                    setLo.accept(cfg(), getLo.apply(DEFAULTS));
                    setHi.accept(cfg(), getHi.apply(DEFAULTS));
                    save();
                });
        return w;
    }

    static Select sel(String label, String desc, List<String> options,
                              Function<ConeConfig, String> get, BiConsumer<ConeConfig, String> set) {
        Select w = new Select(label, options, () -> get.apply(cfg()),
                v -> { set.accept(cfg(), v); save(); });
        meta(w, desc,
                () -> !get.apply(cfg()).equals(get.apply(DEFAULTS)),
                () -> { set.accept(cfg(), get.apply(DEFAULTS)); save(); });
        return w;
    }

    static TextField txt(String label, String desc, String placeholder,
                                 Function<ConeConfig, String> get, BiConsumer<ConeConfig, String> set) {
        TextField w = new TextField(label, placeholder, () -> get.apply(cfg()),
                v -> { set.accept(cfg(), v); save(); });
        meta(w, desc,
                () -> !get.apply(cfg()).equals(get.apply(DEFAULTS)),
                () -> { set.accept(cfg(), get.apply(DEFAULTS)); save(); });
        return w;
    }

    static NumberBox num(String label, String desc, double min, double max, double step,
                                 Function<ConeConfig, Double> get, BiConsumer<ConeConfig, Double> set,
                                 DoubleFunction<String> fmt) {
        NumberBox w = new NumberBox(label, min, max, step,
                () -> get.apply(cfg()), v -> { set.accept(cfg(), v); save(); }, fmt);
        meta(w, desc,
                () -> Math.abs(get.apply(cfg()) - get.apply(DEFAULTS)) > 1e-9,
                () -> { set.accept(cfg(), get.apply(DEFAULTS)); save(); });
        return w;
    }

    static Toggle tSk(String label, String desc,
                              Function<ConeConfig, Boolean> get, BiConsumer<ConeConfig, Boolean> set) {
        Toggle w = new Toggle(label, () -> get.apply(cfg()),
                v -> { set.accept(cfg(), v); save(); SkDesign.refresh(cfg()); });
        meta(w, desc,
                () -> !get.apply(cfg()).equals(get.apply(DEFAULTS)),
                () -> { set.accept(cfg(), get.apply(DEFAULTS)); save(); SkDesign.refresh(cfg()); });
        return w;
    }

    static Slider slSk(String label, String desc, double min, double max, double step,
                               Function<ConeConfig, Double> get, BiConsumer<ConeConfig, Double> set,
                               DoubleFunction<String> fmt) {
        Slider w = new Slider(label, min, max, step, () -> get.apply(cfg()),
                v -> { set.accept(cfg(), v); save(); SkDesign.refresh(cfg()); }, fmt);
        meta(w, desc,
                () -> Math.abs(get.apply(cfg()) - get.apply(DEFAULTS)) > 1e-9,
                () -> { set.accept(cfg(), get.apply(DEFAULTS)); save(); SkDesign.refresh(cfg()); });
        return w;
    }

    static Select selSk(String label, String desc, List<String> options,
                                Function<ConeConfig, String> get, BiConsumer<ConeConfig, String> set) {
        Select w = new Select(label, options, () -> get.apply(cfg()),
                v -> { set.accept(cfg(), v); save(); SkDesign.refresh(cfg()); });
        meta(w, desc,
                () -> !get.apply(cfg()).equals(get.apply(DEFAULTS)),
                () -> { set.accept(cfg(), get.apply(DEFAULTS)); save(); SkDesign.refresh(cfg()); });
        return w;
    }

    static void meta(Widget w, String desc, BooleanSupplier changed, Runnable reset) {
        w.tooltip = desc == null ? "" : desc;
        w.changed = changed;
        w.reset = reset;
    }

    static String pendingProfile = "";
    static final Map<String, String> renameBuf = new HashMap<>();

    private enum Sort { NEWEST, NAME, SIZE }
    static Sort sort = Sort.NEWEST;
    static String catFilter = "All";

    @SafeVarargs
    static List<Module> join(List<Module>... parts) {
        List<Module> out = new ArrayList<>();
        for (List<Module> p : parts) out.addAll(p);
        return out;
    }

    static java.util.function.Supplier<List<Category>> proCategories = List::of;

    static java.util.function.Supplier<List<Module>> proClientModules = List::of;

    static List<Category> build() {
        List<Category> cats = new ArrayList<>();

        cats.add(new Category("Dashboard", List.of()));

        cats.addAll(proCategories.get());
        cats.add(new Category("Market", marketModules()));

        Category pinned = new Category("Pinned", List.of());
        pinned.startsMore = true;
        cats.add(pinned);

        cats.add(new Category("Uptime", uptimeModules()));
        cats.add(new Category("HUD", List.of(hudModule())));

        cats.add(new Category("Client",
                join(miscModules(), proClientModules.get(), List.of(controlsModule()),
                        profileModules())));

        cats.add(new Category("Appearance", appearanceModules()));

        int pi = cats.indexOf(pinned);
        Category filled = pinnedCategory(cats);
        filled.startsMore = true;
        cats.set(pi, filled);
        return cats;
    }

    static List<Module> uptimeModules() {
        List<Module> out = new ArrayList<>();

        out.add(new Module("up.reconnect", "Auto Reconnect",
                "Rejoins the server after any drop (timeout, kick, restart).",
                () -> cfg().autoReconnect,
                () -> { cfg().autoReconnect = !cfg().autoReconnect; save(); },
                body(
                        sl("Delay (s)", "Seconds to wait before rejoining", 5, 60, 1,
                                c -> (double) c.reconnectDelaySec,
                                (c, v) -> c.reconnectDelaySec = (int) Math.round(v), INT),
                        sl("Max tries", "Give up after this many failed rejoins (0 = never)", 0, 20, 1,
                                c -> (double) c.reconnectMaxTries,
                                (c, v) -> c.reconnectMaxTries = (int) Math.round(v), INT),
                        t("Resume rails after", "Put the autopilot / flip rails back to work after a rejoin",
                                c -> c.resumeRailsAfterReconnect, (c, v) -> c.resumeRailsAfterReconnect = v)
                )));

        out.add(new Module("up.autosb", "Auto Skyblock",
                "Runs /skyblock after joining Hypixel, so a rejoin lands back on the island.",
                () -> cfg().autoJoinSkyblock,
                () -> { cfg().autoJoinSkyblock = !cfg().autoJoinSkyblock; save(); },
                body(
                        sl("Lobby delay (s)", "Seconds in the lobby before sending /skyblock", 1, 20, 1,
                                c -> c.skyblockJoinDelaySec,
                                (c, v) -> c.skyblockJoinDelaySec = v, INT)
                )));

        out.add(new Module("up.rest", "Dynamic Rest",
                "Logs off for a break on a human-looking schedule, then resumes.",
                () -> cfg().dynamicRest,
                () -> { cfg().dynamicRest = !cfg().dynamicRest; save(); },
                body(
                        rg("Session (h)", "Play stretch before a break", 0.25, 6, 0.25,
                                c -> c.restSessionMinHours, (c, v) -> c.restSessionMinHours = v,
                                c -> c.restSessionMaxHours, (c, v) -> c.restSessionMaxHours = v, F2),
                        rg("Break (min)", "Logged-off break length", 5, 90, 1,
                                c -> c.restBreakMinMinutes, (c, v) -> c.restBreakMinMinutes = v,
                                c -> c.restBreakMaxMinutes, (c, v) -> c.restBreakMaxMinutes = v, INT)
                )));

        out.add(new Module("up.breaks", "Micro Breaks",
                "Short in-place pauses while a macro runs.",
                () -> cfg().breaksEnabled,
                () -> { cfg().breaksEnabled = !cfg().breaksEnabled; save(); },
                body(
                        rg("Every (min)", "Gap between breaks", 5, 120, 1,
                                c -> c.breakMinMinutes, (c, v) -> c.breakMinMinutes = v,
                                c -> c.breakMaxMinutes, (c, v) -> c.breakMaxMinutes = v, INT),
                        rg("Pause (s)", "How long each pause lasts", 1, 60, 1,
                                c -> c.breakMinSeconds, (c, v) -> c.breakMinSeconds = v,
                                c -> c.breakMaxSeconds, (c, v) -> c.breakMaxSeconds = v, INT)
                )));

        return out;
    }

    static Module hudModule() {
        List<Widget> rows = new ArrayList<>(List.of(
                new Button(() -> "Move HUD", Button.Style.PRIMARY, () -> {
                    Minecraft mc = Minecraft.getInstance();
                    mc.setScreen(new net.cone.gui.hud.HudEditScreen(mc.screen));
                }, false),
                t("Panel background", "Draw the panel behind the HUD text",
                        c -> c.hudBackground, (c, v) -> c.hudBackground = v),
                t("Show position", "Show live XYZ / yaw in the HUD",
                        c -> c.hudShowPos, (c, v) -> c.hudShowPos = v),
                t("Expanded stats", "Show powders/garden stats + session (off = compact)",
                        c -> c.hudExpanded, (c, v) -> c.hudExpanded = v),
                sl("Scale", "Size of the overlay (1.0 = normal)", 0.5, 2.0, 0.1,
                        c -> (double) c.hudScale, (c, v) -> c.hudScale = v.floatValue(), F1)));

        return new Module("hud.status", "Status HUD",
                "On-screen state, region and session overlay.",
                () -> cfg().hudEnabled,
                () -> { cfg().hudEnabled = !cfg().hudEnabled; save(); },
                body(rows.toArray(new Widget[0])));
    }

    static String money(double v) {
        return (v >= 0 ? "+" : "-") + comma(Math.abs(v));
    }

    static String comma(double v) {
        return net.cone.economy.PriceClient.comma(v);
    }

    static List<Module> marketModules() {
        List<Module> out = new ArrayList<>();

        out.add(new Module("eco.alerts", "Price Alerts",
                "Tells you when a bazaar price crosses a line you drew. Reads only, never clicks.",
                () -> cfg().priceAlertsEnabled,
                () -> { cfg().priceAlertsEnabled = !cfg().priceAlertsEnabled; save(); },
                body(
                        new LiveLabel(net.cone.economy.PriceAlerts::summary),
                        sl("Check every (s)", "One batched request covers every rule, so this is "
                                + "the load on the price API", 30, 600, 15,
                                c -> (double) c.priceAlertPollSec,
                                (c, v) -> c.priceAlertPollSec = (int) Math.round(v), INT),
                        sl("Cooldown (min)", "How long a rule stays quiet after it fires",
                                1, 120, 1, c -> (double) c.priceAlertCooldownMin,
                                (c, v) -> c.priceAlertCooldownMin = (int) Math.round(v), INT),
                        t("Sound", "Play a sound when an alert fires",
                                c -> c.priceAlertSound, (c, v) -> c.priceAlertSound = v),
                        new Button("Show my alerts", Button.Style.PRIMARY,
                                () -> net.cone.economy.PriceAlerts.command("list")),
                        new Button("Fire a test alert", Button.Style.GHOST,
                                () -> net.cone.economy.PriceAlerts.command("test")),
                        new Button(() -> "Remove every alert", Button.Style.DANGER,
                                () -> net.cone.economy.PriceAlerts.command("clear"), false),
                        new Label("/alert <item> sell>1.2m fires when the insta-sell price passes 1.2m.",
                                Label.Tone.MUTED),
                        new Label("Also buy<900k, spread>6%, spike>10%, dip>10%. Add \"once\" to fire "
                                + "a rule a single time.", Label.Tone.MUTED),
                        new Label("Alerts fire on the crossing, not for every check the price stays "
                                + "over the line.", Label.Tone.MUTED)
                )));

        out.add(new Module("eco.bazaar", "Bazaar HUD",
                "Overlay of pinned bazaar prices (live insta-sell).",
                () -> cfg().bazaarHudEnabled,
                () -> { cfg().bazaarHudEnabled = !cfg().bazaarHudEnabled; save(); },
                body(
                        new LiveLabel(() -> "Tracking " + cfg().bazaarHudItems.size() + " item(s)"),
                        new Button("Clear tracked items", Button.Style.GHOST,
                                () -> { cfg().bazaarHudItems.clear(); save(); }),
                        new Label("Pin items with /track <item>, remove with /untrack.", Label.Tone.MUTED),
                        new Label("Prices refresh from the server every ~45s.", Label.Tone.MUTED),
                        new Button("Move HUD", Button.Style.GHOST, () -> {
                            var mc = net.minecraft.client.Minecraft.getInstance();
                            mc.setScreen(new net.cone.gui.hud.HudEditScreen(mc.screen));
                        })
                )));

        return out;
    }

    static List<Module> miscModules() {
        List<Module> out = new ArrayList<>();

        out.add(new Module("misc.cmds", "Short Commands",
                "Register /price, /flips and the rest beside /cone.",
                () -> cfg().shortCommands,
                () -> { cfg().shortCommands = !cfg().shortCommands; save(); },
                body(
                        new Label("Every command also answers as /cone <verb>. Type /cone help.",
                                Label.Tone.MUTED),
                        new Label("Turn this off to leave those names to another mod.",
                                Label.Tone.MUTED),
                        new Label("Commands register at startup, so restart Minecraft to apply.",
                                Label.Tone.MUTED)
                )));

        out.add(new Module("misc.aim", "Aim",
                "How the client eases the view when it has to turn.",
                null, null,
                body(
                        sel("Mode", "Instant snap through Human (slow, eased, most legit-looking)",
                                List.of("Instant", "Fast", "Smooth", "Human"),
                                c -> c.aimMode, (c, v) -> c.aimMode = v)
                )));

        out.add(new Module("misc.nick", "Nickname",
                "Shows this name everywhere clientside: chat, tab list, nametag.",
                () -> cfg().nickEnabled,
                () -> { cfg().nickEnabled = !cfg().nickEnabled; save(); },
                body(
                        txt("Name", "Only on your client: no one else sees it", "your alias",
                                c -> c.nickName, (c, v) -> c.nickName = v)
                )));

        return out;
    }

    static Module controlsModule() {
        Container b = new Container();
        b.add(new Label("Click a row, then press the new key. Esc cancels.", Label.Tone.MUTED));
        for (Keybinds.Bind bind : Keybinds.binds()) {
            KeybindButton kb = new KeybindButton(bind.label(),
                    () -> bind.mapping().getTranslatedKeyMessage().getString(),
                    code -> Keybinds.rebind(bind.mapping(), InputConstants.Type.KEYSYM.getOrCreate(code)),
                    button -> {
                        Keybinds.rebind(bind.mapping(), InputConstants.Type.MOUSE.getOrCreate(button));
                        return true;
                    });
            kb.tooltip = "Rebind \"" + bind.label() + "\"";
            b.add(kb);
        }
        return new Module("ctl.keys", "Keybinds",
                "Every Cone key, rebindable. Per-route hotkeys live on the route cards in Macros.",
                null, null, b);
    }

    static List<Module> profileModules() {
        List<Module> out = new ArrayList<>();
        var pm = ConeCore.profiles();

        Container manage = new Container();
        manage.add(new Row()
                .add(new TextField("Profile name", "name it",
                        () -> pendingProfile, v -> pendingProfile = v, true), 2f)
                .add(new Button(() -> "Save", Button.Style.PRIMARY, () -> {
                    if (!pendingProfile.isBlank()) {
                        pm.save(pendingProfile.trim());
                        pendingProfile = "";
                    }
                }, true), 1f));
        Toggle auto = new Toggle("Auto-switch by region", pm::autoSwitch,
                v -> pm.setAutoSwitch(v), true);
        auto.tooltip = "Load a profile automatically when you change region";
        manage.add(auto);
        if (pm.autoSwitch() && !pm.list().isEmpty()) {
            manage.add(new Header("Region profile"));
            List<String> opts = new ArrayList<>();
            opts.add("(none)");
            opts.addAll(pm.list());
            SkyblockData.Region[] regions = {
                    SkyblockData.Region.DWARVEN_MINES,
                    SkyblockData.Region.CRYSTAL_HOLLOWS,
                    SkyblockData.Region.GARDEN,
                    SkyblockData.Region.HUB,
            };
            for (SkyblockData.Region region : regions) {
                Select ddd = new Select(region.label, opts,
                        () -> {
                            String m = pm.regionProfile(region);
                            return m.isEmpty() ? "(none)" : m;
                        },
                        v -> pm.setRegionProfile(region, v.equals("(none)") ? "" : v));
                ddd.tooltip = "Profile loaded on entering " + region.label;
                manage.add(ddd);
            }
        }
        out.add(new Module("prof.manage", "Manage",
                "Snapshot every setting as a named profile.",
                null, null, manage));

        for (String nm : pm.list()) {
            out.add(new Module("prof." + nm, nm,
                    "Flip the switch to load this profile.",
                    () -> nm.equals(pm.current()),
                    () -> pm.load(nm),
                    body(new Button(() -> "Delete this profile", Button.Style.DANGER,
                            () -> pm.delete(nm), true))));
        }
        return out;
    }

    static List<Module> appearanceModules() {
        List<Module> out = new ArrayList<>();

        ColorPicker accent = new ColorPicker("Accent",
                () -> cfg().skijaAccentHue,
                v -> { cfg().skijaAccentHue = v; save(); SkDesign.refresh(cfg()); },
                () -> cfg().skijaAccentSat,
                v -> { cfg().skijaAccentSat = v; save(); SkDesign.refresh(cfg()); });
        accent.tooltip = "The one color this menu is allowed to shout in";
        accent.changed = () -> Math.abs(cfg().skijaAccentHue - DEFAULTS.skijaAccentHue) > 1e-9
                || Math.abs(cfg().skijaAccentSat - DEFAULTS.skijaAccentSat) > 1e-9;
        accent.reset = () -> {
            cfg().skijaAccentHue = DEFAULTS.skijaAccentHue;
            cfg().skijaAccentSat = DEFAULTS.skijaAccentSat;
            save();
            SkDesign.refresh(cfg());
        };
        out.add(new Module("ui.accent", "Accent",
                "The one color this menu is allowed to shout in.",
                null, null,
                body(
                        accent,
                        tSk("Tinted hero cards", "Wash the lead card in the accent's deep tone",
                                c -> c.skijaHeroTint, (c, v) -> c.skijaHeroTint = v)
                )));

        out.add(new Module("ui.canvas", "Canvas",
                "Surfaces, frost and how the menu sits over the world.",
                null, null,
                body(
                        tSk("Frosted background", "Blur the world behind the menu",
                                c -> c.skijaFrost, (c, v) -> c.skijaFrost = v),
                        tSk("Glass sheet", "Faint sheet the panels sit on",
                                c -> c.skijaGlass, (c, v) -> c.skijaGlass = v),
                        slSk("Background dim", "How much the world darkens behind the menu", 0, 0.9, 0.02,
                                c -> c.skijaDim, (c, v) -> c.skijaDim = v, PCT),
                        selSk("Corners", "How rounded every surface is",
                                List.of("Round", "Soft", "Sharp"),
                                c -> c.skijaCorners, (c, v) -> c.skijaCorners = v),
                        slSk("Render density", "Crispness above vanilla GUI scale", 1.0, 2.0, 0.05,
                                c -> c.skijaMenuScale, (c, v) -> c.skijaMenuScale = v,
                                v -> String.format("%.2fx", v)),
                        selSk("Render quality", "Lower it if the menu costs you frames",
                                List.of("Full", "Balanced", "Fast"),
                                c -> c.skijaRenderCap, (c, v) -> c.skijaRenderCap = v),
                        t("Tooltips", "Show the full description when hovering a setting",
                                c -> c.uiTooltips, (c, v) -> c.uiTooltips = v)
                )));

        return out;
    }

    static Category pinnedCategory(List<Category> cats) {
        List<String> pins = cfg().uiPinned;
        List<Module> out = new ArrayList<>();
        if (!pins.isEmpty()) {
            for (Category cat : cats) {
                if (cat.name.equals("Dashboard") || cat.name.equals("Pinned")) continue;
                for (Module m : cat.modules) {
                    List<Widget> all = new ArrayList<>();
                    m.body.collect(all);
                    Container hits = new Container();
                    for (Widget w : all) {
                        if (w.pinId != null && pins.contains(w.pinId)) hits.add(w);
                    }
                    if (!hits.isEmpty()) {
                        out.add(new Module("pin." + m.id, cat.name + " · " + m.name,
                                "Pinned from " + cat.name + ". Middle-click a row to unpin it.",
                                null, null, hits));
                    }
                }
            }
        }
        if (out.isEmpty()) {
            out.add(new Module("pin.empty", "Nothing pinned yet",
                    "Middle-click any setting to pin it here: your most-used switches, one tab away.",
                    null, null, body()));
        }
        return new Category("Pinned", out);
    }

    static List<Module> search(List<Category> cats, String query) {
        String q = query.toLowerCase(Locale.ROOT).trim();
        List<Module> out = new ArrayList<>();
        for (Category c : cats) {
            if (c.name.equals("Dashboard") || c.name.equals("Pinned")) continue;
            for (Module m : c.modules) if (m.matches(q)) out.add(m);
        }
        return out;
    }
}
