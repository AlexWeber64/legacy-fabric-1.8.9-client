# Progress

Goal: a Lunar Client style setup for Minecraft 1.8.9 on Legacy Fabric (Prism instance `1.8.9-lunarlike`), where every
feature can be switched on and off at will. Features come from AxolotlClient where it has them, and from the
`axolotl-extras` add-on in this folder where it does not.

Last updated: 2026-11-05.

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
| 1.7 Visuals | nothing yet | Not done |

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

## In progress

- Making the client UI nicer: not started on code. AxolotlClient 3.0.6 for 1.8.9 has no style options at all (its menu is the
  plain vanilla looking one), so this means building screens, and that needs visual checks in a window.
- Raw mouse input feels odd: the RawInput mod sums every mouse-type device it finds (your Razer's two interfaces plus the
  touchpad) and replaces Minecraft's mouse movement. Waiting for a description of the symptom, and a test with the touchpad off.

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
3. Optimization mod: measured, and not built. See "Optimization findings" below. No further work unless a real Bed Wars
   game on your machine shows frame drops that the numbers below do not explain.
4. More UI: the Extras Menu and HUD corners are done. Ideas not started: restyle AxolotlClient's own options screen, the HUD editor, and the Quickplay menu to match, a Bed Wars preview in the menu.

Needs a decision:

5. First launch of `1.8.9-ornithe-oldanim` (needs a window): check it starts, find where OldAnimations options are, and
   compare with the Legacy Fabric clone for fps. If it works, the open question is how to combine it with your AxolotlClient
   setup, since AxolotlClient has no Ornithe build for 1.8.9 that I have checked.
6. 1.7 Visuals (older notes): OldAnimations lives on an `ornithe` branch only (Ornithe mappings, not Legacy Fabric), 100+ options,
   Kotlin-DSL Gradle build. A port means re-mapping its mixins to yarn 1.8.9 and checking each animation by eye. I did not
   start it because none of it can be verified without the window. Options: port a small set (sword block pose, bow and
   eat animation, hurt tint), or try the Ornithe build on a separate Ornithe instance.

## Optimization findings (2026-11-06)

Measured on a separate copy of the clone (`1.8.9-perf`, 2880x1800 fullscreen, fps uncapped) with an in-game frame recorder
(`perf/FrameBench`, started by an `AXBENCH label seconds` chat line in a singleplayer world only; it writes the summary to the
log and to `config/axolotl-extras-bench/`). Nothing is sent anywhere.

- Plain scene, 30 s: 219 fps on a cold start, then 246 and 251 fps warmed up (1% low 121, then 142 and 152).
- 128 villagers in view: 139 fps average, 1% low 88.
- Lighting stress (a 30x10x30 glowstone block toggled every 1.5 s): 217 and 222 fps, 1% low 108 and 116.
- Phosphor-Legacy 0.1.5 (the one lighting engine mod for 1.8.9 on Legacy Fabric): 224 fps standing and 219 fps under the
  lighting stress, about the same as without it. Its 1% lows were better (185 to 189 against 108 to 152) and it ran 3 garbage
  collections instead of 16, but worst frames got longer (28 to 33 ms) and the run to run spread is about as large as the
  difference, so I did not adopt it. It is removed from the test instance.
- Where the time goes (60 stack samples of the render thread): in the entity scene 25 of 60 samples sit in `glCallList`
  (entity models are old display lists, so this is graphics driver cost), 10 in `glDrawArrays`; in the plain scene `glDrawArrays`
  for terrain and the chunk renderer lead. Nothing in Minecraft's own Java code stands out, so a Java side optimization
  would not move these numbers. Rewriting the model renderer to use buffers is a large change with a risk of visual bugs.
- The larger factor is the machine. With no game running, CPU use was already above 100 percent of one core: OneDrive about 83
  percent of a core, CrossDeviceService 29, Rainmeter 5, plus the Explorer windows on synced folders. The power plan is Balanced.
  Those cause the run to run swings (219 against 251 fps for the same scene) and would cause frame drops in a game.

What is likely to help in a real game, none of it a mod: pause OneDrive while playing, close Rainmeter, switch Windows to the
High performance (or Best performance) power plan, and use the discrete GPU for `javaw.exe` in Windows graphics settings if the
laptop has one. At 250 fps there is no headroom problem for Bed Wars on this hardware, so more mods are not needed.

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
