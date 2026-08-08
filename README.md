# Cone Lite

A Skyblock market-data client for Minecraft. It reads the bazaar and shows you what the
numbers are worth: live prices, ranked spreads, enchanted-book chains, and price alerts.

Fabric, Minecraft 26.1.2, Java 25.

## What it does

| Command | Short form | What it shows |
|---|---|---|
| `/cone price <item>` | `/price` | Instant buy and sell price for one item |
| `/cone flips` | `/flips` | Bazaar spreads ranked by coins per hour |
| `/cone books` | `/enchantflips` | Enchanted-book chains ranked by coins per hour |
| `/cone alert <item> <expr>` | `/alert` | Say something in chat when a price crosses a line |
| `/cone track <item>` | `/track` | Pin an item to the bazaar overlay |
| `/cone menu` | Right-Shift | Open the client menu |
| `/cone help` | | Every command, grouped, with examples |

Alerts take expressions such as `sell>1.2m`, `buy<900k`, `spread>6%`, `spike>10%` and
`dip>10%`. Add `once` to fire a rule a single time. A rule fires on the crossing, not on
every check the price stays over the line.

There is also an on-screen overlay with the items you pinned, a configurable HUD, and a
menu with themes, profiles and rebindable keys.

## What it does not do

Cone Lite reads. It never clicks, never moves the player, and never places an order. There
is no automation of any kind in this repository.

## Prices

The client reads the public, keyless Hypixel bazaar API directly and computes every number
itself: the top of each book (the price that actually fills, not the weighted average), and
guards that flag a quote that looks wrong (an empty or crossed book, a thin top, a price far
from the book's own average, low weekly movement). Every number the client shows has been
through those guards.

It keeps one snapshot, shared by the price lookup, the finders, the alerts and the overlay,
refreshed at most once every 30 seconds. Reading the bazaar needs no account and no API key.

## Build

```
./gradlew build
```

The jar lands in `build/libs/`. Java 25 and a Minecraft 26.1.2 Fabric setup are required.

## Licence

PolyForm Noncommercial 1.0.0. See `LICENSE`. You may read, modify and share this for any
noncommercial purpose. You may not sell it or use it in a commercial product.
