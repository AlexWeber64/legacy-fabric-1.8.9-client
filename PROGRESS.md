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
| Upgrade Display | AxolotlClient (Bed Wars team upgrades overlay) | Present, not tested on a real Hypixel game |
| Toggle Sprint | AxolotlClient (Toggle Sprint HUD) | Works in singleplayer, see "In progress" for the Hypixel side |
| Freelook | AxolotlClient | Works in singleplayer, Hypixel check removed (untested there) |
| Raw Mouse Input | RawInput mod | Done |
| No Fog | Extras | Done, tested in game |
| Keep Inventory Centered | Extras | Done, tested in game |
| Transparent UI | Extras | Done, tested in game (my reading of the setting: no dark layer over the world) |
| Resource Counter (Bed Wars) | Extras | Done, tested in singleplayer with a simulated Bed Wars sidebar |
| Hypixel Quickplay | Extras | Menu and command sending done, only Doubles confirmed |
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

## In progress

- In game test of the two new switches. I did not launch the game because you are playing on Lunar Client. To test, I need
  to open the Minecraft window, so tell me when that is fine.
- Toggle Sprint on a real server: unknown. Singleplayer works, Hypixel is untested.

## To do

1. In game test of Ignore Server Blocks (join a server with `hypixel` in its address, check Freelook is not greyed out)
   and Toggle Sprint Without HUD (module off, key K still toggles).
2. 1.7 Visuals: OldAnimations is Ornithe-only, so this needs a port or a smaller module with the main animations (sword
   block pose, eating and bow animation, damage tint, and so on). Decide scope first.
3. Verify the other Quickplay `/play` names (Bed Wars Solo, 3v3v3v3, 4v4v4v4, 4v4, and SkyWars) against a real server.
4. Test the Resource Counter and Upgrade Display in a real Bed Wars game. The counter shows whenever the sidebar title
   contains "BED WARS", which includes the lobby.
5. Optional optimization mod (not started). The benchmark notes list Phosphor-Legacy and Ksyxis as candidates, neither installed.
6. Optional: a nicer menu look. Currently it reuses AxolotlClient's own UI.
7. Optional: bind Toggle Sprint to your sprint key (currently K, sneak toggle is I, Freelook is V). Rebind under Controls.

## Risks and notes

- Ignore Server Blocks removes a safeguard that AxolotlClient ships on purpose. Whether Hypixel or another server allows
  Freelook, Toggle Sneak or the others is the server's rule, not something the client can settle. I could not read
  Hypixel's official page (403), so I did not confirm it. Switch the option off to get the old behaviour back.
- AxolotlClient tries to contact its own backend even offline and logs "Error while logging in!". Harmless.
- Your clone config is live: it was changed by your own play, and I changed Toggle Sprint to enabled with an empty
  placeholder.
- Backups: the previous add-on jar is in `instances/1.8.9-lunarlike/minecraft/mods-backup`, config backups sit next to
  `AxolotlClient.json`.
