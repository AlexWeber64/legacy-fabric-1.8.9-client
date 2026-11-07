# Progress

Goal: a Lunar Client style setup for Minecraft 1.8.9 on Legacy Fabric (Prism instance `1.8.9-lunarlike`), where every
feature can be switched on and off at will. Features come from AxolotlClient where it has them, and from the
`axolotl-extras` add-on in this folder where it does not.

Last updated: 2026-11-07.

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
