# AxolotlClient Extras

An add-on for [AxolotlClient](https://modrinth.com/mod/axolotlclient) 3.0.6+1.8.9 on Legacy Fabric (Minecraft 1.8.9).
It adds a few features that AxolotlClient does not have. Each one has its own switch under **Extras** in the AxolotlClient
options menu (off by default, except the two noted below), and the Resource Counter is a normal AxolotlClient HUD module with its own entry in the
HUD editor.

| Feature | What it does |
|---|---|
| Bed Wars Resources (HUD) | Iron, gold, diamonds and emeralds as `inventory + ender chest = total`. Opening the ender chest always replaces the ender chest count with what is really in it. Punching the ender chest with resources in hand adds what leaves your inventory within half a second (inferred, since the client never sees the deposit). Everything is forgotten when you leave the world, leave Bed Wars, or a game starts. |
| Extras Menu (F8) | A settings screen for all of the above in one scrolling list: switches with their descriptions, a search box, sections. Opens with F8 (rebindable) or from the Extras section of the AxolotlClient options. Changes apply at once and are saved when it closes. |
| HUD Corners (default medium) | Rounds the corners of every AxolotlClient HUD box (also the Resource Counter and Upgrade Display), or keeps them square. |
| Modern Menus (default on) | Restyles the menus with flat rounded widgets: buttons (vanilla menus, AxolotlClient's options and the HUD editor), AxolotlClient's category buttons and ON/OFF switches, the search box and colour fields, scrollbars, and the sliders (vanilla and AxolotlClient's), plus a dark background instead of dirt and rounded outlined boxes in the HUD editor. Turn it off to get the normal look. |
| Raw mouse guard (automatic) | When the Raw Input mod is installed, AxolotlClient's own "Raw Mouse Input" option is switched off and kept off. That option is not raw input: it rescales the movement and cuts it to whole numbers, which makes slow movements lose distance (it feels like acceleration). |
| Normal Hypixel Messages (default on) | Shows Hypixel's own text for deaths, broken beds, eliminated teams and disconnects instead of AxolotlClient's short rewritten style (`P1 name /rekt/ W2 name`). The stats AxolotlClient added stay next to them, when real stats are loaded (Hypixel API key needed): a teammate's final kill count after their name on a final kill, their beds broken count after a bed they broke, and the per-player stat lines under an eliminated team. |
| Privacy Lock (default on) | Stops AxolotlClient from sending your player UUID (and the UUIDs of players around you) to its own server, and from downloading Discord's game SDK. See `PRIVACY-AUDIT.md`. |
| Chat Stats At Start (default on) | When a Bed Wars game starts, prints one chat line per team with every player's chosen stat (KDR, FKDR, WLR, BBLR or stars, set with Chat Stat). Needs AxolotlClient's Bed Wars module and a Hypixel API key. Local chat only, nothing is sent. |
| Ignore Server Blocks (default on) | Stops AxolotlClient from switching Freelook, Toggle Sneak, Time Changer, Full Bright and Low Fire off by server address, and re-enables anything a server switches off through its `block_mods` message. Servers may still have their own rules. |
| Toggle Sprint Without HUD (default on) | Keeps the Toggle Sprint key working when the Toggle Sprint HUD module is switched off. |
| Upgrade Display (HUD) | Bed Wars upgrades as text, like Lunar's: a bold "Upgrades" heading, one line per bought upgrade with the diamond price of the next tier on the right, then a "Traps" heading. Built from the "Name purchased Thing" chat lines. Next-tier prices come from prices you supplied (Reinforced Armor, Maniac Miner, the forges and Cushioned Boots, for both team sizes). The team size (solo/doubles or 3v3v3v3/4v4v4v4) is read from the waiting lobby's "Mode:" line and then checked against the number of teams in the game (4v4 has two teams and uses the 3s/4s prices), and no prices show until it can tell. It can also be set by hand. Separate from AxolotlClient's icon overlay, which can be switched off in the HUD editor. |
| KDR Level Head and KDR In Tab (default on) | In Bed Wars, the line above a player's head shows their kill/death ratio instead of FKDR (or whatever stat is chosen in the Bed Wars options), and the tab list shows the ratio to the left of the health number. Uses AxolotlClient's Bed Wars module, which must be enabled, and a Hypixel API key in its settings. AxolotlClient invents random stats when it cannot load real ones, so nothing is shown for those players. Kills and deaths are totals, not final kills. |
| 1.7 Visuals (default off, four separate switches) | Block Hitting: the sword, food and bow animations keep swinging when you start blocking just after a hit (vanilla hides the swing). 1.7 Item Positions: held items, blocks, the bow pose and the fishing rod sit in your hand the 1.7 way (larger, turned). 1.7 Sneaking: the camera drops at once when you sneak and rises slowly when you stop. 1.7 Sword Block (outside view): how others and the third person camera see a blocking sword (arm straight in front, sword across the chest). Ported from the OldAnimations mod (AxolotlClient, LGPL-3.0). Visual only, nothing is sent to the server. |
| Reliable Bed Wars Stats (default on) | Loads every player's Bed Wars stats from Hypixel's public API itself, a few players at a time, when a game starts, asks again after failures and slows down when Hypixel says so. AxolotlClient asks once, waits a microsecond and then keeps made-up numbers for good, which left most players at a question mark. Unknown stats (nicked players, no valid key) show "?"; made-up numbers are never shown anywhere (above heads, tab list, messages, the end of game winstreak list, the lobby level head). Needs a Hypixel API key in the AxolotlClient settings. |
| No Fog | Turns fog off after vanilla sets it up, on land and under water. |
| Centered Inventory | Keeps the inventory in the middle of the screen while a potion effect is active. |
| Transparent UI | Skips the dark gradient screens draw over the world. Screens without a world keep their background. |
| Quickplay Menu | Press Y (rebindable) to open a card per game, with the game icon and a button per mode, that send the `/play` command. Only on hypixel.net unless "Hypixel Only" is switched off. The mode names match Hypixel's public API game list, see `QuickplayEntries.java`. |
| Hacks (own category in AxolotlClient's Right Shift menu, all off by default) | **Player ESP**: a box around every other player, through walls, in their team colour, invisible players included, NPCs and anything not in the tab list skipped. **Tracers**: a line from the middle of the screen to each of them. **Enemy Armor**: over each enemy's head, their best armor piece (leggings show the tier) with its Protection level, and their held item with Sharpness or Knockback. **Generator Counts**: a label over every pile of iron, gold, diamonds and emeralds on the ground with icons and counts, through walls (only items the server sends, about 64 blocks). **Bed Defense**: a label over every bed with the team letter and the icons and counts of the blocks around it (a dome up to 5 blocks out, bed height and above), innermost first, "Open" when a face of the bed touches air. **Obsidian Alert**: a short local chat line and a ping when an enemy bed gets obsidian, like `Obby Red bed 6/8 gaps 64m NE` (8/8 is a full inner layer). The team comes from the most common team-coloured blocks around the bed, or else the players next to it, and stays fixed once known. **Obsidian Waypoints** (on with the alert): a beam in the team colour and a label over each of those beds until the obsidian or the bed is gone. **No Blindness**: no blindness fog (drawing only, the effect itself stays). The "Open" button opens the same list in the Extras menu style. Nothing is sent. |

## Frame recorder (developer tool)

In a singleplayer world, a chat line `AXBENCH label seconds` records frame times for that long and then logs average fps, 1% and
0.1% lows, p99 and worst frame, plus garbage collections, and appends them to `config/axolotl-extras-bench/results.txt` (every
frame time goes to `<label>.csv`). It does nothing on servers and nothing else is sent anywhere. See `PROGRESS.md` for what it
showed about optimization mods.

For hands-off runs, if `config/axolotl-extras-bench/run.txt` exists it is read and deleted, and its lines are carried out one
by one: `world <folder>` (open a singleplayer world from the title screen), `cmd /kill ...` (only `/kill`, `/gamerule`, `/time`
and `/tp` are accepted), `bench <label> <seconds> [particles]` (with a particle count it also plays a movement scene: the player
sprints and jumps in circles, particles are spawned around the player, and 64 client side entities appear around the start:
32 villagers with name tags, 16 named armor stands, 16 item drops), `done` and `quit`. Singleplayer only.

## Requirements

- Minecraft 1.8.9, Fabric Loader 0.14.7 or newer, Legacy Fabric API 1.7.0 or newer
- AxolotlClient 3.0.6+1.8.9 (the last Legacy Fabric build)

## Building

```
gradlew build
```

Needs JDK 17 or newer to run Gradle (the mod itself targets Java 8). `libs/` holds jars taken from the AxolotlClient 3.0.6+1.8.9
release, used only to compile against.

## How it plugs in

The mod declares an `axolotlclient.module` entrypoint (`ExtrasModule`). AxolotlClient calls its `init()` before the config manager
loads `AxolotlClient.json`, so these options are saved and loaded together with the rest of AxolotlClient's settings.

## Credits and license

AxolotlClient is LGPL-3.0-or-later. This add-on is released under the same license (the license text is `LICENSE.AxolotlClient`).
The Bed Wars resource counter logic follows the MCRust 1.8.9 client.
