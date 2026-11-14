import zlib, struct, os

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

block_dir = "src/main/resources/assets/obsidium/textures/block"
item_dir = "src/main/resources/assets/obsidium/textures/item"

def clamp(v): return max(0, min(255, int(v)))

# --- BARREL TEXTURES ---
def make_barrel(tier, side_type):
    # Colors per tier
    if tier == "basic":
        # Volcanic ironwood
        wood_base = (45, 28, 22, 255)
        wood_alt = (38, 22, 18, 255)
        band_col = (75, 45, 25, 255)
        trim_col = (180, 95, 30, 255)
        dark_inner = (15, 10, 14, 255)
    elif tier == "advanced":
        # Reinforced cyan volcanite
        wood_base = (30, 36, 48, 255)
        wood_alt = (22, 28, 38, 255)
        band_col = (0, 140, 180, 255)
        trim_col = (0, 210, 255, 255)
        dark_inner = (10, 15, 22, 255)
    else: # supreme
        # Netherite obsidian purple
        wood_base = (35, 18, 38, 255)
        wood_alt = (25, 12, 28, 255)
        band_col = (160, 30, 110, 255)
        trim_col = (255, 60, 180, 255)
        dark_inner = (8, 4, 12, 255)

    pixels = []
    for y in range(16):
        row = []
        for x in range(16):
            if side_type == "front":
                # Front face has an open window in the center (x: 2..13, y: 2..13)
                if 2 <= x <= 13 and 2 <= y <= 13:
                    # Recessed dark showcase
                    if x == 2 or x == 13 or y == 2 or y == 13:
                        c = dark_inner
                    elif x == 3 or y == 3:
                        c = (dark_inner[0] + 5, dark_inner[1] + 5, dark_inner[2] + 7, 255)
                    else:
                        c = (dark_inner[0] - 2, dark_inner[1] - 2, dark_inner[2] - 2, 255)
                elif x in (0, 15) or y in (0, 15):
                    # Outer trim
                    if (x + y) % 3 == 0:
                        c = trim_col
                    else:
                        c = band_col
                else:
                    c = wood_base if (x + y) % 2 == 0 else wood_alt
            elif side_type == "top" or side_type == "bottom":
                # Circular or cross banded lid
                dist_center = max(abs(x - 7.5), abs(y - 7.5))
                if x in (0, 15) or y in (0, 15):
                    c = trim_col if (x == y or x + y == 15) else band_col
                elif 5.5 <= dist_center <= 6.5:
                    c = band_col
                elif 2.0 <= dist_center <= 3.5:
                    c = trim_col if side_type == "top" else band_col
                else:
                    c = wood_base if (x * 3 + y) % 2 == 0 else wood_alt
            else: # side
                # Planks with metal bands at top (y: 1..2) and bottom (y: 13..14)
                if y in (0, 15) or x in (0, 15):
                    c = band_col
                elif y in (1, 2, 13, 14):
                    c = trim_col if x % 4 == 0 else band_col
                else:
                    # Vertical plank grooves
                    if x in (4, 8, 12):
                        c = (wood_alt[0] - 10, wood_alt[1] - 10, wood_alt[2] - 10, 255)
                    else:
                        c = wood_base if (y + x // 4) % 2 == 0 else wood_alt
            row.append(c)
        pixels.append(row)
    return pixels

# Generate blocks
for t in ["basic", "advanced", "supreme"]:
    for s in ["front", "side", "top", "bottom"]:
        p = make_barrel(t, s)
        write_png(f"{block_dir}/{t}_volcanite_item_barrel_{s}.png", 16, 16, p)

# --- UPGRADE ITEMS ---
def make_upgrade(tier):
    if tier == "basic":
        base_plate = (70, 40, 25, 255)
        border = (220, 120, 40, 255)
        gem = (255, 170, 50, 255)
        gem_hl = (255, 220, 130, 255)
    elif tier == "advanced":
        base_plate = (25, 45, 60, 255)
        border = (0, 190, 230, 255)
        gem = (0, 230, 255, 255)
        gem_hl = (180, 250, 255, 255)
    elif tier == "supreme":
        base_plate = (45, 15, 50, 255)
        border = (230, 50, 170, 255)
        gem = (255, 80, 210, 255)
        gem_hl = (255, 190, 240, 255)
    else: # void
        base_plate = (20, 15, 25, 255)
        border = (180, 30, 50, 255)
        gem = (80, 0, 30, 255)
        gem_hl = (230, 40, 70, 255)

    pixels = []
    for y in range(16):
        row = []
        for x in range(16):
            if x in (0, 15) or y in (0, 15) or (x in (1, 14) and y in (1, 14)):
                # Transparent outside frame
                c = (0, 0, 0, 0)
            elif x in (1, 14) or y in (1, 14):
                c = border
            elif x in (2, 13) or y in (2, 13):
                c = (base_plate[0] - 8, base_plate[1] - 8, base_plate[2] - 8, 255)
            elif 5 <= x <= 10 and 5 <= y <= 10:
                # Center gem / insignia
                if x == 7 and y == 7:
                    c = gem_hl
                elif x in (6, 7, 8) and y in (6, 7, 8):
                    c = gem
                else:
                    c = border
            else:
                c = base_plate if (x + y) % 2 == 0 else (base_plate[0] + 5, base_plate[1] + 5, base_plate[2] + 5, 255)
            row.append(c)
        pixels.append(row)
    return pixels

write_png(f"{item_dir}/volcanite_basic_barrel_upgrade.png", 16, 16, make_upgrade("basic"))
write_png(f"{item_dir}/volcanite_advanced_barrel_upgrade.png", 16, 16, make_upgrade("advanced"))
write_png(f"{item_dir}/volcanite_supreme_barrel_upgrade.png", 16, 16, make_upgrade("supreme"))
write_png(f"{item_dir}/volcanite_barrel_void_upgrade.png", 16, 16, make_upgrade("void"))

print("All textures generated successfully!")

