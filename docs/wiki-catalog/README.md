# Wiki item catalog

Category 02 documents machines, generators and craftable industrial components listed
in `scripts/wiki-machines.json`. Each entry includes a source reference for its role.
Storage, transport, ritual blocks and hidden formed parts are outside this category.

Category 01 documents the core mod's registered ingots, dusts, nuggets and raw
metal items (excluding storage blocks), plus the core components explicitly
listed in `scripts/build-wiki-catalog.py`. Further categories remain future batches.

From the repository root, regenerate the data and copied assets with:

```sh
python3 scripts/build-wiki-catalog.py
node scripts/tests/wiki-catalog.test.cjs
node scripts/tests/wiki-catalog.test.cjs machines
```

The generator uses the local Minecraft 1.21 client JAR for vanilla ingredient
textures and names. Pass `--minecraft-jar /path/to/minecraft-client.jar` when the
default Fabric Loom cache is unavailable. It never downloads game files.

Item images come from game assets, with a representative face for block
ingredients. Recipe JSON files are copied exactly from the core mod's recipe
directory. Machine, mining and mob-drop routes are separately documented with
source references; manually documented routes must be reviewed when Java logic
or loot tables change. This is not a complete export of every machine recipe.
Do not hand-edit generated JSON or assets.

Preview using an HTTP server (opening the HTML directly cannot load JSON):

```sh
python3 -m http.server 8765 --directory docs
```

Open `http://localhost:8765/wiki.html?page=Item-Catalog`.
The Node checks cover assets, copied recipe fidelity and renderer behavior;
they do not replace browser layout or in-game recipe testing.

The multiblock guide uses `scripts/wiki-multiblocks.json` as its reviewed source.
The same generator exports its component textures and public data. After changing
manual text or formation validators, review both example layouts and accepted-layout
notes, regenerate, then run `node scripts/tests/wiki-multiblocks.test.cjs`.

Category 03 exports every registered core pipe, wire and universal conduit from
`scripts/wiki-transport.json`. Run `node scripts/tests/wiki-catalog.test.cjs transport`;
this also checks coverage against matching block registrations.

Category 04 documents item/fluid storage and its network, disks, components,
backpacks and storage upgrades via `scripts/wiki-storage.json`. Processing barrels,
energy storage and creative-only blocks are outside this batch.
Run `node scripts/tests/wiki-catalog.test.cjs storage` after regeneration.

Category 05 uses `scripts/wiki-botany.json` for crops, living-tree saplings/fruit,
grafting, botanical tools, automation and both manuals. Gameplay acquisition
routes are separate from crafting JSON. Regenerate and run
`node scripts/tests/wiki-catalog.test.cjs botany`. The practical manual is
`docs/wiki/Botany-Manual.md`; check it against the in-game guide and Java rules.

Category 06 uses `scripts/wiki-energy.json` for energy storage, wireless power,
charging, configuration and compatible upgrades. Storage upgrades intentionally
appear here and in Storage. Run `node scripts/tests/wiki-catalog.test.cjs energy`.
Generators and wires keep their dedicated categories.

Category 07 uses `scripts/wiki-equipment.json` for equipment families, mythic
handheld/wearable items and exploration/safety instruments. Run
`node scripts/tests/wiki-catalog.test.cjs equipment`; family coverage is checked
against literal registrations, including the Hephaestean Paxel.

Category 08 uses `scripts/wiki-building.json` for tree wood/leaves, wood shapes,
ores, compact material blocks, glass, lighting and cement. Run
`node scripts/tests/wiki-catalog.test.cjs building`; suffix-based family coverage
is checked against registrations. Pyretic Titan logs/leaves are produced by the Bonsai Planter from the rare
mutation sapling; this production route is documented separately from block loot.

Wood stairs, slabs, fences and fence gates use self-contained SVG projections
from resolved Minecraft model cuboids, UVs and source textures via
`scripts/wiki_block_icons.py`, rather than a shared plank texture.

Categories 09/10 use `scripts/wiki-mythology.json` and `scripts/wiki-supplies.json`
for rituals, recovery items, fluid containers, consumables, teleportation and
Creative/testing items. `completion.json` reports coverage of all explicit core
item registrations; aliases are not additional items. Run:

```sh
node scripts/tests/wiki-catalog.test.cjs mythology
node scripts/tests/wiki-catalog.test.cjs supplies
node scripts/tests/wiki-completion.test.cjs
```

Crafting station labels derive from `CraftingMenuMixin.isEarlyGameRecipe` and its
Obsidium-input guard. The generator reads prefix/substring/exact whitelist rules.
Review this derivation if the Java rule implementation changes. Labels concern
vanilla crafting tables and the Volcanite Worktable, not every third-party crafter.
The catalog documents item acquisition and checked-in crafting recipes; it is not
an exhaustive machine processing recipe viewer or a replacement for in-game QA.

The model-icon renderer requires Pillow. It rasterizes source UVs onto visible
cuboid faces with a consistent camera, then embeds the transparent render in SVG.
Implementation references and raw recipe JSON remain internal build/test data;
they are not displayed in the player-facing catalog.
