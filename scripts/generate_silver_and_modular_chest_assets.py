import os, json, struct, zlib

def write_png(filename, width, height, pixels):
    raw_data = bytearray()
    for row in pixels:
        raw_data.append(0) # filter type none
        for r, g, b, a in row:
            raw_data.extend((int(r), int(g), int(b), int(a)))
    
    def chunk(tag, data):
        return struct.pack('>I', len(data)) + tag + data + struct.pack('>I', zlib.crc32(tag + data) & 0xffffffff)

    ihdr = struct.pack('>IIBBBBB', width, height, 8, 6, 0, 0, 0)
    png = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', ihdr) + chunk(b'IDAT', zlib.compress(bytes(raw_data))) + chunk(b'IEND', b'')
    os.makedirs(os.path.dirname(filename), exist_ok=True)
    with open(filename, 'wb') as f:
        f.write(png)

def write_json(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(data, f, indent=2)

def clamp(v): return max(0, min(255, int(v)))

# Directories
BLOCK_TEX = "src/main/resources/assets/obsidium/textures/block"
ITEM_TEX = "src/main/resources/assets/obsidium/textures/item"
BLOCKSTATES = "src/main/resources/assets/obsidium/blockstates"
MODELS_BLOCK = "src/main/resources/assets/obsidium/models/block"
MODELS_ITEM = "src/main/resources/assets/obsidium/models/item"
RECIPES = "src/main/resources/data/obsidium/recipe"
LOOT_BLOCKS = "src/main/resources/data/obsidium/loot_table/blocks"

# ==========================================
# 1. TEXTURE GENERATORS
# ==========================================

# --- UPGRADES ---
def make_stack_upgrade():
    # Volcanite dark frame with vibrant purple/magenta stack overflow crystal & scrap rivets
    pixels = []
    for y in range(16):
        row = []
        for x in range(16):
            # Outer dark volcanite border
            if (x in (2, 13) and 2 <= y <= 13) or (y in (2, 13) and 2 <= x <= 13):
                row.append((45, 30, 40, 255))
            elif 3 <= x <= 12 and 3 <= y <= 12:
                # Golden / scrap reinforced corners
                if (x in (3, 12) and y in (3, 12)):
                    row.append((180, 140, 60, 255))
                # Central stack cards / layers (three stacked slabs)
                elif 5 <= x <= 10 and y in (5, 8, 11):
                    row.append((255, 120, 230, 255)) # Glowing magenta stack top
                elif 5 <= x <= 10 and y in (6, 9):
                    row.append((180, 50, 160, 255)) # Shaded stack layer
                elif 5 <= x <= 10 and y in (7, 10):
                    row.append((120, 30, 110, 255)) # Dark stack base
                elif 4 <= x <= 11 and 4 <= y <= 11:
                    row.append((30, 18, 32, 255)) # Dark interior
                else:
                    row.append((60, 38, 55, 255))
            else:
                row.append((0, 0, 0, 0))
        pixels.append(row)
    return pixels

def make_vacuum_upgrade():
    # Cyan magnetic swirling vortex inside volcanite frame
    pixels = []
    for y in range(16):
        row = []
        for x in range(16):
            if (x in (2, 13) and 2 <= y <= 13) or (y in (2, 13) and 2 <= x <= 13):
                row.append((40, 35, 45, 255))
            elif 3 <= x <= 12 and 3 <= y <= 12:
                dx = x - 7.5
                dy = y - 7.5
                dist = (dx*dx + dy*dy)**0.5
                if dist < 2.0:
                    row.append((180, 255, 255, 255)) # Bright eye of vortex
                elif dist < 3.8:
                    row.append((0, 200, 230, 255)) # Vibrant cyan ring
                elif dist < 5.2:
                    row.append((0, 110, 160, 255)) # Deep aqua
                else:
                    row.append((25, 20, 32, 255))
            else:
                row.append((0, 0, 0, 0))
        pixels.append(row)
    return pixels

def make_compactor_upgrade():
    # Orange hydraulic hydraulic piston / anvil matrix
    pixels = []
    for y in range(16):
        row = []
        for x in range(16):
            if (x in (2, 13) and 2 <= y <= 13) or (y in (2, 13) and 2 <= x <= 13):
                row.append((45, 30, 20, 255))
            elif 3 <= x <= 12 and 3 <= y <= 12:
                # Top press plate
                if 4 <= x <= 11 and y in (4, 5):
                    row.append((255, 170, 40, 255))
                # Bottom anvil plate
                elif 4 <= x <= 11 and y in (10, 11):
                    row.append((210, 110, 20, 255))
                # Compression sparks in center
                elif (x in (6, 9) and y in (7, 8)) or (x in (7, 8) and y in (6, 9)):
                    row.append((255, 240, 150, 255))
                elif 6 <= x <= 9 and 7 <= y <= 8:
                    row.append((255, 80, 20, 255))
                else:
                    row.append((35, 22, 18, 255))
            else:
                row.append((0, 0, 0, 0))
        pixels.append(row)
    return pixels

def make_void_upgrade():
    # Dark purple obsidian black hole void
    pixels = []
    for y in range(16):
        row = []
        for x in range(16):
            if (x in (2, 13) and 2 <= y <= 13) or (y in (2, 13) and 2 <= x <= 13):
                row.append((35, 20, 45, 255))
            elif 3 <= x <= 12 and 3 <= y <= 12:
                dx = x - 7.5
                dy = y - 7.5
                dist = (dx*dx + dy*dy)**0.5
                if dist < 2.2:
                    row.append((10, 5, 15, 255)) # Absolute void center
                elif dist < 3.8:
                    row.append((140, 40, 200, 255)) # Violet accretion disk
                elif dist < 5.2:
                    row.append((70, 20, 110, 255))
                else:
                    row.append((25, 14, 32, 255))
            else:
                row.append((0, 0, 0, 0))
        pixels.append(row)
    return pixels

# --- MODULAR CHEST ---
def make_modular_chest(face):
    # Obsidian steel base with magma rivets and modular docking ports
    pixels = []
    for y in range(16):
        row = []
        for x in range(16):
            # Heavy beveled frame
            is_edge = (x == 0 or x == 15 or y == 0 or y == 15)
            is_inner_edge = (x == 1 or x == 14 or y == 1 or y == 14)
            if is_edge:
                row.append((25, 18, 30, 255)) # Obsidian dark border
            elif is_inner_edge:
                if (x in (1, 14) and y in (1, 14)):
                    row.append((255, 180, 50, 255)) # Corner golden rivets
                else:
                    row.append((50, 36, 60, 255))
            else:
                if face == "front":
                    # Lock latch in center (y: 6..9, x: 7..8)
                    if 6 <= y <= 9 and 7 <= x <= 8:
                        row.append((255, 200, 60, 255)) # Golden latch
                    elif 5 <= y <= 10 and 6 <= x <= 9:
                        row.append((180, 120, 30, 255))
                    # Horizontal reinforced split
                    elif y == 7:
                        row.append((30, 22, 38, 255))
                    # 4 Modular Port Nodes on the face
                    elif (x in (3, 12) and y in (3, 12)):
                        row.append((0, 220, 240, 255)) # Glowing cyan socket node
                    elif (x in (2, 4, 11, 13) and y in (3, 12)) or (x in (3, 12) and y in (2, 4, 11, 13)):
                        row.append((35, 55, 65, 255))
                    else:
                        row.append((38, 28, 46, 255))
                elif face == "top":
                    # Circular runic core
                    dx = x - 7.5
                    dy = y - 7.5
                    dist = (dx*dx + dy*dy)**0.5
                    if dist < 2.0:
                        row.append((255, 100, 20, 255)) # Fiery core
                    elif dist < 3.5:
                        row.append((180, 50, 15, 255))
                    elif dist < 4.8:
                        row.append((65, 45, 75, 255))
                    else:
                        row.append((38, 28, 46, 255))
                else: # side
                    # X-reinforcement bracket
                    if abs(x - y) <= 1 or abs((15 - x) - y) <= 1:
                        row.append((55, 40, 65, 255))
                    else:
                        row.append((36, 26, 44, 255))
        pixels.append(row)
    return pixels

# --- SILVER MINERALS ---
def make_silver_ore(is_deepslate):
    # Silver veins (radiant white-silver, soft blue reflection)
    pixels = []
    base_col = (75, 75, 80, 255) if not is_deepslate else (45, 45, 50, 255)
    base_alt = (65, 65, 70, 255) if not is_deepslate else (35, 35, 40, 255)
    for y in range(16):
        row = []
        for x in range(16):
            # Noise stone
            c = base_col if (x * 3 + y * 7) % 5 > 1 else base_alt
            # Silver clusters
            if (3 <= x <= 5 and 4 <= y <= 7) or (10 <= x <= 13 and 8 <= y <= 11) or (7 <= x <= 9 and 12 <= y <= 14) or (8 <= x <= 10 and 2 <= y <= 4):
                if (x + y) % 3 == 0:
                    c = (245, 248, 255, 255) # Silver shine
                elif (x + y) % 2 == 0:
                    c = (210, 218, 230, 255) # Pure silver
                else:
                    c = (165, 175, 190, 255) # Silver shadow
            row.append(c)
        pixels.append(row)
    return pixels

def make_silver_block():
    pixels = []
    for y in range(16):
        row = []
        for x in range(16):
            if x == 0 or y == 0:
                row.append((245, 250, 255, 255)) # Top/left highlight
            elif x == 15 or y == 15:
                row.append((140, 150, 165, 255)) # Bottom/right shadow
            elif (x == 1 or y == 1):
                row.append((230, 238, 250, 255))
            elif (x == 14 or y == 14):
                row.append((160, 170, 185, 255))
            else:
                # Diagonal brushed metal pattern
                v = 200 + ((x + y) % 4) * 12
                row.append((v, v + 5, v + 12, 255))
        pixels.append(row)
    return pixels

def make_raw_silver_block():
    pixels = []
    for y in range(16):
        row = []
        for x in range(16):
            # Lumpy raw nodules of silver embedded in dark stone
            if (x * 11 + y * 13) % 7 < 3:
                row.append((220, 228, 240, 255)) # Raw silver nodule
            elif (x * 11 + y * 13) % 7 == 3:
                row.append((170, 180, 195, 255))
            else:
                row.append((70, 75, 85, 255)) # Matrix stone
        pixels.append(row)
    return pixels

def make_silver_ingot():
    pixels = []
    for y in range(16):
        row = []
        for x in range(16):
            # Classic 3D isometric Ingot shape
            if 4 <= y <= 11 and 3 <= x <= 12:
                if y == 4 or x == 3:
                    row.append((248, 252, 255, 255)) # Specular gleam
                elif y == 11 or x == 12:
                    row.append((150, 160, 175, 255)) # Shadow
                elif 5 <= y <= 7 and 4 <= x <= 11:
                    row.append((225, 235, 245, 255)) # Top face
                else:
                    row.append((185, 195, 210, 255)) # Front bevel
            else:
                row.append((0, 0, 0, 0))
        pixels.append(row)
    return pixels

def make_raw_silver():
    pixels = []
    for y in range(16):
        row = []
        for x in range(16):
            # Organic mineral chunk shape
            if 5 <= y <= 11 and 4 <= x <= 11 and not (x == 4 and y == 5) and not (x == 11 and y == 11):
                if (x in (5, 6) and y in (6, 7)):
                    row.append((245, 250, 255, 255))
                elif (x + y) % 2 == 0:
                    row.append((205, 215, 230, 255))
                else:
                    row.append((145, 155, 170, 255))
            else:
                row.append((0, 0, 0, 0))
        pixels.append(row)
    return pixels

def make_silver_nugget():
    pixels = []
    for y in range(16):
        row = []
        for x in range(16):
            if 6 <= y <= 9 and 6 <= x <= 9:
                if x == 6 and y == 6:
                    row.append((250, 252, 255, 255))
                elif x == 9 or y == 9:
                    row.append((150, 160, 175, 255))
                else:
                    row.append((210, 220, 235, 255))
            else:
                row.append((0, 0, 0, 0))
        pixels.append(row)
    return pixels

def make_silver_dust():
    pixels = []
    for y in range(16):
        row = []
        for x in range(16):
            if 6 <= y <= 11 and 4 <= x <= 11:
                if (x + y * 3) % 4 == 0:
                    row.append((245, 250, 255, 255))
                elif (x + y * 3) % 3 == 0:
                    row.append((195, 205, 220, 255))
                elif (x + y) % 2 == 0:
                    row.append((150, 160, 175, 255))
                else:
                    row.append((0, 0, 0, 0))
            else:
                row.append((0, 0, 0, 0))
        pixels.append(row)
    return pixels

def make_silver_petal():
    pixels = []
    for y in range(16):
        row = []
        for x in range(16):
            # Curved shimmering silver petal
            dx = x - 7.5
            dy = y - 7.5
            if (dx*dx*1.8 + dy*dy) < 14 and y >= 3 and y <= 12:
                if dx < 0 and dy < 0:
                    row.append((250, 252, 255, 255))
                elif dx > 1 or dy > 2:
                    row.append((160, 175, 195, 255))
                else:
                    row.append((215, 225, 240, 255))
            else:
                row.append((0, 0, 0, 0))
        pixels.append(row)
    return pixels

def make_silver_seed():
    pixels = []
    for y in range(16):
        row = []
        for x in range(16):
            if (6 <= x <= 9 and 7 <= y <= 11) or (7 <= x <= 8 and 6 <= y <= 12):
                if y == 6 or x == 7:
                    row.append((240, 245, 255, 255))
                elif y == 12 or x == 9:
                    row.append((140, 150, 170, 255))
                else:
                    row.append((190, 205, 225, 255))
            else:
                row.append((0, 0, 0, 0))
        pixels.append(row)
    return pixels

# --- ARTEMIS SILVER TREE ---
def make_silver_log(is_top):
    pixels = []
    for y in range(16):
        row = []
        for x in range(16):
            if is_top:
                dx = x - 7.5
                dy = y - 7.5
                dist = (dx*dx + dy*dy)**0.5
                if dist < 2.0:
                    row.append((210, 218, 230, 255)) # Silver heartwood
                elif dist < 4.5:
                    row.append((175, 182, 195, 255)) # Ring 1
                elif dist < 6.5:
                    row.append((190, 198, 210, 255)) # Ring 2
                else:
                    row.append((110, 115, 125, 255)) # Silvery bark
            else:
                # Vertical silvery bark with moonlit sheen
                if x in (0, 4, 8, 12):
                    row.append((105, 110, 120, 255)) # Bark fissure
                elif x in (1, 5, 9, 13):
                    row.append((195, 202, 215, 255)) # Bark ridge
                elif x in (2, 6, 10, 14):
                    row.append((160, 168, 180, 255))
                else:
                    row.append((135, 142, 152, 255))
        pixels.append(row)
    return pixels

def make_silver_planks():
    pixels = []
    for y in range(16):
        row = []
        for x in range(16):
            is_h_seam = (y in (3, 7, 11, 15))
            if is_h_seam:
                row.append((115, 120, 130, 255))
            else:
                v = 185 + ((x * 5 + y * 7) % 5) * 8
                row.append((v, v + 5, v + 12, 255))
        pixels.append(row)
    return pixels

def make_silver_leaves():
    pixels = []
    for y in range(16):
        row = []
        for x in range(16):
            # Intricate luminescent silver-blue foliage
            n = (x * 13 + y * 17) % 11
            if n < 3:
                row.append((240, 245, 255, 255)) # Moonlit silver leaf
            elif n < 7:
                row.append((185, 198, 218, 255))
            elif n < 10:
                row.append((130, 145, 170, 255)) # Deeper teal shadow
            else:
                row.append((0, 0, 0, 0)) # Transparency for leaves
        pixels.append(row)
    return pixels

def make_silver_sapling():
    pixels = []
    for y in range(16):
        row = []
        for x in range(16):
            # Little silver stem and leaves
            if x == 7 and 8 <= y <= 14:
                row.append((140, 145, 155, 255)) # Stem
            elif (5 <= x <= 9 and 4 <= y <= 8) or (4 <= x <= 10 and 6 <= y <= 7):
                if (x + y) % 2 == 0:
                    row.append((235, 242, 255, 255))
                else:
                    row.append((170, 185, 205, 255))
            else:
                row.append((0, 0, 0, 0))
        pixels.append(row)
    return pixels

def make_silver_fruit():
    pixels = []
    for y in range(16):
        row = []
        for x in range(16):
            # Glowing moonlit silver apple/fruit
            dx = x - 7.5
            dy = y - 7.5
            dist = (dx*dx + dy*dy)**0.5
            if y == 2 and x == 7:
                row.append((120, 90, 50, 255)) # Stem
            elif dist < 4.8:
                if dx < -1 and dy < -1:
                    row.append((255, 255, 255, 255)) # Moon gleam
                elif dist < 3.0:
                    row.append((225, 235, 250, 255))
                else:
                    row.append((165, 180, 205, 255))
            else:
                row.append((0, 0, 0, 0))
        pixels.append(row)
    return pixels

def make_crop_stage(stage):
    pixels = []
    max_y = 15 - stage * 2
    for y in range(16):
        row = []
        for x in range(16):
            if y >= max_y:
                if x in (4, 7, 11) or (stage >= 4 and x in (3, 8, 12)):
                    if stage == 7 and y <= max_y + 3:
                        row.append((240, 245, 255, 255)) # Fully ripe silver petals
                    elif (x + y) % 2 == 0:
                        row.append((190, 205, 220, 255))
                    else:
                        row.append((140, 155, 175, 255))
                else:
                    row.append((0, 0, 0, 0))
            else:
                row.append((0, 0, 0, 0))
        pixels.append(row)
    return pixels

print("Generating textures...")
write_png(f"{ITEM_TEX}/volcanite_stack_upgrade.png", 16, 16, make_stack_upgrade())
write_png(f"{ITEM_TEX}/volcanite_vacuum_upgrade.png", 16, 16, make_vacuum_upgrade())
write_png(f"{ITEM_TEX}/volcanite_compactor_upgrade.png", 16, 16, make_compactor_upgrade())
write_png(f"{ITEM_TEX}/volcanite_void_upgrade.png", 16, 16, make_void_upgrade())

write_png(f"{BLOCK_TEX}/volcanite_modular_chest_front.png", 16, 16, make_modular_chest("front"))
write_png(f"{BLOCK_TEX}/volcanite_modular_chest_top.png", 16, 16, make_modular_chest("top"))
write_png(f"{BLOCK_TEX}/volcanite_modular_chest_side.png", 16, 16, make_modular_chest("side"))

write_png(f"{BLOCK_TEX}/silver_ore.png", 16, 16, make_silver_ore(False))
write_png(f"{BLOCK_TEX}/deepslate_silver_ore.png", 16, 16, make_silver_ore(True))
write_png(f"{BLOCK_TEX}/silver_block.png", 16, 16, make_silver_block())
write_png(f"{BLOCK_TEX}/raw_silver_block.png", 16, 16, make_raw_silver_block())
write_png(f"{ITEM_TEX}/silver_ingot.png", 16, 16, make_silver_ingot())
write_png(f"{ITEM_TEX}/raw_silver.png", 16, 16, make_raw_silver())
write_png(f"{ITEM_TEX}/silver_nugget.png", 16, 16, make_silver_nugget())
write_png(f"{ITEM_TEX}/silver_dust.png", 16, 16, make_silver_dust())
write_png(f"{ITEM_TEX}/silver_petal.png", 16, 16, make_silver_petal())
write_png(f"{ITEM_TEX}/silver_seed.png", 16, 16, make_silver_seed())

write_png(f"{BLOCK_TEX}/artemis_silver_log.png", 16, 16, make_silver_log(False))
write_png(f"{BLOCK_TEX}/artemis_silver_log_top.png", 16, 16, make_silver_log(True))
write_png(f"{BLOCK_TEX}/artemis_silver_planks.png", 16, 16, make_silver_planks())
write_png(f"{BLOCK_TEX}/artemis_silver_leaves.png", 16, 16, make_silver_leaves())
write_png(f"{BLOCK_TEX}/artemis_silver_sapling.png", 16, 16, make_silver_sapling())
write_png(f"{ITEM_TEX}/artemis_silver_fruit.png", 16, 16, make_silver_fruit())

for s in range(8):
    write_png(f"{BLOCK_TEX}/silver_crop_stage{s}.png", 16, 16, make_crop_stage(s))

print("Textures generated successfully!")

# ==========================================
# 2. MODELS & BLOCKSTATES
# ==========================================
print("Generating Models & Blockstates...")

# Standard simple items
simple_items = [
    "volcanite_stack_upgrade", "volcanite_vacuum_upgrade", "volcanite_compactor_upgrade", "volcanite_void_upgrade",
    "silver_ingot", "raw_silver", "silver_nugget", "silver_dust", "silver_petal", "silver_seed", "artemis_silver_fruit"
]
for item in simple_items:
    write_json(f"{MODELS_ITEM}/{item}.json", {
        "parent": "minecraft:item/generated",
        "textures": { "layer0": f"obsidium:item/{item}" }
    })

# Standard cube blocks (ore, deepslate ore, blocks)
cube_blocks = ["silver_ore", "deepslate_silver_ore", "silver_block", "raw_silver_block", "artemis_silver_planks"]
for blk in cube_blocks:
    write_json(f"{MODELS_BLOCK}/{blk}.json", {
        "parent": "minecraft:block/cube_all",
        "textures": { "all": f"obsidium:block/{blk}" }
    })
    write_json(f"{BLOCKSTATES}/{blk}.json", {
        "variants": { "": { "model": f"obsidium:block/{blk}" } }
    })
    write_json(f"{MODELS_ITEM}/{blk}.json", {
        "parent": f"obsidium:block/{blk}"
    })
    write_json(f"{LOOT_BLOCKS}/{blk}.json", {
        "type": "minecraft:block",
        "pools": [{
            "rolls": 1.0,
            "bonus_rolls": 0.0,
            "entries": [{ "type": "minecraft:item", "name": f"obsidium:{blk}" }],
            "conditions": [{ "condition": "minecraft:survives_explosion" }]
        }]
    })

# Silver Ore & Deepslate Silver Ore drop Raw Silver
for ore in ["silver_ore", "deepslate_silver_ore"]:
    write_json(f"{LOOT_BLOCKS}/{ore}.json", {
        "type": "minecraft:block",
        "pools": [{
            "rolls": 1.0,
            "bonus_rolls": 0.0,
            "entries": [{
                "type": "minecraft:alternatives",
                "children": [
                    {
                        "type": "minecraft:item",
                        "name": f"obsidium:{ore}",
                        "conditions": [{
                            "condition": "minecraft:match_tool",
                            "predicate": { "predicates": { "minecraft:enchantments": [{ "enchantments": "minecraft:silk_touch", "levels": { "min": 1 } }] } }
                        }]
                    },
                    {
                        "type": "minecraft:item",
                        "name": "obsidium:raw_silver",
                        "functions": [{ "function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops" }]
                    }
                ]
            }],
            "conditions": [{ "condition": "minecraft:survives_explosion" }]
        }]
    })

# Artemis Silver Log
write_json(f"{MODELS_BLOCK}/artemis_silver_log.json", {
    "parent": "minecraft:block/cube_column",
    "textures": { "end": "obsidium:block/artemis_silver_log_top", "side": "obsidium:block/artemis_silver_log" }
})
write_json(f"{MODELS_BLOCK}/artemis_silver_log_horizontal.json", {
    "parent": "minecraft:block/cube_column_horizontal",
    "textures": { "end": "obsidium:block/artemis_silver_log_top", "side": "obsidium:block/artemis_silver_log" }
})
write_json(f"{BLOCKSTATES}/artemis_silver_log.json", {
    "variants": {
        "axis=y": { "model": "obsidium:block/artemis_silver_log" },
        "axis=z": { "model": "obsidium:block/artemis_silver_log_horizontal", "x": 90 },
        "axis=x": { "model": "obsidium:block/artemis_silver_log_horizontal", "x": 90, "y": 90 }
    }
})
write_json(f"{MODELS_ITEM}/artemis_silver_log.json", { "parent": "obsidium:block/artemis_silver_log" })
write_json(f"{LOOT_BLOCKS}/artemis_silver_log.json", {
    "type": "minecraft:block",
    "pools": [{ "rolls": 1.0, "entries": [{ "type": "minecraft:item", "name": "obsidium:artemis_silver_log" }], "conditions": [{ "condition": "minecraft:survives_explosion" }] }]
})

# Artemis Silver Leaves
write_json(f"{MODELS_BLOCK}/artemis_silver_leaves.json", {
    "parent": "minecraft:block/cube_all",
    "textures": { "all": "obsidium:block/artemis_silver_leaves" }
})
write_json(f"{BLOCKSTATES}/artemis_silver_leaves.json", {
    "variants": { "": { "model": "obsidium:block/artemis_silver_leaves" } }
})
write_json(f"{MODELS_ITEM}/artemis_silver_leaves.json", { "parent": "obsidium:block/artemis_silver_leaves" })
write_json(f"{LOOT_BLOCKS}/artemis_silver_leaves.json", {
    "type": "minecraft:block",
    "pools": [{
        "rolls": 1.0,
        "entries": [{
            "type": "minecraft:alternatives",
            "children": [
                {
                    "type": "minecraft:item",
                    "name": "obsidium:artemis_silver_leaves",
                    "conditions": [{ "condition": "minecraft:match_tool", "predicate": { "predicates": { "minecraft:enchantments": [{ "enchantments": "minecraft:silk_touch", "levels": { "min": 1 } }] } } }]
                },
                {
                    "type": "minecraft:item",
                    "name": "obsidium:artemis_silver_sapling",
                    "conditions": [{ "condition": "minecraft:random_chance", "chance": 0.05 }]
                }
            ]
        }]
    }]
})

# Artemis Silver Sapling
write_json(f"{MODELS_BLOCK}/artemis_silver_sapling.json", {
    "parent": "minecraft:block/cross",
    "textures": { "cross": "obsidium:block/artemis_silver_sapling" }
})
write_json(f"{BLOCKSTATES}/artemis_silver_sapling.json", {
    "variants": { "": { "model": "obsidium:block/artemis_silver_sapling" } }
})
write_json(f"{MODELS_ITEM}/artemis_silver_sapling.json", {
    "parent": "minecraft:item/generated",
    "textures": { "layer0": "obsidium:block/artemis_silver_sapling" }
})
write_json(f"{LOOT_BLOCKS}/artemis_silver_sapling.json", {
    "type": "minecraft:block",
    "pools": [{ "rolls": 1.0, "entries": [{ "type": "minecraft:item", "name": "obsidium:artemis_silver_sapling" }], "conditions": [{ "condition": "minecraft:survives_explosion" }] }]
})

# Artemis Silver Slab
write_json(f"{MODELS_BLOCK}/artemis_silver_slab.json", {
    "parent": "minecraft:block/slab",
    "textures": { "bottom": "obsidium:block/artemis_silver_planks", "top": "obsidium:block/artemis_silver_planks", "side": "obsidium:block/artemis_silver_planks" }
})
write_json(f"{MODELS_BLOCK}/artemis_silver_slab_top.json", {
    "parent": "minecraft:block/slab_top",
    "textures": { "bottom": "obsidium:block/artemis_silver_planks", "top": "obsidium:block/artemis_silver_planks", "side": "obsidium:block/artemis_silver_planks" }
})
write_json(f"{BLOCKSTATES}/artemis_silver_slab.json", {
    "variants": {
        "type=bottom": { "model": "obsidium:block/artemis_silver_slab" },
        "type=top": { "model": "obsidium:block/artemis_silver_slab_top" },
        "type=double": { "model": "obsidium:block/artemis_silver_planks" }
    }
})
write_json(f"{MODELS_ITEM}/artemis_silver_slab.json", { "parent": "obsidium:block/artemis_silver_slab" })
write_json(f"{LOOT_BLOCKS}/artemis_silver_slab.json", {
    "type": "minecraft:block",
    "pools": [{ "rolls": 1.0, "entries": [{ "type": "minecraft:item", "name": "obsidium:artemis_silver_slab" }], "conditions": [{ "condition": "minecraft:survives_explosion" }] }]
})

# Artemis Silver Stairs
write_json(f"{MODELS_BLOCK}/artemis_silver_stairs.json", {
    "parent": "minecraft:block/stairs",
    "textures": { "bottom": "obsidium:block/artemis_silver_planks", "top": "obsidium:block/artemis_silver_planks", "side": "obsidium:block/artemis_silver_planks" }
})
write_json(f"{MODELS_BLOCK}/artemis_silver_stairs_inner.json", {
    "parent": "minecraft:block/inner_stairs",
    "textures": { "bottom": "obsidium:block/artemis_silver_planks", "top": "obsidium:block/artemis_silver_planks", "side": "obsidium:block/artemis_silver_planks" }
})
write_json(f"{MODELS_BLOCK}/artemis_silver_stairs_outer.json", {
    "parent": "minecraft:block/outer_stairs",
    "textures": { "bottom": "obsidium:block/artemis_silver_planks", "top": "obsidium:block/artemis_silver_planks", "side": "obsidium:block/artemis_silver_planks" }
})
write_json(f"{BLOCKSTATES}/artemis_silver_stairs.json", {
    "variants": {
        "facing=east,half=bottom,shape=straight": { "model": "obsidium:block/artemis_silver_stairs" },
        "facing=west,half=bottom,shape=straight": { "model": "obsidium:block/artemis_silver_stairs", "y": 180, "uvlock": True },
        "facing=south,half=bottom,shape=straight": { "model": "obsidium:block/artemis_silver_stairs", "y": 90, "uvlock": True },
        "facing=north,half=bottom,shape=straight": { "model": "obsidium:block/artemis_silver_stairs", "y": 270, "uvlock": True },
        "facing=east,half=top,shape=straight": { "model": "obsidium:block/artemis_silver_stairs", "x": 180, "uvlock": True },
        "facing=west,half=top,shape=straight": { "model": "obsidium:block/artemis_silver_stairs", "x": 180, "y": 180, "uvlock": True },
        "facing=south,half=top,shape=straight": { "model": "obsidium:block/artemis_silver_stairs", "x": 180, "y": 90, "uvlock": True },
        "facing=north,half=top,shape=straight": { "model": "obsidium:block/artemis_silver_stairs", "x": 180, "y": 270, "uvlock": True }
    }
})
write_json(f"{MODELS_ITEM}/artemis_silver_stairs.json", { "parent": "obsidium:block/artemis_silver_stairs" })
write_json(f"{LOOT_BLOCKS}/artemis_silver_stairs.json", {
    "type": "minecraft:block",
    "pools": [{ "rolls": 1.0, "entries": [{ "type": "minecraft:item", "name": "obsidium:artemis_silver_stairs" }], "conditions": [{ "condition": "minecraft:survives_explosion" }] }]
})

# Artemis Silver Fence & Gate
write_json(f"{MODELS_BLOCK}/artemis_silver_fence_post.json", { "parent": "minecraft:block/fence_post", "textures": { "texture": "obsidium:block/artemis_silver_planks" } })
write_json(f"{MODELS_BLOCK}/artemis_silver_fence_side.json", { "parent": "minecraft:block/fence_side", "textures": { "texture": "obsidium:block/artemis_silver_planks" } })
write_json(f"{MODELS_BLOCK}/artemis_silver_fence_inventory.json", { "parent": "minecraft:block/fence_inventory", "textures": { "texture": "obsidium:block/artemis_silver_planks" } })
write_json(f"{BLOCKSTATES}/artemis_silver_fence.json", {
    "multipart": [
        { "apply": { "model": "obsidium:block/artemis_silver_fence_post" } },
        { "when": { "north": "true" }, "apply": { "model": "obsidium:block/artemis_silver_fence_side", "uvlock": True } },
        { "when": { "east": "true" }, "apply": { "model": "obsidium:block/artemis_silver_fence_side", "y": 90, "uvlock": True } },
        { "when": { "south": "true" }, "apply": { "model": "obsidium:block/artemis_silver_fence_side", "y": 180, "uvlock": True } },
        { "when": { "west": "true" }, "apply": { "model": "obsidium:block/artemis_silver_fence_side", "y": 270, "uvlock": True } }
    ]
})
write_json(f"{MODELS_ITEM}/artemis_silver_fence.json", { "parent": "obsidium:block/artemis_silver_fence_inventory" })
write_json(f"{LOOT_BLOCKS}/artemis_silver_fence.json", {
    "type": "minecraft:block",
    "pools": [{ "rolls": 1.0, "entries": [{ "type": "minecraft:item", "name": "obsidium:artemis_silver_fence" }], "conditions": [{ "condition": "minecraft:survives_explosion" }] }]
})

write_json(f"{MODELS_BLOCK}/artemis_silver_fence_gate.json", { "parent": "minecraft:block/template_fence_gate", "textures": { "texture": "obsidium:block/artemis_silver_planks" } })
write_json(f"{MODELS_BLOCK}/artemis_silver_fence_gate_open.json", { "parent": "minecraft:block/template_fence_gate_open", "textures": { "texture": "obsidium:block/artemis_silver_planks" } })
write_json(f"{BLOCKSTATES}/artemis_silver_fence_gate.json", {
    "variants": {
        "facing=south,in_wall=false,open=false": { "model": "obsidium:block/artemis_silver_fence_gate", "uvlock": True },
        "facing=west,in_wall=false,open=false": { "model": "obsidium:block/artemis_silver_fence_gate", "y": 90, "uvlock": True },
        "facing=north,in_wall=false,open=false": { "model": "obsidium:block/artemis_silver_fence_gate", "y": 180, "uvlock": True },
        "facing=east,in_wall=false,open=false": { "model": "obsidium:block/artemis_silver_fence_gate", "y": 270, "uvlock": True },
        "facing=south,in_wall=false,open=true": { "model": "obsidium:block/artemis_silver_fence_gate_open", "uvlock": True },
        "facing=west,in_wall=false,open=true": { "model": "obsidium:block/artemis_silver_fence_gate_open", "y": 90, "uvlock": True },
        "facing=north,in_wall=false,open=true": { "model": "obsidium:block/artemis_silver_fence_gate_open", "y": 180, "uvlock": True },
        "facing=east,in_wall=false,open=true": { "model": "obsidium:block/artemis_silver_fence_gate_open", "y": 270, "uvlock": True }
    }
})
write_json(f"{MODELS_ITEM}/artemis_silver_fence_gate.json", { "parent": "obsidium:block/artemis_silver_fence_gate" })
write_json(f"{LOOT_BLOCKS}/artemis_silver_fence_gate.json", {
    "type": "minecraft:block",
    "pools": [{ "rolls": 1.0, "entries": [{ "type": "minecraft:item", "name": "obsidium:artemis_silver_fence_gate" }], "conditions": [{ "condition": "minecraft:survives_explosion" }] }]
})

# Volcanite Modular Chest
write_json(f"{MODELS_BLOCK}/volcanite_modular_chest.json", {
    "parent": "minecraft:block/cube",
    "textures": {
        "particle": "obsidium:block/volcanite_modular_chest_front",
        "north": "obsidium:block/volcanite_modular_chest_front",
        "south": "obsidium:block/volcanite_modular_chest_side",
        "east": "obsidium:block/volcanite_modular_chest_side",
        "west": "obsidium:block/volcanite_modular_chest_side",
        "up": "obsidium:block/volcanite_modular_chest_top",
        "down": "obsidium:block/volcanite_modular_chest_top"
    }
})
write_json(f"{BLOCKSTATES}/volcanite_modular_chest.json", {
    "variants": {
        "facing=north": { "model": "obsidium:block/volcanite_modular_chest" },
        "facing=south": { "model": "obsidium:block/volcanite_modular_chest", "y": 180 },
        "facing=west": { "model": "obsidium:block/volcanite_modular_chest", "y": 270 },
        "facing=east": { "model": "obsidium:block/volcanite_modular_chest", "y": 90 }
    }
})
write_json(f"{MODELS_ITEM}/volcanite_modular_chest.json", { "parent": "obsidium:block/volcanite_modular_chest" })
write_json(f"{LOOT_BLOCKS}/volcanite_modular_chest.json", {
    "type": "minecraft:block",
    "pools": [{ "rolls": 1.0, "entries": [{ "type": "minecraft:item", "name": "obsidium:volcanite_modular_chest" }], "conditions": [{ "condition": "minecraft:survives_explosion" }] }]
})

# Silver Crop
variants = {}
for s in range(8):
    write_json(f"{MODELS_BLOCK}/silver_crop_stage{s}.json", {
        "parent": "minecraft:block/crop",
        "textures": { "crop": f"obsidium:block/silver_crop_stage{s}" }
    })
    variants[f"age={s}"] = { "model": f"obsidium:block/silver_crop_stage{s}" }
write_json(f"{BLOCKSTATES}/silver_crop.json", { "variants": variants })
write_json(f"{LOOT_BLOCKS}/silver_crop.json", {
    "type": "minecraft:block",
    "pools": [
        {
            "rolls": 1.0,
            "entries": [{ "type": "minecraft:item", "name": "obsidium:silver_petal" }],
            "conditions": [{ "condition": "minecraft:block_state_property", "block": "obsidium:silver_crop", "properties": { "age": "7" } }]
        },
        {
            "rolls": 1.0,
            "entries": [{ "type": "minecraft:item", "name": "obsidium:silver_seed" }],
            "conditions": [{ "condition": "minecraft:survives_explosion" }]
        }
    ]
})

print("Models & Blockstates created!")

# ==========================================
# 3. RECIPES
# ==========================================
print("Generating Recipes...")

# Stack Upgrade (Requires Netherite Scrap!)
write_json(f"{RECIPES}/volcanite_stack_upgrade.json", {
    "type": "minecraft:crafting_shaped",
    "category": "misc",
    "pattern": [
        " G ",
        "SVS",
        " R "
    ],
    "key": {
        "G": { "item": "minecraft:gold_ingot" },
        "S": { "item": "minecraft:netherite_scrap" },
        "V": { "item": "obsidium:volcanite_ingot" },
        "R": { "item": "minecraft:redstone" }
    },
    "result": { "id": "obsidium:volcanite_stack_upgrade", "count": 1 }
})

# Scrap Reforging Recipe
write_json(f"{RECIPES}/netherite_ingot_from_scrap_reforging.json", {
    "type": "minecraft:crafting_shaped",
    "category": "misc",
    "pattern": [
        "SG",
        "VG"
    ],
    "key": {
        "S": { "item": "minecraft:netherite_scrap" },
        "G": { "item": "minecraft:gold_ingot" },
        "V": { "item": "obsidium:volcanite_ingot" }
    },
    "result": { "id": "minecraft:netherite_ingot", "count": 1 }
})

# Upgrades
write_json(f"{RECIPES}/volcanite_vacuum_upgrade.json", {
    "type": "minecraft:crafting_shaped",
    "category": "misc",
    "pattern": [
        " E ",
        "HVH",
        " E "
    ],
    "key": {
        "E": { "item": "minecraft:ender_pearl" },
        "H": { "item": "minecraft:hopper" },
        "V": { "item": "obsidium:volcanite_ingot" }
    },
    "result": { "id": "obsidium:volcanite_vacuum_upgrade", "count": 1 }
})

write_json(f"{RECIPES}/volcanite_compactor_upgrade.json", {
    "type": "minecraft:crafting_shaped",
    "category": "misc",
    "pattern": [
        " I ",
        "PVP",
        " I "
    ],
    "key": {
        "I": { "item": "minecraft:iron_block" },
        "P": { "item": "minecraft:piston" },
        "V": { "item": "obsidium:volcanite_ingot" }
    },
    "result": { "id": "obsidium:volcanite_compactor_upgrade", "count": 1 }
})

write_json(f"{RECIPES}/volcanite_void_upgrade.json", {
    "type": "minecraft:crafting_shaped",
    "category": "misc",
    "pattern": [
        " O ",
        "LVR",
        " O "
    ],
    "key": {
        "O": { "item": "minecraft:obsidian" },
        "L": { "item": "minecraft:lava_bucket" },
        "R": { "item": "minecraft:redstone" },
        "V": { "item": "obsidium:volcanite_ingot" }
    },
    "result": { "id": "obsidium:volcanite_void_upgrade", "count": 1 }
})

# Volcanite Modular Chest
write_json(f"{RECIPES}/volcanite_modular_chest.json", {
    "type": "minecraft:crafting_shaped",
    "category": "misc",
    "pattern": [
        "VOV",
        "OCO",
        "VOV"
    ],
    "key": {
        "V": { "item": "obsidium:volcanite_ingot" },
        "O": { "item": "minecraft:obsidian" },
        "C": { "item": "obsidium:volcanite_chest" }
    },
    "result": { "id": "obsidium:volcanite_modular_chest", "count": 1 }
})

# Silver Ingot <-> Block
write_json(f"{RECIPES}/silver_block.json", {
    "type": "minecraft:crafting_shaped",
    "category": "building_blocks",
    "pattern": ["###", "###", "###"],
    "key": { "#": { "item": "obsidium:silver_ingot" } },
    "result": { "id": "obsidium:silver_block", "count": 1 }
})
write_json(f"{RECIPES}/silver_ingots_from_block.json", {
    "type": "minecraft:crafting_shapeless",
    "category": "misc",
    "ingredients": [{ "item": "obsidium:silver_block" }],
    "result": { "id": "obsidium:silver_ingot", "count": 9 }
})

# Silver Ingot <-> Nuggets
write_json(f"{RECIPES}/silver_ingot_from_nuggets.json", {
    "type": "minecraft:crafting_shaped",
    "category": "misc",
    "pattern": ["###", "###", "###"],
    "key": { "#": { "item": "obsidium:silver_nugget" } },
    "result": { "id": "obsidium:silver_ingot", "count": 1 }
})
write_json(f"{RECIPES}/silver_nuggets_from_ingot.json", {
    "type": "minecraft:crafting_shapeless",
    "category": "misc",
    "ingredients": [{ "item": "obsidium:silver_ingot" }],
    "result": { "id": "obsidium:silver_nugget", "count": 9 }
})

# Raw Silver <-> Raw Silver Block
write_json(f"{RECIPES}/raw_silver_block.json", {
    "type": "minecraft:crafting_shaped",
    "category": "building_blocks",
    "pattern": ["###", "###", "###"],
    "key": { "#": { "item": "obsidium:raw_silver" } },
    "result": { "id": "obsidium:raw_silver_block", "count": 1 }
})
write_json(f"{RECIPES}/raw_silver_from_block.json", {
    "type": "minecraft:crafting_shapeless",
    "category": "misc",
    "ingredients": [{ "item": "obsidium:raw_silver_block" }],
    "result": { "id": "obsidium:raw_silver", "count": 9 }
})

# Smelting / Blasting Silver
write_json(f"{RECIPES}/silver_ingot_from_smelting_raw.json", {
    "type": "minecraft:smelting",
    "category": "misc",
    "cookingtime": 200,
    "experience": 0.7,
    "ingredient": { "item": "obsidium:raw_silver" },
    "result": { "id": "obsidium:silver_ingot" }
})
write_json(f"{RECIPES}/silver_ingot_from_blasting_raw.json", {
    "type": "minecraft:blasting",
    "category": "misc",
    "cookingtime": 100,
    "experience": 0.7,
    "ingredient": { "item": "obsidium:raw_silver" },
    "result": { "id": "obsidium:silver_ingot" }
})
write_json(f"{RECIPES}/silver_ingot_from_smelting_ore.json", {
    "type": "minecraft:smelting",
    "category": "misc",
    "cookingtime": 200,
    "experience": 0.7,
    "ingredient": [{ "item": "obsidium:silver_ore" }, { "item": "obsidium:deepslate_silver_ore" }],
    "result": { "id": "obsidium:silver_ingot" }
})
write_json(f"{RECIPES}/silver_ingot_from_blasting_ore.json", {
    "type": "minecraft:blasting",
    "category": "misc",
    "cookingtime": 100,
    "experience": 0.7,
    "ingredient": [{ "item": "obsidium:silver_ore" }, { "item": "obsidium:deepslate_silver_ore" }],
    "result": { "id": "obsidium:silver_ingot" }
})

# Petal & Seed Recipes
write_json(f"{RECIPES}/silver_ingot_from_petals.json", {
    "type": "minecraft:crafting_shaped",
    "category": "misc",
    "pattern": ["##", "##"],
    "key": { "#": { "item": "obsidium:silver_petal" } },
    "result": { "id": "obsidium:silver_ingot", "count": 1 }
})
write_json(f"{RECIPES}/silver_seed_from_petal.json", {
    "type": "minecraft:crafting_shapeless",
    "category": "misc",
    "ingredients": [{ "item": "obsidium:silver_petal" }, { "item": "minecraft:wheat_seeds" }],
    "result": { "id": "obsidium:silver_seed", "count": 1 }
})

# Artemis Silver Tree Woodcraft
write_json(f"{RECIPES}/artemis_silver_planks.json", {
    "type": "minecraft:crafting_shapeless",
    "category": "building_blocks",
    "ingredients": [{ "item": "obsidium:artemis_silver_log" }],
    "result": { "id": "obsidium:artemis_silver_planks", "count": 4 }
})
write_json(f"{RECIPES}/artemis_silver_slab.json", {
    "type": "minecraft:crafting_shaped",
    "category": "building_blocks",
    "pattern": ["###"],
    "key": { "#": { "item": "obsidium:artemis_silver_planks" } },
    "result": { "id": "obsidium:artemis_silver_slab", "count": 6 }
})
write_json(f"{RECIPES}/artemis_silver_stairs.json", {
    "type": "minecraft:crafting_shaped",
    "category": "building_blocks",
    "pattern": ["#  ", "## ", "###"],
    "key": { "#": { "item": "obsidium:artemis_silver_planks" } },
    "result": { "id": "obsidium:artemis_silver_stairs", "count": 4 }
})
write_json(f"{RECIPES}/artemis_silver_fence.json", {
    "type": "minecraft:crafting_shaped",
    "category": "building_blocks",
    "pattern": ["#S#", "#S#"],
    "key": { "#": { "item": "obsidium:artemis_silver_planks" }, "S": { "item": "minecraft:stick" } },
    "result": { "id": "obsidium:artemis_silver_fence", "count": 3 }
})
write_json(f"{RECIPES}/artemis_silver_fence_gate.json", {
    "type": "minecraft:crafting_shaped",
    "category": "building_blocks",
    "pattern": ["S#S", "S#S"],
    "key": { "#": { "item": "obsidium:artemis_silver_planks" }, "S": { "item": "minecraft:stick" } },
    "result": { "id": "obsidium:artemis_silver_fence_gate", "count": 1 }
})
write_json(f"{RECIPES}/artemis_silver_log_to_charcoal.json", {
    "type": "minecraft:smelting",
    "category": "misc",
    "cookingtime": 200,
    "experience": 0.15,
    "ingredient": { "item": "obsidium:artemis_silver_log" },
    "result": { "id": "minecraft:charcoal" }
})

print("Recipes created!")

# ==========================================
# 4. TAGS
# ==========================================
print("Updating Tags...")

def append_tag(path, new_values):
    data = {"replace": False, "values": []}
    if os.path.exists(path):
        with open(path, 'r', encoding='utf-8') as f:
            data = json.load(f)
    for v in new_values:
        if v not in data["values"]:
            data["values"].append(v)
    data["values"].sort()
    write_json(path, data)

# Logs, Leaves, Saplings, Planks, Fences, Slabs, Gates
append_tag("src/main/resources/data/minecraft/tags/block/logs.json", ["obsidium:artemis_silver_log"])
append_tag("src/main/resources/data/minecraft/tags/item/logs.json", ["obsidium:artemis_silver_log"])
append_tag("src/main/resources/data/minecraft/tags/block/logs_that_burn.json", ["obsidium:artemis_silver_log"])
append_tag("src/main/resources/data/minecraft/tags/item/logs_that_burn.json", ["obsidium:artemis_silver_log"])
append_tag("src/main/resources/data/minecraft/tags/block/leaves.json", ["obsidium:artemis_silver_leaves"])
append_tag("src/main/resources/data/minecraft/tags/block/saplings.json", ["obsidium:artemis_silver_sapling"])
append_tag("src/main/resources/data/minecraft/tags/item/saplings.json", ["obsidium:artemis_silver_sapling"])
append_tag("src/main/resources/data/minecraft/tags/block/planks.json", ["obsidium:artemis_silver_planks"])
append_tag("src/main/resources/data/minecraft/tags/item/planks.json", ["obsidium:artemis_silver_planks"])
append_tag("src/main/resources/data/minecraft/tags/block/wooden_fences.json", ["obsidium:artemis_silver_fence"])
append_tag("src/main/resources/data/minecraft/tags/item/wooden_fences.json", ["obsidium:artemis_silver_fence"])
append_tag("src/main/resources/data/minecraft/tags/block/wooden_slabs.json", ["obsidium:artemis_silver_slab"])
append_tag("src/main/resources/data/minecraft/tags/item/wooden_slabs.json", ["obsidium:artemis_silver_slab"])
append_tag("src/main/resources/data/minecraft/tags/block/fence_gates.json", ["obsidium:artemis_silver_fence_gate"])
append_tag("src/main/resources/data/minecraft/tags/item/fence_gates.json", ["obsidium:artemis_silver_fence_gate"])

# Mineable
append_tag("src/main/resources/data/minecraft/tags/block/mineable/pickaxe.json", [
    "obsidium:silver_ore", "obsidium:deepslate_silver_ore", "obsidium:raw_silver_block", "obsidium:silver_block", "obsidium:volcanite_modular_chest"
])
append_tag("src/main/resources/data/minecraft/tags/block/mineable/axe.json", [
    "obsidium:artemis_silver_log", "obsidium:artemis_silver_planks", "obsidium:artemis_silver_slab",
    "obsidium:artemis_silver_stairs", "obsidium:artemis_silver_fence", "obsidium:artemis_silver_fence_gate"
])
append_tag("src/main/resources/data/minecraft/tags/block/crops.json", ["obsidium:silver_crop"])
append_tag("src/main/resources/data/minecraft/tags/item/crops.json", ["obsidium:silver_petal"])

# Common / Convention tags (c:ingots/silver, etc.)
append_tag("src/main/resources/data/c/tags/item/ingots.json", ["obsidium:silver_ingot"])
append_tag("src/main/resources/data/c/tags/item/nuggets.json", ["obsidium:silver_nugget"])
append_tag("src/main/resources/data/c/tags/item/dusts.json", ["obsidium:silver_dust"])
append_tag("src/main/resources/data/c/tags/item/raw_materials.json", ["obsidium:raw_silver"])
append_tag("src/main/resources/data/c/tags/item/ores.json", ["obsidium:silver_ore", "obsidium:deepslate_silver_ore"])
append_tag("src/main/resources/data/c/tags/block/ores.json", ["obsidium:silver_ore", "obsidium:deepslate_silver_ore"])

# Biogas Leaves Tag
append_tag("src/main/resources/data/obsidium/tags/item/biogas_leaves.json", ["obsidium:artemis_silver_leaves"])

# ==========================================
# 5. TRANSLATIONS (en_us.json)
# ==========================================
print("Updating Translations...")
lang_file = "src/main/resources/assets/obsidium/lang/en_us.json"
with open(lang_file, 'r', encoding='utf-8') as f:
    lang = json.load(f)

new_entries = {
    "item.obsidium.volcanite_stack_upgrade": "Volcanite Stack Upgrade",
    "item.obsidium.volcanite_vacuum_upgrade": "Volcanite Vacuum Module",
    "item.obsidium.volcanite_compactor_upgrade": "Volcanite Compactor Module",
    "item.obsidium.volcanite_void_upgrade": "Volcanite Void Module",
    "block.obsidium.volcanite_modular_chest": "Volcanite Modular Chest",
    
    "block.obsidium.silver_ore": "Silver Ore",
    "block.obsidium.deepslate_silver_ore": "Deepslate Silver Ore",
    "block.obsidium.raw_silver_block": "Block of Raw Silver",
    "block.obsidium.silver_block": "Block of Silver",
    "item.obsidium.raw_silver": "Raw Silver",
    "item.obsidium.silver_ingot": "Silver Ingot",
    "item.obsidium.silver_nugget": "Silver Nugget",
    "item.obsidium.silver_dust": "Silver Dust",
    "block.obsidium.silver_crop": "Volcanic Silver Crop",
    "item.obsidium.silver_petal": "Silver Petal",
    "item.obsidium.silver_seed": "Volcanic Silver Seed",

    "block.obsidium.artemis_silver_log": "Artemis Silver Log",
    "block.obsidium.artemis_silver_planks": "Artemis Silver Planks",
    "block.obsidium.artemis_silver_slab": "Artemis Silver Slab",
    "block.obsidium.artemis_silver_stairs": "Artemis Silver Stairs",
    "block.obsidium.artemis_silver_fence": "Artemis Silver Fence",
    "block.obsidium.artemis_silver_fence_gate": "Artemis Silver Fence Gate",
    "block.obsidium.artemis_silver_leaves": "Artemis Silver Leaves",
    "block.obsidium.artemis_silver_sapling": "Artemis Silver Sapling",
    "item.obsidium.artemis_silver_fruit": "Artemis Silver Fruit"
}

for k, v in new_entries.items():
    lang[k] = v

write_json(lang_file, lang)
print("All assets successfully generated!")

