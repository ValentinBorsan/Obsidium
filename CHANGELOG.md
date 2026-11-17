# Changelog

## Website — Navigation & Collection Refinement — 2026-11-17

- Refined the index into aligned illustrated category cards, with category search,
  clear/keyboard controls, a three-step workshop introduction and practical FAQ.
- Reduced catalog header clutter by moving alternate categories into a disclosure;
  added a collection breadcrumb, clearer crafting help and larger item portraits.
- Improved small-screen navigation with scrollable category links and active-page
  accessibility; retained large recipes and neutral icon backgrounds.
- Validation: landing search interaction checks, all catalog/multiblock/completion
  checks and local link/asset checks pass. Browser visual review remains pending.

## Wiki — Stair Perspective & Cleaner Recipe Pages — 2026-11-17

- Corrected the model camera/visible-face projection so stair steps face the viewer;
  regenerated all wood-shape icons and inspected the resulting stair render.
- Removed implementation/source-reference panels, raw JSON views and recipe-JSON
  links from all catalog pages and the multiblock guide. Crafting diagrams remain.
- Internal recipe/source data is retained for regeneration and verification.
- Validation: all catalog, completion and multiblock checks pass; public renderers
  are checked to omit technical reference sections.

## Wiki — Core Catalog Completion & Worktable Labels — 2026-11-17

- Completed coverage of all 405 explicit core item registrations across ten
  categories, adding 16 mythology and 27 fluids/supplies/utility items.
- Added station labels outside collapsed crafting recipes, derived from the actual
  vanilla-table whitelist and Obsidium-input restriction in CraftingMenuMixin.
- Clearly labeled Creative/testing availability and non-crafting acquisition routes.
- Validation: all ten category suites, multiblock checks and a complete registration
  audit pass; fixtures confirm Worktable/Wrench bootstrap and restricted equipment.
- Coverage excludes the addon; machine operating recipes and browser/in-game visual
  QA are separate from the item acquisition catalog.

## Wiki — Distinct Wood Shape Icons — 2026-11-17

- Replaced 36 flat wood-shape icons with self-contained SVG projections of the
  actual stairs, slab, fence and gate models, using their original textures/UVs.
- Catalog and recipe ingredient images now show each block's distinct silhouette.
- Validation: generated SVGs parse; building catalog checks cover model-image types
  and distinct stair/slab/fence/gate exports. A stair thumbnail was inspected.

## Wiki — Building Blocks — 2026-11-17

- Added 85 building/resource blocks with 59 source crafting variants, game textures,
  wood/foliage/ore/material/glass/cement filters and the eighth landing category.
- Documented harvesting separately from crafting. Pyretic Titan logs/leaves come
  from Bonsai Planter production using the sapling obtained through rare mutation.
- Validation: all eight catalog suites and multiblock checks pass, including registered
  wood/ore-family coverage, assets and exact source recipe exports.

## Wiki — Larger Construction & Recipe Diagrams — 2026-11-17

- Enlarged crafting cells up to 80px and multiblock cells to 64px, with larger
  ingredient images and labels. Catalog cards now use one column for readability.
- Enlarged Markdown recipe images to 720px within the available page width.
- Kept diagrams centered, responsive recipe sizing and keyboard-accessible horizontal
  scrolling for large multiblock layers on narrow screens.

## Wiki — Equipment & Exploration — 2026-11-17

- Added 47 equipment and exploration items with 57 source crafting variants,
  real textures, role descriptions and weapon/tool/armor filters.
- Added the seventh category to the redesigned landing page and catalog routing;
  no addon promotion was introduced.
- Validation: all seven catalog suites and multiblock checks pass; equipment-family
  coverage matches source registrations, including the Hephaestean Paxel.

## Website — Catalog Landing Redesign — 2026-11-17

- Rebuilt the landing page around six illustrated catalog categories and the
  multiblock/botany guides, with responsive layouts and keyboard navigation.
- Removed the former wiki navigation and addon promotion. Old wiki routes resolve
  to current catalog/guide pages; the default wiki entry opens Materials.
- Validation: all catalog/multiblock checks pass; local assets, current navigation
  targets, JavaScript syntax and absence of addon promotion are checked.
- Browser visual review could not run because the browser host was unavailable.

## Wiki — Energy & Upgrades — 2026-11-17

- Added 40 energy/configuration/upgrade items with 45 source crafting variants,
  game textures and role filters. Includes batteries, cells, cores, wireless pylons,
  charging, overclock, conduit upgrades, filters and storage modules.
- Linked the category from the landing page, catalog navigation and related guides.
- Validation: all six catalog suites and multiblock checks pass; recipe exports,
  assets, search/filter behavior and source references are checked.

## Wiki — Botany & Manuals — 2026-11-17

- Added 71 botanical items/blocks and both guide books with 50 crafting variants,
  real textures and explicit crop, grafting, fruit and beehive acquisition routes.
- Added a practical botany manual covering grafting, hybrids, rare mutation,
  automation and beekeeping, linked from the landing page and book cards.
- Corrected the in-game botany book to specify sneaking when harvesting scions.
- Validation: all five catalog suites and multiblock checks pass; client compilation
  verifies the manual text change. Browser/in-game visual review remains pending.

## Wiki — Storage & Items — 2026-11-17

- Added 45 storage items and blocks with 50 source crafting variants, game textures,
  role filters and links from the landing page and wiki. Includes backpacks, chests,
  item/fluid barrels, tanks, disks, network blocks, Quantum Link and storage upgrades.
- Validation: all four catalog suites and multiblock checks pass; exported recipes
  match source JSON, images and references exist, and search/filter behavior passes.

## Wiki — Pipes, Wires & Larger Recipes — 2026-11-17

- Added all 12 core pipes, wires and conduits with 13 source recipe variants,
  real textures, role filters and links from the landing page and wiki.
- Enlarged and centered recipe grids, Markdown recipe images and multiblock
  diagrams, with smaller dimensions on narrow screens.
- Validation: all three catalog suites and multiblock checks pass; transport
  coverage matches the registered core pipe/wire/conduit blocks.

## Wiki & Manual — Multiblock Construction — 2026-11-17

- Added a dedicated construction guide for nine multiblocks, with 15 layer
  diagrams, real component textures, example block counts, formation steps,
  operating notes and implementation references.
- Distinguished recommended manual layouts from accepted validator alternatives;
  documented the facing-dependent Poseidon footprint and flexible altar scan.
- Corrected the in-game manual for altar placement/retrieval, supported casings,
  forge corner blocks, collider coil minimum and example-only construction layouts.
- Validation: material and machine catalog checks pass; multiblock checks validate
  layer counts, assets, references and escaping. Client Java compilation passes.
  Browser layout and in-game visual review remain to be performed.

## Mythic Rails 0.5.0 — Station Inventories, Brakes & Coupling — 2026-11-16

- Corrected curved-rail rendering with a regenerated source and a UV model calibrated
  to the actual straight-rail connector pixels. Export verifies both outgoing edges;
  all four corner rotations share the calibrated model.
- Loading/Unloading Stations now have nine persistent buffer slots, player inventory
  access, shift-click and hopper/Fabric storage access. Adjacent inventories remain
  optional sources/destinations. Breaking a station drops its buffer once.
- Full unloading buffers hold the freight cart; empty/full completion waits about
  two seconds before automatic departure. Misaligned stations stop with a GUI status
  instead of letting carts pass. Pause permits occupied-station alignment.
- Motor off now brakes the locomotive on rails, including powered rails. Stopped or
  docked trains preserve burn reserve. Uncoupled locomotives stop at stations too.
- Added paid Chain coupling for one locomotive and one Hermes Freight Cart. Reciprocal
  UUIDs, recent rail path and spacing persist; followers track curves without copying
  cargo. Coupled station stops hold the locomotive, then resume the train on release.
- Added a visible connector, GUI unlink and shift-chain unlink; paid chains refund once.
  Missing partners hold the engine. Destruction clears links without losing freight.
- Protected stale station inventory menus and avoided unloaded-neighbor probes when
  checking redstone. Preserved the 0.4.1 powered-rail hook compatibility fix.
- Validation: **174 checks passed** across current suites: 60 train/buffer/restart,
  45 industry/GUI, 29 freight regressions and 40 station/restart. Both train and station
  fixtures stop and restart the same temporary worlds. Latest train/industry report:
  `build/reports/runtime-tests/20261116-181059-526701/summary.json`; freight/station:
  `build/reports/runtime-tests/20261116-180721-886697/summary.json`.
- Model references, bounds and locale keys checked; full offline build passed.
  All 45 remapped production checks also pass on Prism's Java 21 / Loader 0.16.5 /
  MixinExtras 0.4.1: `build/reports/production-smoke/20261116-181923-733502/summary.json`.
  The exact tested JAR was copied to both Prism instances with backups and SHA-256
  verification. Build/deployment records: `build/reports/mythic-rails-0.5.0-2026-11-16/`.
  A connected-client visual review remains necessary.

## Mythic Rails 0.4.1 — Prism Startup Fix — 2026-11-16

- Fixed the bootstrap crash with Fabric Loader 0.16.5 / MixinExtras 0.4.1:
  `FactoryRedirectWrapperMixinTransformer` raised `ClassCastException` while
  processing the custom powered-rail hook compiled by the newer development toolchain.
- Replaced the redirect with a compatible WrapOperation that delegates the original
  check and adds the Ember rail behavior. Existing IDs, inventories and worlds are unchanged.
- Reproduced the exact failure with the original packaged 0.4.0 JAR and Prism's Java 21,
  Loader 0.16.5 and MixinExtras 0.4.1 in an isolated dedicated server.
- Packaged 0.4.1 starts and stops normally on that same baseline. All **45 functional
  checks pass** on both development and remapped production JARs with the older loader.
- Added `scripts/production-server-smoke.py` to test packaged releases against Prism's
  actual cached runtime; it uses a temporary world and never opens user saves.
- Evidence: `build/reports/production-smoke/20261116-171001-054275` (negative control),
  `20261116-171021-729892` (startup), `20261116-171551-865695` (45 checks on the final production JAR);
  development run: `build/reports/runtime-tests/20261116-170836-195945`.
  These server checks do not validate the graphical client or the full modpack.
- Full offline build passed. The exact SHA-256-verified tested JAR was installed in
  both Prism instances with backups. Build/deployment records:
  `build/reports/rails-loader-compat-2026-11-16/`.

## Mythic Rails 0.4.0 — Rails, Locomotive & Station Controls — 2026-11-16

- Added Obsidian Rails and Ember Powered Rails with generated textures and vanilla
  crafting upgrades. Vanilla rails are unchanged. Custom powered rails accelerate
  and brake vanilla carts using the same parameters as vanilla powered rails.
- Added the Hephaestean Locomotive: solid-fuel inventory, motor on/off, selectable
  initial direction, saved burn reserve and a dedicated fuel GUI. No coupling yet.
- Added a boiler/chimney display model and a generated locomotive item icon.
- Loading and Unloading Stations now have a GUI showing cargo occupancy, transfer
  status, redstone hold and direction, plus pause/resume, rotation and manual release.
  Server checks reject remote interaction and rotation of occupied stations.
- Pausing holds the cart and stops transfers. Manual release is an explicit override
  of completion/redstone hold; automatic departure retains previous behavior.
- Existing carts/stations remain compatible; station pause is saved in block state.
- Validation: **113 checks passed**: 45 new-function checks, 29 freight regressions,
  39 station/restart checks. Actual server restart verifies locomotive fuel/components,
  motor state/direction and paused stations. Model/translation references checked.
  Reports under `build/reports/runtime-tests/`: `20261116-165942-810341` (industry),
  `20261116-165832-934976` (freight), `20261116-170111-263881` (restart).
- Full offline Gradle build passed, including core storage/fluid/crafting regressions.
  Verified production-remapped rail hook, GUI classes, recipes and texture dimensions;
  fixtures/artwork sources are excluded. Build and deployment logs:
  `build/reports/mythic-rails-phase3-2026-11-16/`. Updated both Prism instances with
  backups and SHA-256 verification.
- New-function testing initially caught an incorrect fixture assumption that named
  and unnamed coal should stack; the fixture now verifies component-aware rejection
  and transfer of matching fuel. In-game client visual review remains necessary.

## Mythic Rails 0.3.0 — Visual Refresh — 2026-11-16

- Added generated Obsidium-style pixel textures: dark obsidian, gold trim, cyan loading
  accents and orange unloading accents. Station top arrows indicate departure direction.
- Added a transparent freight-cart item icon, a custom minecart hull texture and a
  reinforced cargo-crate display model. Vanilla rail blocks are unchanged.
- Added a dedicated six-row freight screen with dark slots, gold borders and cyan accents.
  Vanilla inventory transfer behavior and all 54 cargo slots are retained.
- Added localized cart tooltips and retained English/Romanian station instructions.
- Obsidium: moved the Sacred Codex of Demeter tooltip from hard-coded Romanian to
  English/Romanian translation keys selected by the game language.
- Validation: **29 cart/menu checks passed**, including both shift-click directions,
  and **34 station checks passed**, including restart of the same server world.
  Reports: `build/reports/runtime-tests/20261116-163856-117405/summary.json`
  and `build/reports/runtime-tests/20261116-163718-407641/summary.json`.
- Verified model texture references and tooltip keys for both languages.
  Client sources compile; in-game visual review remains necessary.
- Full offline build passed, including core storage/fluid/crafting regressions.
  Production texture dimensions and packaged translations verified; test fixtures and
  artwork sources excluded. Log: `build/reports/rails-visual-refresh-2026-11-16/build.log`.
  Obsidium 1.1.0 and Mythic Rails 0.3.0 copied to the main and Tech Reborn QA Prism
  instances with backups and SHA-256 verification.

## Mythic Rails 0.2.0 — Phase 2 — 2026-11-16

- Added Hermes Loading and Unloading Stations beneath aligned, straight rails.
  Carts stop at the center, transfer cargo, then depart in the station's facing direction.
- Connect inventories on the left of the departure direction at station block height.
  Transfers use Fabric transactions, preserve item components and respect sided access:
  up to 8 items every 4 ticks, with no intermediate cargo buffer.
- Loading completes on an empty source or full cart; unloading completes on an empty cart.
  Missing/unloaded inventories and blocked unloading destinations keep the cart docked.
- Redstone holds departure while allowing transfers. A short completion delay avoids immediate departure.
- Persisted docking and release state prevents repeated docking on departure and survives server restart.
  Existing 0.1.0 carts remain compatible. Added station recipes, drops, mining tags and placement tooltips.
- Validation: **34 station checks passed**, including an end-to-end route and a real stop/restart
  of the same world, plus **24 freight-cart regression checks** passed.
  Reports: `20261116-155112-494479/summary.json` and `20261116-154844-096188/summary.json`
  under `build/reports/runtime-tests/`. Client rendering still needs an in-game check.

- Full offline Gradle build passed, including core storage/fluid/crafting checks.
  Verified separate 0.2.0 addon JAR, required station resources and vanilla texture references;
  runtime fixtures are excluded. Build log: `build/reports/mythic-rails-phase2-2026-11-16/build.log`.

## Mythic Rails 0.1.0 — Phase 1 — 2026-11-16

- Restored Mythic Rails as a separate Fabric addon requiring Obsidium 1.1.0+.
- Added the Hermes Freight Cart: 54 cargo slots, vanilla chest GUI and rendering,
  manual rail placement, named carts, vanilla movement and hopper transfers.
- Added a survival crafting recipe and a Tools & Utilities creative entry.
- Native Fabric integration: **24 checks passed**, covering inventory bounds,
  component-preserving entity NBT round-trip, hopper transfers, rail movement,
  item placement and exact cargo/cart drops on destruction.
- Report: `build/reports/runtime-tests/20261116-153227-529232/summary.json`.
- Full Gradle build passed, including core storage/fluid/crafting regressions. The addon JAR contains client rendering and excludes test fixtures; the core JAR remains separate.
- Custom stations, automatic departure and locomotives are planned for later phases.
  Client rendering and a full server restart still require an in-game test.

## 1.1.0 - 2026-11-16

### New Content

- **Poseidon Abyssal Pump (2×2×2 Multiblock)**:
  - Added the endgame `Poseidon Abyssal Pump` and `Poseidon Abyssal Casing` (2×2×2 compact multiblock, assembled with the Volcanite Wrench).
  - Supports **infinite water** pumping from a 2×2 water source, configurable pump speed & scan radius, fluid filtering, block replacement mode, redstone control, and Quantum Link channel integration.
  - Added formed/unformed block textures, custom GUI, and a dedicated **Poseidon** page in the Obsidium Manual (`Pipes → Poseidon`).
- **Upgraded Logistics & Quantum Linking**:
  - Added **Advanced** and **Supreme** tiers for `Hermes Item Pipe` and `Naiad Fluid Pipe`.
  - Added the **Quantum Link Module** and linked block states for `Volcanite Modular Chest`, `Volcanic Wood Liquid Barrel`, and `Reinforced Volcanic Liquid Barrel`, along with pipe configuration UI (`PipeConfigMenu`).
- **Odyssey Advancements**:
  - Added new progression advancements (`Late Industry`, `Quantum Infrastructure`, `Rare Renewal`, `Workshop Automation`).

### Fixes & Improvements

- **Naiad Fluid Pipe**: Fixed export/import collar rendering on connected endpoints, smoothed fluid extraction rates, and eliminated tank level flickering during transfer.
- **Olympian Hadron Collider**: Restricted transmutations strictly to valid collider recipes instead of converting arbitrary items into Volcanite Ingots.
- **Telluric Astrolabe**: Translated item tooltips and status messages to English.
- **Obsidium Manual**:
  - Renamed `Volcanite Storage Pipe` to **Volcanite Optic Fiber**.
  - Renamed `Tartarus Engine` to **Tartarus Piston** and added cryo-cooling / thermal throttle details.
  - Fixed high-contrast card backgrounds (`Storage → Crafter`, `Mythology → Pylons`, `Mythology → Thanatos`).
  - Fixed text/button overlaps in `Collider → Collisions` pagination and `Mythology → Ritual` (`The 7 Divine Pacts`).

- **Volcanite Storage Remote**:
  - Uses the configured range consistently when opening and keeping the terminal open; removes the previous hard-coded 128-block menu limit.
  - Resolves the controller in its linked dimension when cross-dimension access is enabled. Only loaded controller chunks are accessible; remote use never loads the destination chunk.
  - Validates the original remote, its binding, controller identity, range and dimension throughout the session. Replacing, removing or relinking the remote invalidates access.
  - Physical terminal access keeps its normal 8-block range even when a remote is carried. Unrelated menu providers and malformed links are rejected.
- **Fluid Persistence**: Quantum fluid vaults and local barrels preserve full fluid variants, including components. Plain buckets and bottles reject variants they cannot represent instead of silently stripping components.
- **Backpacks**: Open menus remain bound to the original backpack stack, preventing inventory writes to a replacement backpack.

### Validation

- Replaced deprecated chunk checks with non-loading chunk-source queries in remote access and Mythic Rails stations; corrected heading/list spacing. **56 native checks passed** (22 remote + 34 stations, including server restart), recorded in `20261116-155440-213697/summary.json`. Full offline build passed; log: `build/reports/warning-fixes-2026-11-16/build.log`.
- Native Fabric server tests run on 2026-11-16; detailed results are retained under `build/reports/runtime-tests/`.
- Storage remote: **22 checks passed**, including item-driven menu opening in the linked dimension, recorded in `20261116-150041-991613/summary.json`.
- Fluid persistence: **21 checks passed**; Quantum chest regression: **23 checks passed** in `20261116-145958-762216/summary.json`.
- Earlier fluid/pump/Quantum/Tech Reborn compatibility run: **95 checks passed** (21 + 28 + 23 + 23), recorded in `20261116-082639-394114/summary.json`.
- `./gradlew build --offline --no-daemon`: **passed**, including storage, fluid and crafting regression checks. Production JAR version **1.1.0** verified; runtime test fixtures are excluded. Build log: `build/reports/remote-fix-2026-11-16/build.log`.
- These are automated server checks. Graphical rendering, sound perception and the full survival progression still require an in-game playtest.

## 1.0.0 - 2026-11-14

- Initial release of Obsidium for Minecraft 1.21 (Fabric).
