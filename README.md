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
| Dynamic FOV toggle | A key (Controls, section AxolotlClient, unbound until you set it) that switches AxolotlClient's own Dynamic FOV option on and off, with a note above the hotbar. Off keeps the FOV still when you sprint, draw a bow or have Speed. The same switch is in the Extras menu under Look. |
| Centered Inventory | Keeps the inventory in the middle of the screen while a potion effect is active. |
| Transparent UI | Skips the dark gradient screens draw over the world. Screens without a world keep their background. |
| Quickplay Menu | Press Y (rebindable) to open a card per game, with the game icon and a button per mode, that send the `/play` command. Only on hypixel.net unless "Hypixel Only" is switched off. The mode names match Hypixel's public API game list, see `QuickplayEntries.java`. |
| Hacks (own menu, all off by default) | Drawing-only helpers for Bed Wars: ESP, nametags, generator and bed info, alerts, projectile paths and a radar. See [Hacks](#hacks) below. |

## Hacks

Open the Hacks menu from its own category in AxolotlClient's Right Shift menu. Every hack is off by default and has its own switch; the fine-tuning switches sit under it in the same section. They only draw or print a local chat line: nothing is sent to the server. NPCs (Hypixel's shopkeepers and upgrade villagers, found by their UUID, the tab list and "[NPC]" in the name) are left out, and your team is read from the Bed Wars sidebar ("YOU").

Click a category to open it.

<details>
<summary><b>ESP</b> (2)</summary>

| Hack | What it does |
|---|---|
| Player ESP | A box around every other player, through walls, in their team colour, invisible players included, NPCs and anything not in the tab list skipped. |
| Tracers | A line from the middle of the screen to each of them. |

**Fine tuning:** ESP Style (outline, filled, both), ESP Color (team, distance, health), ESP Range, ESP: Show Invisible, ESP: Show Teammates (these also cover tracers and nametags; the radar has its own two).

</details>

<details>
<summary><b>Nametags and labels</b> (2)</summary>

| Hack | What it does |
|---|---|
| Nametags Through Walls | Name in team colour, health (from the server's health score, never made up) and distance. |
| Enemy Armor | Over each enemy's head, their best armor piece (leggings show the tier) with its Protection level, and their held item with Sharpness or Knockback. |

**Fine tuning:** Nametag Health, Nametag Distance, Show Held Item. **Label Sizes**: a slider from 25% to 200% (steps of 5%) each for generator counts, bed defense, waypoints, nametags, enemy armor, and fireball and pearl labels.

</details>

<details>
<summary><b>Bed Wars info</b> (2)</summary>

| Hack | What it does |
|---|---|
| Generator Counts | A label over every pile of iron, gold, diamonds and emeralds on the ground with icons and counts, through walls (only items the server sends, about 64 blocks). |
| Bed Defense | A label over every bed with the team letter and the icons and counts of the blocks around it (a dome up to 5 blocks out, bed height and above), innermost first, "Open" when a face of the bed touches air. |

**Fine tuning:** Count Iron, Count Gold, Count Diamonds and Count Emeralds (one switch each, all on), Show Own Bed.

</details>

<details>
<summary><b>Alerts</b> (4)</summary>

| Hack | What it does |
|---|---|
| Obsidian Alert | A short local chat line and a ping when an enemy bed gets obsidian, like `Obby Red bed 6/8 gaps 64m NE` (8/8 is a full inner layer). The team comes from the most common team-coloured blocks around the bed, or else the players next to it, and stays fixed once known. With Obsidian Waypoints (on with the alert), a beam in the team colour and a label over each of those beds until the obsidian or the bed is gone. |
| Invisibility Alert | A short local chat line and a ping when an enemy goes invisible (`Invis Red Alex went invisible 23m NE`), and for 60 seconds a see-through box in the team colour where it happened, with a line the way they were looking (it fades over the last 5 seconds). With Invisibility Waypoints (on with the alert) the mark also gets a beam and a label with the name, seconds since and distance. With Only After a Potion (on by default) it only counts enemies seen holding an invisibility potion in the 3 seconds before. It also counts an enemy who disappears from your side within 32 blocks, alive, right after holding the potion. Someone who comes into range already invisible gives no alert, players who walk out of range without a potion in hand never count, and spectators and dying players are skipped. |
| Hidden Player Trail | A breadcrumb trail of players you cannot see, from what the server still sends for them: footstep, hurt, drink and bow sounds, footstep particles, and blocks being broken by an entity you do not have. A sound only counts when no visible player or mob is at that spot. Each crumb sits in the middle of the lower block a player takes up; crumbs of one walk are joined by a line, a block being broken gets an outline, a hit is a red cube. Trail Time (5, 10, 20 or 30 seconds), Trail Style (markers through walls, or coloured dust particles in the world), Trail Labels ("Hidden 3s 14m NE", or the name when one Invisibility Alert mark tells who it is) and a chat line when a new trail starts. Sneaking players make no footstep sounds, so they leave no trail. |
| Rush Alert | With Rush Distance 15, 25 or 40 blocks: a chat line and a sound when an enemy comes near your bed. Your bed is the one of your team colour, or the one nearest where you stood when the game started. |

**Fine tuning:** a sound switch for each alert (Obsidian, Invisibility, Rush), Obsidian Waypoints, Invisibility Waypoints, Only After a Potion, and the trail options above.

</details>

<details>
<summary><b>Projectiles</b> (3)</summary>

| Hack | What it does |
|---|---|
| Fireball Warning | The flight and impact block of every fireball, the soft blocks (wool, wood) the blast will likely break (an estimate), and a sound when one is coming at you. |
| Pearl Landing | The flight and landing block of every flying pearl. |
| Trajectories | While holding a pearl, snowball or egg, or drawing a bow, the line it would fly and the block (or player, in green) it would hit. |

**Fine tuning:** Fireball Sound, Mark Blast Blocks, Bow Trajectory, Throw Trajectory.

</details>

<details>
<summary><b>Radar</b> (1)</summary>

| Hack | What it does |
|---|---|
| Radar | A minimap on the HUD with every other player shown by the face of their skin (face and hat layer, framed in the team colour), NPCs skipped. Radar Centre puts you or your own bed in the middle (a key switches it); the bed radar turns gray with the reason ("Bed out of range (143m)", "No bed") while the bed is beyond your render distance or not known, since players there are not sent to you. Move it by dragging its "Radar" box in AxolotlClient's HUD editor, like any HUD module. |

**Fine tuning:** Range (16 to 128 blocks), Shape (circle or square), Size, Face Size, Background Opacity, Turn With View (or north up), Show Own Face, Radar: Show Teammates, Radar: Show Invisible, Keep Far Players on Edge, Team Colour Frame, Show Names, Height Arrows, Grid and North.

</details>

<details>
<summary><b>Vision</b> (1)</summary>

| Hack | What it does |
|---|---|
| No Blindness | No blindness fog (drawing only, the effect itself stays). |

</details>

<details>
<summary><b>Hack keys</b> (2)</summary>

| Hack | What it does |
|---|---|
| Emergency toggle | G switches every hack off and remembers which were on, H switches exactly those back on. The memory is kept in `config/axolotl-extras/hacks-off.txt`, so it survives a restart. |
| Toggle keys | Each of the 15 main hack switches has its own toggle key, unbound until you set it. All hack keys are in Controls, section AxolotlClient Extras: Hacks. A short note above the hotbar says what a key did (switch Hack Key Note off for none). |

</details>

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

Needs JDK 17 or newer to run Gradle (the mod itself targets Java 8). The build compiles against AxolotlClient's jars in `libs/`,
which are not in this repository (they are AxolotlClient's, not ours to redistribute). Put these four there first, all taken
from the AxolotlClient 3.0.6+1.8.9 release jar on Modrinth:

- `AxolotlClient-3.0.6+1.8.9.jar`: the release jar itself.
- `AxolotlClient-3.0.6.jar` and `AxolotlClient-config-2.1.13+1.8.9.jar`: inside it, under `META-INF/jars/`.
- `AxolotlClientConfig-common-2.1.13.jar`: inside `AxolotlClient-config-2.1.13+1.8.9.jar`, under `META-INF/jars/`.

GitHub Actions builds every push the same way (`.github/workflows/build.yml`): it downloads the release jar from Modrinth,
checks its SHA-256, unpacks the nested jars into `libs/`, runs `gradlew build` (with the unit tests) and keeps the built jar
as an artifact of the run.

## How it plugs in

The mod declares an `axolotlclient.module` entrypoint (`ExtrasModule`). AxolotlClient calls its `init()` before the config manager
loads `AxolotlClient.json`, so these options are saved and loaded together with the rest of AxolotlClient's settings.

## Credits and license

AxolotlClient is LGPL-3.0-or-later. This add-on is released under the same license (the license text is `LICENSE.AxolotlClient`).
The Bed Wars resource counter logic follows the MCRust 1.8.9 client.
