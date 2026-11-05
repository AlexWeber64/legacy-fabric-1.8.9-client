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
3. Optimization mod (not started). The benchmark notes list Phosphor-Legacy and Ksyxis as candidates, neither installed.
   Any of them needs before and after fps runs, which need the game open.
4. Optional: a nicer menu look. Currently it reuses AxolotlClient's own UI. Needs visual checks.

Needs a decision:

5. First launch of `1.8.9-ornithe-oldanim` (needs a window): check it starts, find where OldAnimations options are, and
   compare with the Legacy Fabric clone for fps. If it works, the open question is how to combine it with your AxolotlClient
   setup, since AxolotlClient has no Ornithe build for 1.8.9 that I have checked.
6. 1.7 Visuals (older notes): OldAnimations lives on an `ornithe` branch only (Ornithe mappings, not Legacy Fabric), 100+ options,
   Kotlin-DSL Gradle build. A port means re-mapping its mixins to yarn 1.8.9 and checking each animation by eye. I did not
   start it because none of it can be verified without the window. Options: port a small set (sword block pose, bow and
   eat animation, hurt tint), or try the Ornithe build on a separate Ornithe instance.

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
