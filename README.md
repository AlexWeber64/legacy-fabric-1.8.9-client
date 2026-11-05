# AxolotlClient Extras

An add-on for [AxolotlClient](https://modrinth.com/mod/axolotlclient) 3.0.6+1.8.9 on Legacy Fabric (Minecraft 1.8.9).
It adds a few features that AxolotlClient does not have. Each one has its own switch under **Extras** in the AxolotlClient
options menu (off by default, except the two noted below), and the Resource Counter is a normal AxolotlClient HUD module with its own entry in the
HUD editor.

| Feature | What it does |
|---|---|
| Bed Wars Resources (HUD) | Iron, gold, diamonds and emeralds as `inventory + ender chest = total`. Opening the ender chest always replaces the ender chest count with what is really in it. Punching the ender chest with resources in hand adds what leaves your inventory within half a second (inferred, since the client never sees the deposit). Everything is forgotten when you leave the world, leave Bed Wars, or a game starts. |
| Ignore Server Blocks (default on) | Stops AxolotlClient from switching Freelook, Toggle Sneak, Time Changer, Full Bright and Low Fire off by server address, and re-enables anything a server switches off through its `block_mods` message. Servers may still have their own rules. |
| Toggle Sprint Without HUD (default on) | Keeps the Toggle Sprint key working when the Toggle Sprint HUD module is switched off. |
| No Fog | Turns fog off after vanilla sets it up, on land and under water. |
| Centered Inventory | Keeps the inventory in the middle of the screen while a potion effect is active. |
| Transparent UI | Skips the dark gradient screens draw over the world. Screens without a world keep their background. |
| Quickplay Menu | Press Y (rebindable) to open a grid of `/play` buttons. Only on hypixel.net unless "Hypixel Only" is switched off. The command names are unverified, see `QuickplayEntries.java`. |

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
