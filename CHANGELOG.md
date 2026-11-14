# Changelog - Obsidium & Addons

All notable changes to the **Obsidium** core mod and its official addons will be documented in this file.

---

## [1.0.0] - 2026-11-08

### Architecture
* **Modular Multi-Project Architecture**:
  * Restructured the repository into a multi-project Gradle setup.
  * Core mod remains focused as `:obsidium` (`obsidium-1.0.0.jar`).
  * New autonomous railway and tree farm system modularized into official subproject addon `:addons:mythic_rails` (`obsidium-mythic-rails-1.0.0.jar`).

### Official Addon: Obsidium: Mythic Rails (v1.0.0)
* **Autonomous Convoys**: Multi-cart coupling system via Volcanite Wrench or standard wrenches.
* **Specialized Carts**:
  * *Hephaestean Magma Locomotive*: Pulls and drives convoys with solid fuel / lava consumption.
  * *Demetrian Arborist Cart*: Automatically scans, fells trees, collects timber/fruits, and replants saplings.
  * *Tartarean Smelter Cart*: Rapid mobile log smelting into charcoal with direct locomotive auto-refueling.
  * *Hermes Freight Cart*: High-capacity modular 27-slot convoy cargo chest.
* **Railway & Station Infrastructure**:
  * *Hephaestean MagRail*: High-speed volcanic booster track (over 2x vanilla speed).
  * *Hermes Docking Rail*: Smart station track featuring centering brakes and redstone catapult launching.
  * *Hermes Depot Unloader*: Station unloader with sapling conservation and automated departure signaling.
  * *Hephaestean Depot Fueler*: Automatic locomotive refueling station.
* **Visuals & Art**: 15 custom pixel-art textures and distinct custom 3D minecart chassis.
* **Compatibility**: Soft-dependency on Obsidium core with universal fallback support for vanilla and third-party mods.
