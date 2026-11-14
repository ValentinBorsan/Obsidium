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

## License

Obsidium is distributed under CC0 1.0 Universal. See [LICENSE](LICENSE).
