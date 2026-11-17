# Wiki item catalog

Category 01 documents the core mod's registered ingots, dusts, nuggets and raw
metal items (excluding storage blocks), plus the core components explicitly
listed in `scripts/build-wiki-catalog.py`. Other categories remain future batches.

From the repository root, regenerate the data and copied assets with:

```sh
python3 scripts/build-wiki-catalog.py
node scripts/tests/wiki-catalog.test.cjs
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
