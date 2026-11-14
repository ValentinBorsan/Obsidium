# 🌋 Obsidium: Olympian Odyssey — Modpack Creator Guide

This directory contains the production-ready architecture for the official **Obsidium: Olympian Odyssey** modpack on **Minecraft 1.21 Fabric**.

---

## 📁 Directory Structure
* [`modrinth.index.json`](file:///Users/valentinborsan/DEV/ObsidianMod/modpack/modrinth.index.json): Official manifest specification for Modrinth export (`.mrpack`).
* [`modlist.md`](file:///Users/valentinborsan/DEV/ObsidianMod/modpack/modlist.md): Curated list of required and recommended mods (Performance, Recipe Viewers, Terrain, Combat).
* [`config/obsidium.json`](file:///Users/valentinborsan/DEV/ObsidianMod/modpack/config/obsidium.json): Pre-calibrated configuration file for optimal gameplay balance.
* [`quests/questline_guide.md`](file:///Users/valentinborsan/DEV/ObsidianMod/modpack/quests/questline_guide.md): 4-chapter narrative quest progression (The Ash Age $\rightarrow$ The Volcanic Grid $\rightarrow$ Sacred Dionysian Orchards $\rightarrow$ Olympian Ascension).
* [`Obsidium-Olympian-Odyssey-1.0.0.mrpack`](file:///Users/valentinborsan/DEV/ObsidianMod/modpack/Obsidium-Olympian-Odyssey-1.0.0.mrpack): Production-ready package file for 1-click import into Prism Launcher, Modrinth App, or ATLauncher.

---

## 🚀 How to Test the Modpack Locally (Prism Launcher / Modrinth App / CurseForge)

### Method 1: In Prism Launcher (Recommended)
1. Open **Prism Launcher** $\rightarrow$ Click **Add Instance**.
2. Select:
   * **Version**: `1.21`
   * **Modloader**: `Fabric` (version `0.16.5` or newer).
3. In the instance window, click **Mods** $\rightarrow$ **Download Mods**:
   * Add the recommended mods from [`modlist.md`](file:///Users/valentinborsan/DEV/ObsidianMod/modpack/modlist.md) (Sodium, Lithium, FerriteCore, REI, Jade, Better Combat, etc.).
4. Copy the compiled mod jar:
   * `build/libs/obsidium-1.0.0.jar` directly into the `.minecraft/mods/` directory.
5. Copy `modpack/config/obsidium.json` into `.minecraft/config/`.
6. Launch the game!

---

## 🌐 Deploying to a Dedicated Server
1. Install a Fabric 1.21 server environment with Java 21+.
2. Place `obsidium-1.0.0.jar` and server-compatible performance mods (Lithium, FerriteCore) in the server's `mods/` directory.
3. Place `modpack/config/obsidium.json` in the server's `config/` directory.
4. Client-side visual mods (Sodium, ImmediatelyFast, REI, Jade, Better Combat) will run locally on connecting player clients.
5. Enjoy rock-solid 20 TPS gameplay with fully synced network capability.

---

## 📦 How to Build the `.mrpack` Archive
Run the bundling script or archive:
1. `modrinth.index.json`
2. `overrides/` folder (containing `config/obsidium.json` and `mods/obsidium-1.0.0.jar`).
3. Save as `Obsidium-Olympian-Odyssey-1.0.0.mrpack`.
