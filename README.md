# Obsidium

Obsidium is a Fabric mod for Minecraft 1.21, built with Java 21. It adds volcanic machines, mineral-bearing trees, the volcanic caldera, and related items and mobs.

## Requirements

- Minecraft Java Edition **1.21**
- **Java 21**
- Fabric Loader **0.19.5 or newer**
- Fabric API for Minecraft 1.21

The mod bundles Team Reborn Energy. REI is optional and is used only in the development environment.

## Install in a client

1. Install Fabric Loader for Minecraft 1.21 and use Java 21.
2. Put `obsidium-1.0.0.jar` and the matching Fabric API jar in the instance's `mods` folder.
3. Launch the Fabric profile.

## Install on a dedicated server

1. Install a Fabric server for Minecraft 1.21 and run it once using Java 21.
2. Put `obsidium-1.0.0.jar` and Fabric API for Minecraft 1.21 in the server's `mods` folder.
3. Install the same Obsidium and Fabric API versions in every connecting client.
4. Start the server with Java 21.

Obsidium adds blocks, items, world generation, and recipes, so players need it on both the server and client. Do not put REI on a dedicated server unless another server-side mod requires it.

## Add to a modpack

Add the Obsidium jar and Fabric API to the pack's `mods` list, select Minecraft 1.21 with Fabric Loader, and set Java 21 as the runtime. Keep Minecraft, Fabric Loader, Fabric API, and Obsidium versions consistent between the pack and its server. Include the `LICENSE` file when redistributing the mod.

## Build from source

Run `./gradlew remapJar`. The installable jar is written to `build/libs/obsidium-1.0.0.jar`; the `*-sources.jar` is for development and should not be installed in a game.

GitHub Actions builds the project on pushes and pull requests and uploads the jars as a workflow artifact.

## Tests

Run `./gradlew build --offline --no-daemon` for JVM regressions, and `python3 scripts/runtime-tests.py` for isolated Fabric server checks covering pumping, Quantum chests and compaction. See [TESTING.md](TESTING.md) for prerequisites, reports and the Prism/multiplayer checklist.

## Progression

The default progression uses a thermal early game, a processing and automation mid game, and boss-gated rare renewal / Supreme technology in late game. See [the progression notes](docs/PROGRESIE_EARLY_MID_LATE.md) for changed recipes, modpack tuning and obtainability checks.

## License

Obsidium uses the **MIT License** for new project-author contributions. See [LICENSE](LICENSE). Earlier releases and material distributed under CC0 retain their CC0 permissions; see [license history](LICENSE-HISTORY.md).

### Mythic Rails — addon separat

Mythic Rails adaugă cartul de marfă Hermes cu 54 de sloturi și stații automate
de încărcare/descărcare cu GUI, șine proprii și o locomotivă cu combustibil.
[Instalare, teste și etape următoare](addons/mythic_rails/README.md).
