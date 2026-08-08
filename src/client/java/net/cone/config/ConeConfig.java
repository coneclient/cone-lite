package net.cone.config;

public final class ConeConfig {
    public boolean hudEnabled = true;

    public int hudX = 4;
    public int hudY = 4;

    public float hudScale = 1.0f;

    public boolean hudBackground = true;

    public boolean hudShowPos = true;

    public boolean hudExpanded = true;

    public boolean hudProPrompt = true;

    public int configVersion = 1;

    public String uiTheme = "Abyss";

    public double uiAccentHue = -1;

    public String uiMotion = "Full";

    public String uiDensity = "Cozy";

    public boolean uiHighContrast = false;

    public double uiScrim = 0.78;

    public boolean uiTooltips = true;

    public java.util.List<String> uiPinned = new java.util.ArrayList<>();

    public double skijaAccentHue = -1;

    public double skijaAccentSat = 0.88;

    public boolean skijaHeroTint = true;

    public boolean skijaFrost = true;

    public boolean skijaGlass = true;

    public double skijaDim = 0.36;

    public String skijaCorners = "Round";

    public double skijaMenuScale = 1.5;

    public String skijaRenderCap = "Balanced";

    public boolean nickEnabled = false;

    public String nickName = "";

    public String aimMode = "Smooth";

    public boolean autoReconnect = true;

    public int reconnectDelaySec = 10;

    public int reconnectMaxTries = 0;

    public boolean resumeRailsAfterReconnect = true;

    public boolean autoJoinSkyblock = true;

    public double skyblockJoinDelaySec = 4;

    public boolean dynamicRest = false;

    public double restSessionMinHours = 0.5;
    public double restSessionMaxHours = 1.0;

    public double restBreakMinMinutes = 20;
    public double restBreakMaxMinutes = 30;

    public boolean breaksEnabled = true;
    public double breakMinMinutes = 20;
    public double breakMaxMinutes = 45;
    public double breakMinSeconds = 3;
    public double breakMaxSeconds = 12;

    public boolean fsWorldChange = true;
    public boolean fsTeleport = true;
    public boolean fsHealth = true;

    public boolean fsScreenOpen = true;

    public boolean fsBps = true;

    public boolean fsDirt = false;

    public boolean panicActEnabled = true;

    public boolean panicLookAround = true;

    public boolean panicHitBlock = true;

    public int panicHitCount = 3;

    public boolean panicChat = true;

    public String panicOrder = "freeze,look,hit,chat";

    public int panicTurnTicks = 5;

    public String panicChatMessages = "hi?,?,hello?,what happened,huh";

    public boolean failsafeSound = true;

    public String failsafeSoundId = "entity.warden.roar";

    public boolean failsafeBeep = false;

    public double teleportThreshold = 4.0;

    public double healthPercent = 0.5;

    public double bpsMax = 20.0;

    public boolean shortCommands = true;

    public boolean helpHintShown = false;

    public boolean ircEnabled = true;

    public boolean rpcEnabled = false;

    public String rpcAppId = "";

    public boolean bazaarHudEnabled = false;
    public int bazaarHudX = 4;
    public int bazaarHudY = 120;

    public java.util.List<String> bazaarHudItems = new java.util.ArrayList<>();

    public boolean priceAlertsEnabled = true;

    public java.util.List<net.cone.economy.AlertRule> priceAlertRules = new java.util.ArrayList<>();

    public int priceAlertPollSec = 60;

    public int priceAlertCooldownMin = 10;

    public boolean priceAlertSound = true;

    public String priceAlertSoundId = "minecraft:block.note_block.pling";

    public double flipMinPct = 3.0;
    public long flipMinVol = 30_000;

    public double flipMinPrice = 10.0;
    public double flipMinMargin = 10.0;
    public int flipLimit = 8;

    public double enchantMinRoi = 5.0;
    public double enchantMinNet = 10_000;
    public long enchantMinVol = 5_000;
    public int enchantLimit = 8;

    public int flipAmount = 0;

    public double flipBudget = 100_000;

    public int flipRelistMin = 5;

    public int flipTotal = 0;

    public boolean flipRebuy = true;

    public boolean flipReflip = false;

    public double flipRelistJitterPct = 0.28;

    public int flipSessionMinFlips = 12;
    public int flipSessionMaxFlips = 25;

    public double flipSessionRestMin = 8;
    public double flipSessionRestMax = 22;

    public int flipDailyCap = 180;

    public boolean flipStaffStop = true;

    public boolean flipKeepAwake = true;

    public double flipMaxPurseFraction = 0.85;

    public boolean remoteEnabled = false;

    public java.util.List<String> flipBlocked =
            new java.util.ArrayList<>(java.util.List.of("ESSENCE_"));

    public boolean headlessMode = false;

    public int headlessFps = 10;

    public boolean headlessMute = true;

    public String headlessWindow = "Minimize";

    public boolean headlessAutoJoin = true;

    public String headlessServer = "play.hypixel.net";

    public boolean discordEnabled = false;

    public String discordWebhookUrl = "";

    public boolean discordOnFailsafe = true;

    public int discordReportMinutes = 60;
}
