package net.cone.economy;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads the public Hypixel bazaar directly and computes every price the client shows. The endpoint
 * is keyless, so each client polls Hypixel for itself: no account and no shared backend. One
 * snapshot feeds the price lookup, both flip finders, the alerts and the HUD, refreshed at most
 * once per {@link #TTL_MS}.
 */
public final class HypixelBazaar {
    private static final String URL = "https://api.hypixel.net/v2/skyblock/bazaar";
    private static final long TTL_MS = 30_000;

    // Bazaar instant-sell tax. Hypixel charges ~1.25% by default; assume no perks for a conservative
    // net, so a flip that clears here clears in game.
    static final double TAX = 0.0125;

    // Guards. A flagged book must never be traded on: the quoted number is not the number that fills.
    private static final double HIGHBALL_FACTOR = 1.5;
    private static final long MIN_MOVING_WEEK = 5_000;
    private static final double MIN_TOP_OF_BOOK_COINS = 100_000;
    private static final double MAX_BID_DRIFT = 0.10;

    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8)).build();

    private static volatile Map<String, Book> snapshot = Map.of();
    private static volatile long fetchedAt;
    private static final Object LOCK = new Object();

    private HypixelBazaar() {}

    /**
     * One product's book, reduced to the numbers a client acts on. instaBuy is the lowest sell offer
     * (what you pay to buy now); instaSell is the highest buy order (what you get selling now).
     * avgBuy/avgSell are Hypixel's own weighted averages over each book, the baseline for the guards.
     */
    public record Book(String id, double instaBuy, double instaSell, double avgBuy, double avgSell,
                       double askDepth, double bidDepth, boolean bookOk,
                       long buyMovingWk, long sellMovingWk) {
        double netSell() {
            return instaSell * (1 - TAX);
        }
    }

    static Map<String, Book> books() throws Exception {
        refreshIfStale();
        return snapshot;
    }

    static void refreshIfStale() throws Exception {
        if (!snapshot.isEmpty() && System.currentTimeMillis() - fetchedAt < TTL_MS) return;
        synchronized (LOCK) {
            if (!snapshot.isEmpty() && System.currentTimeMillis() - fetchedAt < TTL_MS) return;
            snapshot = fetch();
            fetchedAt = System.currentTimeMillis();
        }
    }

    private static Map<String, Book> fetch() throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(URL))
                .timeout(Duration.ofSeconds(12))
                .header("User-Agent", "cone-lite")
                .GET().build();
        HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() == 429) throw new PriceClient.RateLimited();
        if (res.statusCode() != 200) throw new java.io.IOException("bazaar HTTP " + res.statusCode());
        JsonObject root = GSON.fromJson(res.body(), JsonObject.class);
        if (root == null || !root.has("products")) throw new java.io.IOException("bazaar payload empty");
        JsonObject products = root.getAsJsonObject("products");
        Map<String, Book> out = new HashMap<>(products.size() * 2);
        for (Map.Entry<String, JsonElement> e : products.entrySet()) {
            if (!e.getValue().isJsonObject()) continue;
            Book b = toBook(e.getKey(), e.getValue().getAsJsonObject());
            if (b != null) out.put(b.id(), b);
        }
        return out;
    }

    private static Book toBook(String id, JsonObject p) {
        JsonObject q = p.getAsJsonObject("quick_status");
        if (q == null) return null;
        double wavgBuy = num(q, "buyPrice");
        double wavgSell = num(q, "sellPrice");
        long buyWk = lng(q, "buyMovingWeek");
        long sellWk = lng(q, "sellMovingWeek");

        // buy_summary holds the SELL offers you buy from (lowest is best). sell_summary holds the BUY
        // orders you sell into (highest is best). Take the extreme explicitly, do not trust the sort.
        double ask = 0, askDepth = 0;
        for (JsonElement el : arr(p, "buy_summary")) {
            JsonObject l = el.getAsJsonObject();
            double price = num(l, "pricePerUnit");
            if (price <= 0) continue;
            if (ask == 0 || price < ask) { ask = price; askDepth = num(l, "amount"); }
        }
        double bid = 0, bidDepth = 0;
        for (JsonElement el : arr(p, "sell_summary")) {
            JsonObject l = el.getAsJsonObject();
            double price = num(l, "pricePerUnit");
            if (price <= 0) continue;
            if (price > bid) { bid = price; bidDepth = num(l, "amount"); }
        }
        boolean bookOk = ask > 0 && bid > 0;
        double instaBuy = ask > 0 ? ask : wavgBuy;
        double instaSell = bid > 0 ? bid : wavgSell;
        return new Book(id, instaBuy, instaSell, wavgBuy, wavgSell, askDepth, bidDepth, bookOk, buyWk, sellWk);
    }

    /**
     * Guard reasons for a book. An empty list means the quote is clean. This mirrors the price
     * layer; the poll-history "unseasoned" check is the only one a single snapshot cannot do, so it
     * is left out.
     */
    static List<String> guard(Book b) {
        List<String> r = new ArrayList<>();
        if (b.avgBuy() > 0 && b.instaBuy() > b.avgBuy() * HIGHBALL_FACTOR) {
            r.add("highball: instaBuy far above the book average");
        }
        if (b.sellMovingWk() < MIN_MOVING_WEEK) {
            r.add("illiquid: low 7-day movement");
        }
        if (!b.bookOk()) {
            r.add("no book: one side of the order book is empty");
        }
        if (b.instaSell() > 0 && b.instaBuy() > 0 && b.instaSell() >= b.instaBuy()) {
            r.add("crossed: best buy order is at or above the best sell offer");
        }
        double askCoins = b.askDepth() * b.instaBuy();
        double bidCoins = b.bidDepth() * b.instaSell();
        double thinner = Math.min(askCoins, bidCoins);
        if (thinner > 0 && thinner < MIN_TOP_OF_BOOK_COINS) {
            r.add("thin top: only " + Math.round(thinner) + " coins resting at the quoted price");
        }
        if (b.instaSell() > 0 && b.avgSell() > 0) {
            double drift = Math.abs(b.instaSell() - b.avgSell()) / b.instaSell();
            if (drift > MAX_BID_DRIFT) {
                r.add("dispersed: best buy order sits " + Math.round(drift * 100) + "% off the book average");
            }
        }
        return r;
    }

    private static double num(JsonObject o, String k) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsDouble() : 0;
    }

    private static long lng(JsonObject o, String k) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsLong() : 0;
    }

    private static JsonArray arr(JsonObject o, String k) {
        return o.has(k) && o.get(k).isJsonArray() ? o.getAsJsonArray(k) : new JsonArray();
    }
}
