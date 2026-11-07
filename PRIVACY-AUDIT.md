# Privacy and safety audit

Date: 2026-11-05. Scope: every jar in the `1.8.9-lunarlike` mods folder, plus AxolotlClient's source at tag 3.0.6.
Method: static only. Nothing was run for this audit.

## Are the jars the official ones?

Each third-party jar was hashed (SHA-512) and looked up on Modrinth. All four match a published release of the
expected project.

| Jar | Modrinth project | Version |
|---|---|---|
| AxolotlClient-3.0.6+1.8.9.jar | axolotlclient (p2rxzX0q) | 3.0.6+1.8.9 |
| RawInput-1.1.1-fabric.jar | rawmouseinput (cHtd43CF) | 1.1.1 |
| fabric-language-kotlin-1.14.2+kotlin.2.4.21.jar | fabric-language-kotlin (Ha28R6CL) | 1.14.2+kotlin.2.4.21 |
| legacy-fabric-api-1.13.5.jar | legacy-fabric-api (9CJED7xi) | 1.13.5 |

`axolotl-extras-0.1.0.jar` is built from the source in this folder, so it has no Modrinth entry.

A matching hash proves the file is the one the author published. It does not prove the author is trustworthy, and I did
not rebuild AxolotlClient from source to compare bytecode (the sandbox refused to run its build scripts).

## What each jar can do (class file scan, nested jars included)

The scan reads every class's constant pool for network calls, sockets, process launching, class loading tricks,
native library loading, script engines, environment reads, clipboard use, webhook URLs and credential file names.

- **AxolotlClient (plus its bundled libraries):** no raw sockets, no class loading tricks, no script engines, no
  webhook URLs, no credential file names. The one process launch is the "open this link in the browser" helper. It
  bundles native Discord SDK bridge libraries (`discord_game_sdk_jni.dll` and friends), which are only loaded if Discord
  RPC is switched on. Its network addresses are all in the list below and match the source.
- **RawInput:** no network, no process, no files outside the game. It reads mouse devices through JInput.
- **Fabric Language Kotlin and Legacy Fabric API:** no network and no process launching. The only URL opens are for
  resources inside the game's own classpath.
- **axolotl-extras:** no network, no process, no URLs.

I found nothing that looks like a remote access tool or stealer. A static scan cannot prove a negative, but there is none
of the usual machinery (sockets, remote class loading, command execution, token or browser data access).

## Everything AxolotlClient can contact

| Address | When | What it sends | Status here |
|---|---|---|---|
| `moehreag.duckdns.org/axolotlclient-api` | "Badges" option, which was ON by default | Your player UUID on join (POST) and quit (DELETE), and the UUID of every other player it sees (GET), plus your IP address as with any request | **Blocked by the Privacy Lock** |
| `dl-game-sdk.discordapp.net` | Only if Discord RPC is switched on | Downloads Discord's game SDK DLL with no integrity check, then loads it into the game | **Blocked by the Privacy Lock** (RPC is also off in your config) |
| `api.hypixel.net` | Only with a Hypixel API key set | Your key and other players' UUIDs | Not used, your key field is empty |
| `login.microsoftonline.com`, Xbox and `api.minecraftservices.com` | Only when you add an account in AxolotlClient's account screen | Microsoft sign-in | User initiated. Not blocked |
| `bin.gart.sh` | Only when you share a screenshot from its screenshot viewer | The screenshot | User initiated. Not blocked |

Your Minecraft username is not in any AxolotlClient request. The UUID identifies the account, and anyone can look a UUID
up to get the username, so treat it as equivalent.

The session access token is read by the account module, but only ever sent to Microsoft and Mojang endpoints, and only
written to `config/accounts.json` if you add an account there (your file is empty).

There is no update checker and no usage telemetry in the source or in the class file scan.

## What the Privacy Lock does

Extras option "Privacy Lock" (on by default) cancels, inside AxolotlClient:
- `NetworkHelper.setOnline`, `setOffline`, `getUser` and `getOnline`, so the AxolotlClient server is never contacted for
  badges (other players just show no badge).
- `GameSdkDownloader.downloadSdk`, and switches RPC off.

Checked so far: the target methods exist in the distributed jars, and a minimized launch of the instance (2026-11-05)
logged "[Extras] Privacy Lock patches loaded" at startup. At startup the add-on loads the patched AxolotlClient classes
on purpose, so a patch that fails to apply is logged as an error and shown as a red chat message when you join a world.
Not yet seen: a world join with the lock on (that is when AxolotlClient used to call its server). When you next play on the
clone, the log should have no "Sucessfully logged in at AxolotlClient" and no "Error while logging in" line. Your last
Hypixel session in that instance (21:59, before the lock existed) does have "Error while logging in!", the call to the
server failing.

## Added by the add-on: Reliable Bed Wars Stats (2026-11-07)

With the key in your AxolotlClient settings, the add-on's own stats loader (`StatsLoader`) sends one request per player of the
Bed Wars game you are in to `https://api.hypixel.net/v2/player?uuid=...`, with your key in the `API-Key` header. That is the same
request AxolotlClient makes with the same key (its `HypixelAbstractionLayer`), just reliably and in parallel. It contacts nothing
else, sends no UUID to anyone but Hypixel, and logs nothing that identifies a player (the log only says if Hypixel refused the key
or asked to slow down). The switch "Reliable Bed Wars Stats" turns it off, and with no key set it makes no request at all.

## Not covered

- Prism Launcher and vanilla Minecraft's own network traffic (Mojang session, skins, Prism's metadata downloads).
- Lunar Client.

## The Ornithe instance's mods

OldAnimations 1.2.0 and Ornithe Standard Libraries 0.22.2 were hash checked against Modrinth when downloaded and run
through the same class scan: no URLs, no sockets, no process launching. OldAnimations bundles AxolotlClient's config
library, whose only flagged call is the "open link in browser" helper.
