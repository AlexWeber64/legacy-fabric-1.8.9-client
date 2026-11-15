# Progress

Goal: a Lunar Client style setup for Minecraft 1.8.9 on Legacy Fabric (Prism instance `1.8.9-lunarlike`), where every
feature can be switched on and off at will. Features come from AxolotlClient where it has them, and from the
`axolotl-extras` add-on in this folder where it does not.

Last updated: 2026-11-14.

## Lunar module map

| Lunar module | Provided by | State |
|---|---|---|
| Armor Status | AxolotlClient | Done, enabled |
| FPS | AxolotlClient | Done, enabled |
| Potion Effects | AxolotlClient | Done, enabled |
| Zoom | AxolotlClient | Done, enabled |
| Time Changer | AxolotlClient | Done, enabled |
| Upgrade Display | Extras (text list like Lunar's), AxolotlClient has an icon version | Written 2026-11-05, unit tested, not seen in game or on Hypixel. Prices filled in from your lists, team size detected automatically (lobby line checked against a screenshot, in-game team lines from memory) |
| Toggle Sprint | AxolotlClient (Toggle Sprint HUD) | Works in singleplayer, see "In progress" for the Hypixel side |
| Freelook | AxolotlClient | Works in singleplayer, Hypixel check removed (untested there) |
| Raw Mouse Input | RawInput mod | Done |
| No Fog | Extras | Done, tested in game |
| Keep Inventory Centered | Extras | Done, tested in game |
| Transparent UI | Extras | Done, tested in game (my reading of the setting: no dark layer over the world) |
| Resource Counter (Bed Wars) | Extras | Done, tested in singleplayer with a simulated Bed Wars sidebar |
| Hypixel Quickplay | Extras | Done, every mode name found in Hypixel's public API game list, Doubles confirmed by you |
| 1.7 Visuals | Extras (ported from OldAnimations) | Four switches, off by default, checked in game with before/after screenshots 2026-11-06. Not ported: third person item positions, the third person sneak model, old damage tint, glint and the 100 other OldAnimations options |

## Completed

- Baseline instance `1.8.9` with Legacy Fabric and Legacy Fabric API, plus `BENCHMARK.md` (you measured 165 fps stable).
- Clone instance `1.8.9-lunarlike` with AxolotlClient 3.0.6, RawInput, Fabric Language Kotlin, Legacy Fabric API and the add-on.
- Add-on project builds with Gradle, 15 unit tests pass.
- Extras menu inside the AxolotlClient options, every feature has its own switch.
- Resource Counter:
  - inventory plus ender chest equals total, per resource
  - resets when you leave the world, leave Bed Wars, or a game starts
  - opening the ender chest replaces the ender chest count with the real contents
  - punching the ender chest with resources in hand adds the amount that left your inventory within half a second (inferred)
- Quickplay menu (key Y), Doubles uses `/play bedwars_eight_two`.
- Toggle Sprint: found that it was dead because its HUD module was switched off (the toggle lives in that module's tick).
  Re-enabled it in your clone config and measured 11.22 blocks per 2 s toggled against 8.63 walking.
- Written this round (built and installed in the clone, not yet run in game):
  - **Ignore Server Blocks** (default on): AxolotlClient no longer switches Freelook, Toggle Sneak, Time Changer, Full Bright
    and Low Fire off by server address, and anything a server switches off through its `block_mods` message is switched
    back on within a tick.
  - **Toggle Sprint Without HUD** (default on): the toggle key keeps working when the Toggle Sprint HUD module is off.

- Quickplay mode names checked against `api.hypixel.net/v2/resources/games` (all Bed Wars and SkyWars names used are listed there).
- Toggle Sprint key rebound to your sprint key (backtick, key 41) in the clone's `options.txt`, so it behaves like Lunar's
  toggle. Backup: `options.txt.before-sprintkey`. Sneak toggle stays on I, Freelook on V.

- In game tests on 2026-11-05 (instance `1.8.9-lunarlike`, singleplayer, Toggle Sprint HUD module switched off):
  - Toggle Sprint Without HUD: walking 2 s covered 8.63 blocks, with the toggle on 11.50, after toggling off again 8.84.
    The toggle is on the sprint key (backtick).
  - Ignore Server Blocks: after a direct connect to `hypixel.invalid` (a Hypixel-named address that cannot resolve, so no
    real server was contacted), Freelook still worked with the option on (camera orbited, player facing stayed 0.0).
    Control run with the option off: Freelook was blocked (player facing changed to 41.6, camera stayed first person).
  - Log had no mixin or add-on errors, only the expected unknown host and AxolotlClient login errors.

- Upgrade Display team size detection, two steps: the waiting lobby "Mode: Doubles" line (checked against your screenshot), then the
  in-game team count (8 team lines mean solo/doubles, 4 or 2 mean 3s/4s/4v4), which wins once it can be read. The in-game team
  line format is from memory and not yet seen.
- Upgrade Display module (text layout from your Lunar screenshot). unit tests pass. Built and installed in the
  clone, never rendered in game.

- KDR Level Head and KDR In Tab (2026-11-05): written, built, installed in the clone, never run in game. Lives in its own mixin
  config (`axolotlextras.bedwars.mixins.json`, not required) so a failure there only logs. Needs a Hypixel API key, and your
  config has `hypixel_api_key` empty.

- Privacy and safety audit (2026-11-05), see `PRIVACY-AUDIT.md`: all four third-party jars match Modrinth releases, class scans
  found no remote access, stealer or command execution machinery, and the only AxolotlClient server contact (badges, sending
  your UUID) is blocked by the new Privacy Lock (default on). The lock is built and installed but not yet run in game.
- Chat Stats At Start (2026-11-05): when a Bed Wars game starts, one chat line per team with every player's KDR (or FKDR,
  WLR, BBLR, stars, set with Chat Stat). Written, unit tested for the formatting, never run in game.

- Death messages (2026-11-05): found in your Hypixel log that AxolotlClient's Bed Wars module rewrites death, bed and
  elimination messages, and appends gold numbers (like 9185, 554) that are your final kill and bed counts taken from made-up
  stats, because the Hypixel API key is empty. The numbers and the per-player lines under "TEAM ELIMINATED" are now only shown
  when the stats are real. Then, at your request, Hypixel's own text is shown again for deaths, broken beds, eliminated teams and disconnects (option Normal Hypixel Messages, default on), keeping the stats next to them: a teammate's final kill count or beds broken after their name, and the stat lines under an eliminated team. Built and the hook applied at startup in a minimized launch, not yet seen in a real game.
- Minimized test launches: `scratchpad\scripts\Launch-Minimized.ps1` starts an instance with its windows minimized and not
  activated, and only touches processes of that instance (never Lunar). First minimized launch worked: startup log showed the
  Privacy Lock patches loaded and no mixin errors. Limits: no input is possible while minimized, a minimized game ignores the
  close request (it has to be stopped), and it logs a GL error every frame.

- Nicer UI (2026-11-06), first round, tested fullscreen at 2880x1800 (GUI scale 5): rounded corners on all HUD boxes (HUD Corners
  option) and a new Extras Menu (F8) with sections, switches, search, scrolling and descriptions. Closing it crashed the game
  once (wrong mod id when saving the config); fixed, and the close and the config save were checked afterwards.
- Test launches are now fullscreen: `fullscreen:true` is set in the clone's `options.txt` (backup `options.txt.before-fullscreen`),
  and the test scripts convert menu clicks from GUI units, so they work at any resolution.

- UI round 2 (2026-11-06): Quickplay redesigned (a card per game with its icon: bed for Bed Wars, ender eye for SkyWars), a
  Modern Menus skin for every button, AxolotlClient's category buttons and ON/OFF switches, the HUD editor boxes and a dark menu
  background. Checked in game, fullscreen at 2880x1800.
- UI round 3 (2026-11-06): text fields (the Search box and colour fields), scrollbars and sliders restyled too, in AxolotlClient's
  option pages and the vanilla menus. Checked in game. AxolotlClient's list draws its own scrollbar, so that one is switched off
  and replaced. Not restyled: the vanilla title screen buttons are covered by the button skin but its panorama and logo are not,
  and the colour picker popup of AxolotlClient was not opened.
- Raw mouse input (2026-11-06): cause found. AxolotlClient's own "Raw Mouse Input" option was ON; it rescales and truncates the
  movement (checked: slow steps and a fast move of the same distance gave 9.5 and 10.4 degrees with it on, 18.9 and 18.9 with
  it off). It is now switched off and kept off whenever the Raw Input mod is installed. A second issue is still open: the Raw Input
  mod adds up every mouse device it finds, and a test with injected movement counted it twice, so the sensitivity may be about
  double what it should be. A probe that shows what your real mouse reports is in the scratchpad (`ji\run-probe.cmd`).
- Confirmed by the user on 2026-11-06: Freelook and Toggle Sprint work.

- Hacks menu (2026-11-07): a new "Hacks" category in AxolotlClient's own menu (Right Shift, next to Extras; no new key)
  with an "Open" button for the same list in the Extras menu style (`ui/HacksScreen`). Five switches, all off by default:
  Player ESP, Tracers, Generator Counts, Bed Defense, Obsidian Alert. Code in `hacks/`: `ResourceClusters` (piles of
  dropped resources), `BedDefense` (blocks around a bed, inside out, "exposed" check, obsidian layer coverage),
  `HackMath` (team colour, compass, world to screen projection), all unit tested (`HacksTest`, 12 tests);
  `BedScanner` reads 12 chunks per tick from their block arrays (radius 10 chunks); `HackRenderer` draws the boxes and
  tracers at the end of the world pass (before the hand) and the labels on the HUD by projecting their positions with the
  saved camera matrices. Mixins `HackWorldRenderMixin` and `HackHudMixin` are in the optional visuals config; their
  targets were checked against the 1.8.9 intermediary names. No open-source Legacy Fabric mod with these features was
  found (the 1.8.9 ones are Forge and mostly GPL), so nothing was copied or downloaded. Built, never run in game.
  To check: the boxes line up with players (also in third person), labels sit over generators and beds, the item icons
  draw correctly on the HUD, and the obsidian alert fires once per change (a change has to hold for 2 seconds first).
- Hacks round 2 (2026-11-07, from your feedback): the obsidian alert is one short line (`Obby Red bed 6/8 gaps 64m NE`);
  the count is out of the spots in the obsidian's layer, which is 8 for the inner layer (the first round's example of 14
  was made up, a second layer has 18). Your own team's bed is not announced. Team detection (`hacks/TeamGuess`, unit
  tested): the most common team-coloured wool, clay, glass or carpet within 10 blocks of the bed, when it leads clearly;
  otherwise the team of the players within 8 blocks; kept once known. New switches: Obsidian Waypoints (beam and label
  over each reported bed, kept while its chunk is out of range), Enemy Armor (armor tier and Protection level, held item
  and Sharpness/Knockback, enemies only), No Blindness (`NoBlindnessMixin`, wraps the blindness check in the two fog
  methods only, so sprinting still sees the effect). Bed logic moved to `hacks/BedTracker`. 16 tests pass. Built, never
  run in game. Unchecked guesses: the Hypixel wool colours for Aqua, Pink and Gray teams, and whether Hypixel sends the
  enchantments of other players' armor (it should, since enchanted armor glints).
- Hacks round 3 (2026-11-07): Rush Alert (+ Rush Distance), Fireball Warning, Pearl Landing, Trajectories, Nametags
  Through Walls. `hacks/Trajectory` steps projectiles like 1.8.9 does (move, acceleration, drag, gravity; pearls,
  snowballs and eggs 1.5 speed, 0.03 gravity; arrows 3 x charge, 0.05 gravity; fireballs 0.95 drag plus their
  acceleration), unit tested (`TrajectoryTest`, 7 tests). `hacks/Projectiles` uses it for flying fireballs and pearls
  (once per tick) and your own aim (every frame, from the hand position the game uses). Fireball blast marks are an
  estimate: blocks within 2.2 of the impact with a blast resistance under 4, skipping glass and beds (Hypixel makes them
  blast-proof). `hacks/RushAlert`: your bed is the bed of your team colour nearest where you stood 2 seconds after the
  game start message, or the bed nearest that spot within 40 blocks; each enemy is announced once per visit with a 20
  second cooldown. Nametag health comes from the below-name or tab list score, else the entity health, else nothing.
  Drawing moved to `hacks/Draw3D` (one batch of see-through boxes, one of lines). 79 tests pass. Built, never run in
  game. To check: the aim line starts at the hand and lands where the pearl really lands, fireball impact matches, and
  the rush alert picks your own bed (it stays silent until it can tell).
- Deployed to the play clone 1.8.9-lunarlike on 2026-11-07 for testing (all hacks rounds). The previous jar is in
  mods-backup as axolotl-extras-0.1.0.before-hacks.jar. All hack switches start off.
- Hacks round 4 (2026-11-07), after the first in-game test:
  - Rush Alert fix. The user's report: at the start it went off for teammates, and on some maps it thought the team was
    Green. Cause: the own team came from the name colour, which in the waiting lobby is the rank colour (VIP is green), and
    the own bed was then cached for good. Now the own team comes from the Bed Wars sidebar line with "YOU" on it
    (`TeamGuess.fromSidebarLine`), with the name colour only as a fallback after the game start. The own bed is the bed
    nearest the start spot (any colour guess), takes the own team, and is looked up again when the team changes. Bed team
    guesses only count players once the game runs. No rush alerts for 10 seconds after the start spot is taken. Teammates
    are told by scoreboard team or team colour. New switch: Rush Sound.
  - Rush: Include Teammates (added 2026-11-15 as "Rush: Teammates Too", renamed the same day): a Rush Alert
    switch, off by default. On, the alert also counts teammates near your bed (same distance, cooldown and chat
    line); off, it stays enemies only, as after this fix. Played once with it off on 2026-11-15, together with the
    team prefix fix (pull request #10): a doubles game as Aqua, one Rush line for the whole game, for a Gray enemy
    who broke your bed 11 seconds later, and no line for anyone else, also not while players respawned. A teammate's
    line now has a green "[Teammate]" after the name (`HackMath.rushMessage`, 1 test).
  - NPCs left out of ESP, tracers, nametags, enemy armor, rush and the aim line: version 2 UUIDs (Hypixel's fake players),
    a hidden name tag (team rule "never"), or "[NPC]" in the name, on top of the tab list check.
  - Fine tuning switches: ESP Style, ESP Color, ESP Range, Show Invisible Players, Show Teammates, Nametag Health, Nametag
    Distance, Show Held Item, Diamonds and Emeralds, Iron and Gold, Show Own Bed, Obsidian Sound, Fireball Sound, Mark Blast
    Blocks, Bow Trajectory, Throw Trajectory, Label Size. The Hacks menu has new sections Nametags and Labels, and Alerts.
  - 83 tests pass (4 new: sidebar line, NPC UUIDs, option numbers, colour scale). Built, not yet run in game. To check:
    the sidebar really has "YOU" on the own team's line (if not, the team is only known from the name after the start),
    and that shopkeepers are gone from ESP.
  - Deployed to the play clone 1.8.9-lunarlike on 2026-11-07. The previous jar (hacks rounds 1 to 3) is in mods-backup as
    axolotl-extras-0.1.0.before-hacks-round4.jar.
- Hacks round 5 (2026-11-07), after the user's second test (labels missing, random freezes of several seconds):
  - All HUD labels were invisible (generator counts, bed defense, obsidian waypoint labels, nametags, enemy armor,
    projectile labels). Cause: `HackHudMixin` injected at HEAD of `InGameHud.render`, but 1.8.9 only switches to the
    2D HUD view (`GameRenderer.setupHudMatrixMode`) a few instructions later, so the labels were drawn with the 3D camera.
    Now injected right after that call. ESP boxes, tracers and beams were not affected (they are drawn in the world pass).
  - Generator Counts read the item stack straight from the data tracker: `ItemEntity.getItemStack()` wrote "Item entity
    has no item?!" to the log for items the server had not filled in yet (about 50 lines per game).
  - Freezes: nothing in the hack code should take seconds (bed scan about 1 ms per tick), so `hacks/FreezeWatch` now
    measures it. Any hack step over 50 ms writes "[Extras] Hacks: <step> took N ms" to latest.log, and a gap of over a
    second between ticks writes "[Extras] Freeze: ..." with the garbage collection time and the slowest hack step in that
    gap. The "Error executing task" NullPointerExceptions in the log are vanilla scoreboard packet errors from Hypixel;
    they were in the logs before the hacks too. Instance memory is 2 to 4 GB on Java 8 (Parallel GC).
  - 83 tests pass. Built, not yet run in game. To check: labels show at generators and beds, and what the freeze lines say.
  - Deployed to the play clone 1.8.9-lunarlike on 2026-11-07. The previous jar (round 4) is in mods-backup as
    axolotl-extras-0.1.0.before-hacks-round5.jar.
- Label Sizes (2026-11-07): the single Label Size option is replaced by one size per label type (Generator Counts, Bed
  Defense, Obsidian Waypoints, Nametags, Enemy Armor, Fireballs and Pearls), each 25%, 50%, 75%, 100%, 125%, 150% or 200%, in a
  new Label Sizes section of the Hacks menu. Stacked labels (waypoint text over the bed defense label, enemy armor over the
  nametag) are moved up by half of each label's scaled height, so they do not overlap at any mix of sizes. 83 tests pass.
  Deployed to the play clone 1.8.9-lunarlike on 2026-11-07; the previous jar (round 5) is in mods-backup as
  axolotl-extras-0.1.0.before-label-sizes.jar.
- Freeze log read (2026-11-07): 13 freezes in one session, mostly about 1.06 s (one 2 s). During each, all hacks together
  took 0 to 4 ms and garbage collection 0 ms, so neither the hacks nor memory is the cause; something else holds the game
  thread for about a second. `FreezeWatch` now also starts a watcher thread that writes the game thread's stack to
  latest.log at 600 ms and 900 ms into a freeze ("[Extras] Freeze going on ..."), to show what it is stuck in. Built, not
  yet deployed.
- Git history (2026-11-07): the folder is now a git repo, pushed to the private GitHub repo
  AlexWeber64/legacy-fabric-1.8.9-client. There was no version control before, so the history was rebuilt by replaying every
  recorded file change from the two Claude Code conversations in order (file writes and edits directly, the file-editing parts
  of shell commands in a scratch copy), with the real time of each change; the replay ends byte for byte at the files as they
  were. 30 commits, one per piece of work, then this one. `libs/` (AxolotlClient's jars) and CLAUDE.md are not in the repo.
  CI (GitHub Actions, `.github/workflows/build.yml`) builds and tests each commit after the first; the workflow was added to
  every one of them, and each was pushed on its own so each shows its build result. The first commit has no build file fix yet
  (`archivesBaseName`, fixed in the second), so it has no CI.
- Label size sliders (2026-11-08): the six Label Sizes options are sliders now (AxolotlClient `IntegerOption`, 25 to 200
  percent), drawn as a bar like FOV both in AxolotlClient's menu and in the Hacks menu (`ExtrasScreen` got a slider: click or
  drag on the bar, steps of 5). New option names (`axolotlextras.labelSize...`), because AxolotlClient cannot read the old
  text values into a number option; on the first tick after start the old button values in AxolotlClient.json are carried
  over to sliders that were never saved (`LabelSizeMigration`, 4 tests). 87 tests pass. Built, not yet seen in game.
  Deployed to the play clone 1.8.9-lunarlike on 2026-11-08 together with the freeze stack logger; the previous jar (label size
  buttons) is in mods-backup as axolotl-extras-0.1.0.before-label-sliders.jar.
- Emergency toggle (2026-11-10): two keys, rebindable in Controls under AxolotlClient: G ("Hacks: All Off") switches the 12
  main hack switches off and remembers which were on, H ("Hacks: Back On") switches exactly those back on. Pressing G again
  while off keeps the memory and adds any hack switched on by hand since. The memory is in
  `config/axolotl-extras/hacks-off.txt` (deleted on H), so it survives a restart or crash. The keys only work in game (no
  menu or chat open). The config is saved right away. A note above the hotbar ("Hacks off (3)", "Hacks back on (3)") unless
  Hack Key Note is off. `hacks/HackPanic`, 4 tests.
- Hack toggle keys (2026-11-10): one toggle key per main hack switch (the same 12 as the emergency toggle,
  `ExtrasModule.HACK_MAIN_SWITCHES`), unbound by default so they take no keys used in game. All hack keys, G and H included,
  are in their own Controls section "AxolotlClient Extras: Hacks". A key flips its hack, saves the config and shows
  "Player ESP on" / "off" above the hotbar (option Hack Key Note, which replaced Emergency Toggle Note before it was ever
  deployed). Fine-tuning switches (style, sounds, sizes) have no keys. 91 tests pass.
  Deployed to the play clone 1.8.9-lunarlike on 2026-11-10 together with the emergency toggle, not yet seen in game; the
  previous jar (label size sliders) is in mods-backup as axolotl-extras-0.1.0.before-hack-keys.jar.
- Radar (2026-11-12, built, 100 tests pass, not yet seen in game): a minimap on the HUD (main switch Radar, off by
  default, in the Hacks menu section "Radar" and with its own toggle key, so now 13 main hack switches). Every other real
  player (same NPC and tab list filter as ESP) is drawn as the face of their skin: the 8x8 face plus the hat layer from the
  skin texture, the way the tab list draws heads, framed in the team colour. Options: Range (16, 32, 64, 128 blocks), Shape
  (circle, square), Corner plus side and top/bottom offsets in GUI pixels, Size (50 to 250 px), Face Size (50% to 250% of
  10 px), Background Opacity, Turn With View (on: ahead is up; off: north up with a view arrow), Show Own Face (off: an
  arrow), Show Teammates, Show Invisible (faded), Keep Far Players on Edge (faded, in their direction), Team Colour Frame,
  Show Names (half size), Height Arrows (green above, red below, more than 3 blocks) and Grid and North. Drawn in the HUD
  pass before the hack labels, so the hotbar and HUD modules stay on top. `hacks/Radar` (drawing), `hacks/RadarMath`
  (placement, 9 tests). Deployed to the play clone 1.8.9-lunarlike on 2026-11-13.
- Dynamic FOV toggle (2026-11-13, built, 100 tests pass, not yet seen in game): key "Toggle Dynamic FOV" (Controls,
  section AxolotlClient, unbound by default) flips AxolotlClient's own `dynamicFov` option (on by default; off keeps
  the FOV still when sprinting, drawing a bow or with Speed), saves the config and shows "Dynamic FOV on/off" above
  the hotbar. The option is also listed in the Extras menu under Look, with a tooltip. Deployed to the play clone 1.8.9-lunarlike on 2026-11-13.
- Radar Centre (2026-11-13, built, 101 tests pass, not yet seen in game): option You (default) or Your Bed, plus an
  unbound key "Radar: You / Your Bed" in the hack keys section. On the bed, the bed icon is in the middle and you are
  drawn where you are (on the rim, faded, when out of range). Your bed comes from the same tracker as Bed Defense and
  the Rush Alert (nearest bed to the start spot, else your team's), so the bed scan now also runs for the radar. The
  bed radar is live only while the bed's chunk is loaded and the bed is within your render distance (render distance
  x 16 blocks, `RadarMath.bedState`, 1 test). Otherwise it turns gray, hides other players and says "Bed out of
  range (143m)" or "No bed" (not in a game, not found yet, or broken). Note: a server may send players over a
  shorter distance than its chunks, so near the edge of render distance the live radar can still miss some.
  Deployed to the play clone 1.8.9-lunarlike on 2026-11-13 together with the radar and the Dynamic FOV toggle, not yet
  seen in game; the previous jar (hack keys) is in mods-backup as axolotl-extras-0.1.0.before-radar.jar.
- Generator Counts per ore (2026-11-13, built, 101 tests pass, not yet seen in game): the two switches Diamonds and
  Emeralds / Iron and Gold are replaced by four, Count Iron, Count Gold, Count Diamonds and Count Emeralds (all on by
  default). The old two are no longer read, so anyone who had one of them off gets it back on once.
- Invisibility Alert (2026-11-13, built, 107 tests pass, not yet seen in game): a hack (off by default, in the Alerts
  section, with its own toggle key and on the emergency toggle). `hacks/InvisTracker` (no Minecraft imports, 6 tests)
  watches every enemy once the game runs: when one seen visible turns invisible and stays so for half a second, it
  keeps a mark where it happened (position, head yaw and pitch, team) for 60 seconds, one per player, and the chat
  gets `Invis <name> went invisible 23m NE` plus a ping (Invisibility Sound). Players first seen already invisible,
  teammates and spectators give no mark; a player who leaves range while waiting is dropped. The mark is a
  see-through box (0.6 x 1.8, filled and outlined in the team colour, through walls) with a 3-block line from the eyes
  the way they looked, fading over the last 5 seconds. Invisibility Waypoints (on by default) adds the obsidian-style
  beam and a label "<name> invis 12s 23m NE" (Waypoints label size, renamed from Obsidian Waypoints). Drawing and a
  local chat line only, nothing is sent. Players leaving render distance cannot be flagged: the server despawns
  them on your side, they drop out of the tracker, and coming back already invisible gives no mark (tested). Only
  After a Potion (on by default) also needs the enemy seen holding a potion with the invisibility effect (from the
  held item the server sends, read with `PotionItem.getCustomPotionEffects`) in the 3 seconds before; 110 tests pass
  (4 new). To check in game: that Hypixel shows the potion in hand while drinking (if not, switch it off),
  that a potion really sets the invisible flag on Hypixel before the armor hides, and that dying or respawning does
  not give false alerts.
- Invisibility Alert, vanish case (2026-11-13): the user saw that Hypixel does not show invisible players at all (ESP
  with Show Invisible found nothing), so the server likely stops sending them, and the flag path never fires there.
  `InvisTracker` now also gives a mark when an enemy disappears from your side while alive, nearer than 32 blocks,
  within 3 seconds of holding an invisibility potion (Hypixel does show the bottle while drinking, the user says).
  The mark is at their last seen spot and look. Dying players are fed in as not alive, so a death is not taken for
  vanishing; walking out of range without a potion, or beyond 32 blocks, never gives a mark; flagged then gone gives
  one mark. Tests rewritten around `InvisTracker.Sighting` (11).
- Hidden Player Trail (2026-11-13, built, 119 tests pass, not yet seen in game): a hack (off by default, Alerts
  section, own toggle key, on the emergency toggle). `mixin/HiddenTrailMixin` (visuals config) reads, without
  changing anything, the server's sound packets, particle packets and block breaking progress (main thread run
  only). Footstep (`step.*`), hurt, drink, eat, burp and bow sounds and footstep particles become a crumb when no
  loaded living entity (armor stands left out: generators carry them) is within 2 blocks; block breaking counts when
  the breaking entity id is not loaded (and not you). `hacks/HiddenTrail` (no Minecraft imports, 7 tests) snaps each
  crumb to the middle of the block (for a sound at the feet, the lower block of the two), skips the same block
  again within half a second, links crumbs within 4.5 blocks and 2 seconds into a walk, keeps at most 300, and drops
  them after Trail Time (5/10/20/30 s, default 10). Who it is: the Invisibility Alert mark of that entity id for
  block breaking, else the only mark there is (two or more: unknown, drawn pale purple). Style Markers: cubes and
  lines through walls, a red cube for a hit, an outline round a block being broken. Style Particles: coloured
  redstone dust at each crumb every 4 ticks (walls hide it). Labels over the newest crumb of each walk (Waypoints
  size) and a chat line when a trail starts after 5 quiet seconds. The first 40 crumbs per world are written to
  latest.log ("Hidden trail: step.stone at x y z"), so one game shows what Hypixel really sends. Only reads packets
  and draws, nothing is sent. To check in game: whether Hypixel sends step sounds for hidden invisible players (if
  not, only hits, drinking, bow shots and block breaking give crumbs), false crumbs from mobs or players at the edge
  of what is loaded, and teammates who drink invisibility (they may show up as hidden too).
- Fix (2026-11-13): invisible players were dropped as NPCs. The user saw no ESP box on an invisible player (Show
  Invisible on) although their held item still showed, so the player was still sent. The NPC filter skipped every
  player on a team with name tags set to "never", and servers hide invisible players' name tags that way. The rule
  now only counts for visible players (NPCs are still caught by version 2 UUIDs, the tab list and "[NPC]"). This
  fixes ESP, tracers, nametags, the radar and the Invisibility Alert for invisible players. Invisible players also no
  longer "explain" a sound for the hidden trail, so their steps go on it too. An Invisibility Alert now writes one
  line to latest.log saying whether the player was still sent (flagged) or gone from your side.
- Fix (2026-11-14): the game crashed when the radar was switched on ("Registering texture", NullPointerException in
  `Radar.face`). In these yarn mappings `AbstractClientPlayerEntity.getSkinId()` returns the cape (null without one)
  and `getCapeId()` returns the skin; the radar used `getSkinId()`, so a player without a cape crashed it (and one
  with a cape would have shown a piece of the cape). It now uses `getCapeId()` and draws a gray square if there is
  no texture at all. Deployed to the play clone 1.8.9-lunarlike on 2026-11-14; the crashing jar is in mods-backup as
  axolotl-extras-0.1.0.before-radar-crash-fix.jar.
- README (2026-11-14): the hacks moved out of the one long table row into their own "Hacks" section, grouped like the
  Hacks menu (ESP, Nametags and labels, Bed Wars info, Alerts, Projectiles, Radar, Vision, Hack keys). Each category
  is a collapsed block (`<details>`) with a table of its hacks and their fine-tuning switches.
- Radar in the HUD editor (2026-11-15, built, 119 tests pass, not yet seen in game): `hacks/RadarHud`, an AxolotlClient
  HUD entry ("Radar"), so the radar is dragged in the HUD editor like the other HUD modules. It counts as on whenever
  the Radar hack is on (so the emergency toggle and the toggle key still switch it), its width and height follow Radar
  Size, and it saves only its position (no own on/off switch, no scale, so the two menus cannot disagree). The radar
  is now drawn by that entry with the other HUD modules, no longer from the hack labels pass. Radar Corner and the two
  offsets are gone; the default spot is the top right, where the radar sat before.
- Dreams game start (2026-11-15, built, 127 tests pass, not yet seen in game): a Lucky Blocks game showed that
  AxolotlClient and the add-on only start a Bed Wars game on "Protect your bed and destroy the enemy beds.", which
  that mode does not print, so AxolotlClient's Bed Wars module and everything built on it (stats, KDR, Chat Stats
  At Start, Hardcore Hearts) stayed off. Every start in the logs (43 normal games and 1 Lucky Blocks game) opens with
  a "▬" line and then a title starting with "Bed Wars", and no other banner does (Duels shows "Bridge Duel").
  `bedwars/GameStartWatcher` starts a game on that title (the old line still counts on its own, never twice for one
  banner); the add-on's own start handling uses it, and `mixin/BedwarsModStartMixin` starts AxolotlClient's game the
  same way AxolotlClient does (a 10 tick countdown) and logs "[Extras] Bed Wars game start seen". The other Dreams
  modes (Rush, Ultimate, Voidless, Armed, Swappage, Castle) are not checked yet. 5 tests.
- Clearer names (2026-11-15): ESP and the radar each have a "Show Invisible" and a "Show Teammates" switch, which
  looked like duplicates. They are now "ESP: Show Invisible", "ESP: Show Teammates" (ESP, tracers and nametags) and
  "Radar: Show Invisible", "Radar: Show Teammates" (the radar only), and each tooltip points to the other switch.
  Labels only; the saved option names are unchanged, so settings carry over.
- Back to You When Bed Breaks (2026-11-15, built, 120 tests pass, not yet seen in game): a radar option, on by
  default. With Radar Centre on Your Bed, once your bed is broken the radar centres on you for the rest of the game.
  The bed counts as broken when its chunk is loaded and the bed block is gone (one in an unloaded chunk counts as
  still there). Radar Centre itself is not changed: a new game (or a new world) clears the broken mark, so the radar
  starts on your bed again. Off: the radar stays gray with "No bed" as before. `RadarMath.centreOnBed`, 1 test.
- Teams from the name prefix, spectators left out (2026-11-15, built, 121 tests pass, not yet seen in game): you
  reported that teammate detection did not work. The log of the two games on 2026-11-15 shows the Rush Alert was right
  about every player with a team letter (you were Blue, then Red; the alerts were for Green and Blue players who then
  broke your bed), but it also fired for gray names without a letter (`nufikfufik`, `_amethyst08`) a few seconds
  after a team was eliminated: spectators, which Hypixel makes invisible, flying and gray. The old check read the
  first colour code, and gray counted as the Gray team. Now `TeamGuess.fromTeamPrefix` reads the letter and colour
  Hypixel puts in front of every name in a game ("§9§lB Name"; both must agree), your own team comes from your own
  prefix first (then the sidebar, then your name colour), and while your name has a prefix, players without one are
  left out of every hack (ESP, radar, Rush Alert, Invisibility Alert, bed team votes). Without prefixes (another
  server or mode) it works as before. 1 test. You then said the alert named a teammate right after they respawned,
  gray and without a letter: Hypixel grays out players waiting to respawn, and they reappear next to your bed still
  gray for a moment. Those are left out by the same rule, and the rule (and your team) now holds for the whole world,
  also while you are dead yourself (your own name is gray then, which would otherwise have made you "Gray").
- Deployed to the play clone 1.8.9-lunarlike on 2026-11-13 (ore switches, Invisibility Alert, Hidden Player Trail,
  the NPC filter fix), not yet seen in game; the previous jar (radar) is in mods-backup as
  axolotl-extras-0.1.0.before-invis.jar.

## In progress

- Stats showing "?" and made-up numbers (reported 2026-11-07, fixed in two rounds, installed in the play clone, not yet seen on
  Hypixel). Cause, in AxolotlClient: `BedwarsPlayer.tick` asks for a player's stats once, `HypixelAbstractionLayer.getPlayer`
  waits one microsecond for the web request, and when the answer is not there yet it stores made-up stats (`generateFake`,
  including a winstreak) for good. It also caps itself at 56 lookups per minute (the waiting lobby, where Hypixel scrambles every
  name, uses them up), remembers a failed request for good, and the HTTP client runs about 2 requests at a time. Not the key: a
  normal Hypixel API key, the same public API the stats websites use. Round 1 (asking AxolotlClient's cache again every few
  ticks) was not enough: after a full 10 second wait only you had stats, the other 15 were still "?". Round 2 (2026-11-07,
  evening): the add-on loads the stats itself (`StatsLoader`, option "Reliable Bed Wars Stats", default on): 4 threads, retries
  with backoff, pauses on a 429 or when few requests are left, treats a player Hypixel has no data for (nicked) as unknown. Tested
  in a test instance against the real API: 18 lookups (2 real accounts, 16 made-up ids) done in 1.75 s, the 2 real ones loaded, the
  16 made-up ones unknown, and the field names give real numbers for your account (252 final kills, 148 final deaths, 13 stars).
  `BedwarsPlayerMixin` swaps the real numbers in for any player who only has made-up ones, keeping the kills of the current game.
  Unknown stats show "?" above heads and in the tab list; the end of game winstreak list skips them; the lobby level head shows
  "Level: ?" instead of random levels like 137; the chat stats list waits up to 15 seconds, or less when everyone is settled. At
  startup the log says "Stats patch applied" or "STATS PATCH NOT APPLIED" for each patch (all applied in a test launch).
  Nicked players have no stats and stay "?". To check next game: the numbers should be there 1 to 3 seconds after the start.
- Nothing is being built right now. The UI restyle (Modern Menus, Extras Menu, HUD corners, Quickplay) is done, see Completed.
- Raw mouse input: the cause of the "enhanced pointer precision" feel was fixed (AxolotlClient's own option, now kept off).
  Still unconfirmed: whether it feels right now, and the possible double counting of mouse devices by the Raw Input mod
  (run `ji\run-probe.cmd` from the scratchpad with `!` if it still feels too fast).

- Ornithe instance `1.8.9-ornithe-oldanim` (Prism, separate from the other two) for trying OldAnimations (1.7 visuals).
  Written by hand, never launched yet. Contents: Fabric Loader 0.20.1 on Ornithe generation 2 intermediary for 1.8.9, Flap
  0.0.8, Ornithe Standard Libraries 0.22.2 (beta), OldAnimations 1.2.0, Java 21 override, 2 to 4 GB memory. Hashes of both mod
  jars match Modrinth's. I did not run the official installer: its release page has no binaries and the sandbox refused a
  build from source, so I wrote the same files its source describes (`prism_pack.rs`). Untested whether Prism accepts it.
  OldAnimations 1.2.0 lists no dependencies on Modrinth but its code calls OSL, so OSL is included. AxolotlClient is not in
  this instance, and Mod Menu is not installed either, so I do not know yet where OldAnimations' options show up.
- Not tested: the server "block_mods" message path of Ignore Server Blocks (needs a server that sends it), and anything on a
  real Hypixel connection (the test account is offline, so I did not join Hypixel).

## To do

Needs a game window (waiting for your go-ahead):

1. Try Freelook, Toggle Sprint and the toggles on real Hypixel with your own account (I can't log in for you).
2. Test the Resource Counter and Upgrade Display in a real Bed Wars game. The counter shows whenever the sidebar title
   contains "BED WARS", which includes the lobby. I did not guess a lobby/game check without seeing the real sidebars.
3. Optimization: measured, Phosphor-Legacy shows no measurable speed benefit (see "Optimization findings" below). Kept
   installed in the play clone by your decision on 2026-11-06. Watch for dark patches from lighting bugs; if one shows up,
   delete `phosphor-legacy-0.1.5+1.8.9.jar` from `mods`.
4. UI leftovers (small, optional): the title screen panorama and logo are not restyled, and the colour picker popup of
   AxolotlClient was never opened to check it. A Bed Wars preview inside the Extras Menu is an idea, not started.

Needs a decision:

5. First launch of `1.8.9-ornithe-oldanim` (needs a window): check it starts, find where OldAnimations options are, and
   compare with the Legacy Fabric clone for fps. If it works, the open question is how to combine it with your AxolotlClient
   setup, since AxolotlClient has no Ornithe build for 1.8.9 that I have checked.
6. 1.7 Visuals (older notes, now done in a smaller form, see "1.7 visuals" below): OldAnimations lives on an `ornithe` branch only (Ornithe mappings, not Legacy Fabric), 100+ options,
   Kotlin-DSL Gradle build. A port means re-mapping its mixins to yarn 1.8.9 and checking each animation by eye. I did not
   start it because none of it can be verified without the window. Options: port a small set (sword block pose, bow and
   eat animation, hurt tint), or try the Ornithe build on a separate Ornithe instance.

## 1.7 visuals (2026-11-06)

Ported from OldAnimations (AxolotlClient, LGPL-3.0, source from its `ornithe` branch) into the add-on, as an optional mixin config
(`axolotlextras.visuals.mixins.json`, required false, so a failing patch cannot stop the game from starting). Each has its own switch
in the Extras Menu (F8, section "1.7 visuals"), all off by default. Checked in a test instance with a window minimized, using
screenshots taken from inside the game (`perf/BenchDriver`), options off against on:

- Block Hitting: with a hit followed at once by blocking, vanilla shows the block pose with no swing; with the switch on the sword swings through the screen. Vanilla 1.8.9 also throws away left clicks while you hold right click, and this does not change that (it is visual only, nothing is sent).
- 1.7 Item Positions: sword, bow (drawn and idle), fishing rod are larger and sit differently; blocks look the same as before. I cannot compare with a real 1.7 client here, so the values are OldAnimations' own, moved to the 1.8 models.
- 1.7 Sneaking: logged per tick: the camera height drops at once on sneak and rises 1.54, 1.587, 1.604, 1.614, 1.618, 1.619 after letting go (60 percent of the gap per tick, the 1.7 rate).
- 1.7 Sword Block (outside view): the sword is held across the chest with the arm out in front, instead of upright at the side.

Left out on purpose: attacking while blocking (a gameplay change, not a visual) and the third person sneak model and item
positions (many pieces, easy to get wrong without a 1.7 client to compare).

Test tool changes: the test driver can now give items, select slots, turn the camera, hold keys, switch these options, and
save screenshots, all from a steps file with no keyboard or mouse input. The sprint, jump, forward and sneak keys cannot be
pressed in code because AxolotlClient reads the real keyboard for them, so a small wrapper input (`perf/ForcedInput`) forces
them. Caveat for the earlier optimization tests: their movement scene pressed forward the old way, so the player turned and
jumped but did not really run forward. The conclusion (Phosphor makes no measurable difference) is based on that scene; a
rerun with real running would also load new chunks, which is where a lighting engine matters most.

## Optimization findings (2026-11-06)

Measured on a separate copy of the clone (`1.8.9-perf`, 2880x1800 fullscreen, fps uncapped) with an in-game frame recorder
(`perf/FrameBench`, started by an `AXBENCH label seconds` chat line in a singleplayer world only; it writes the summary to the
log and to `config/axolotl-extras-bench/`). Nothing is sent anywhere.

- Plain scene, 30 s: 219 fps on a cold start, then 246 and 251 fps warmed up (1% low 121, then 142 and 152).
- 128 villagers in view: 139 fps average, 1% low 88.
- Lighting stress (a 30x10x30 glowstone block toggled every 1.5 s): 217 and 222 fps, 1% low 108 and 116.
- Phosphor-Legacy 0.1.5 (the one lighting engine mod for 1.8.9 on Legacy Fabric; jar hash matches Modrinth, static scan shows
  no network, process or native code). The first single runs looked like a big win for 1% lows (185 against 108 to 152), but
  that was noise. Repeated as 8 alternating sessions (A without, B with, order A B B A A B B A, a 15 s warm-up and 30 s
  recordings), the two were the same:
  - Standing: 305 fps without (1% low 152), 300 with (148).
  - Lighting stress: 278 without (1% low 128), 279 with (130).
  - 128 villagers: 161 without (114), 156 with (111).
  - Two sessions were thrown away because the whole machine slowed down (one hitch of 838 ms, fps down by a third).
- Movement scene (the player sprints and jumps in circles, turning every tick; about 530 particles alive at the peak; 32 villagers
  with name tags, 16 named armor stands and 16 item drops, all client side): sessions A1, B2, B3, A4 in that order, three 40 s
  recordings each after a 15 s warm-up.
  - A1: 168 (still warming up), 235 and 233 fps, 1% low 42 to 44.
  - B2: 198, 199 and 190 fps, 1% low 51 to 52.
  - B3: 230, 228 and 231 fps, 1% low 41 to 43.
  - A4: 103, 100 and 104 fps, 1% low 40 to 48.
  The same jar set gave 235 fps in one session and 102 in another, so the machine's state moves the result by a factor of two,
  far more than any mod could. Where the machine state was the same (A1 recordings 2 and 3 against B3), the numbers match:
  234 against 230 fps, 1% low 43 against 42. Conclusion: no measurable benefit from Phosphor. Time-slicing its light queue (the
  one patch that could shorten worst frames) is not worth building, since light updates do not show up as the bottleneck.
  It is still installed in the play clone and the test instances. Remove it from `mods` to go back to vanilla lighting.
- Test method problems found on the way, so future runs avoid them: a leftover `find` process of mine used a core for hours and
  slowed the earlier movement runs (stopped; those runs are not used); typing test commands with SendKeys leaked into the
  user's own windows when the game lost focus, and fails on a locked screen or when the first launch is slow. The recorder
  now has `perf/BenchDriver` instead, which reads `config/axolotl-extras-bench/run.txt` (open world, a few safe commands,
  recordings, quit) and needs no keyboard or mouse input. The 64 scene entities are spawned on the client in one tick instead
  of through about 90 chat commands.
- Where the time goes (60 stack samples of the render thread): in the entity scene 25 of 60 samples sit in `glCallList`
  (entity models are old display lists, so this is graphics driver cost), 10 in `glDrawArrays`; in the plain scene `glDrawArrays`
  for terrain and the chunk renderer lead. Nothing in Minecraft's own Java code stands out, so a Java side optimization
  would not move these numbers. Rewriting the model renderer to use buffers is a large change with a risk of visual bugs.
- The larger factor is the machine. With no game running, CPU use was already above 100 percent of one core: OneDrive about 83
  percent of a core, CrossDeviceService 29, Rainmeter 5, plus the Explorer windows on synced folders. The power plan is Balanced.
  Those cause the run to run swings (219 against 251 fps for the same scene) and would cause frame drops in a game.

What is likely to help in a real game, none of it a mod: pause OneDrive while playing, close Rainmeter, switch Windows to the
High performance (or Best performance) power plan. There is no discrete GPU to switch to: the laptop has only an Intel Arc 140V
(integrated), which shares its power and heat budget with the CPU, so background CPU load also costs graphics speed. At 250 fps there is no headroom problem for Bed Wars on this hardware, so more mods are not needed.

## Risks and notes

- Ignore Server Blocks removes a safeguard that AxolotlClient ships on purpose. Whether Hypixel or another server allows
  Freelook, Toggle Sneak or the others is the server's rule, not something the client can settle. I could not read
  Hypixel's official page (403), so I did not confirm it. Switch the option off to get the old behaviour back.
- AxolotlClient's badges feature was on by default and contacted its own server (it sent your player UUID, and asked about
  other players' UUIDs). The Privacy Lock now blocks that, see `PRIVACY-AUDIT.md`. The "Error while logging in!" log line
  from earlier test sessions was this call failing.
- Your clone config is live: it was changed by your own play, and I changed Toggle Sprint to enabled with an empty
  placeholder.
- Backups: the previous add-on jar is in `instances/1.8.9-lunarlike/minecraft/mods-backup`, config backups sit next to
  `AxolotlClient.json`.
- Workflow (2026-11-13): changes now go to `main` through pull requests, one branch per piece of work, at the user's
  request. GitHub Actions builds and tests every pull request (once per push, a newer push cancels the older build)
  and `main` after each merge; branches without a pull request are not built. A failed build keeps the test reports
  as an artifact.
