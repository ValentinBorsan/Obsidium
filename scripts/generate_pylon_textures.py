import zlib
import struct
import os

def create_png(width, height, rgba_data):
    def chunk(tag, data):
        return struct.pack('>I', len(data)) + tag + data + struct.pack('>I', zlib.crc32(tag + data) & 0xffffffff)
    
    header = b'\x89PNG\r\n\x1a\n'
    ihdr = chunk(b'IHDR', struct.pack('>IIBBBBB', width, height, 8, 6, 0, 0, 0))
    raw_lines = bytearray()
    for y in range(height):
        raw_lines.append(0)  # filter type 0
        raw_lines.extend(rgba_data[y*width*4 : (y+1)*width*4])
    idat = chunk(b'IDAT', zlib.compress(bytes(raw_lines), 9))
    iend = chunk(b'IEND', b'')
    return header + ihdr + idat + iend

out_dir_block = "/Users/valentinborsan/DEV/ObsidianMod/src/main/resources/assets/obsidium/textures/block"
out_dir_item = "/Users/valentinborsan/DEV/ObsidianMod/src/main/resources/assets/obsidium/textures/item"
os.makedirs(out_dir_block, exist_ok=True)
os.makedirs(out_dir_item, exist_ok=True)

# 1. PYLON BASE (16x16): Obsidian dark casing with volcanic gold trim
base = bytearray(16 * 16 * 4)
for y in range(16):
    for x in range(16):
        idx = (y * 16 + x) * 4
        noise = ((x * 11 + y * 13) % 7) - 3
        # Border
        if x in (0, 15) or y in (0, 15):
            r, g, b = 32 + noise, 28 + noise, 36 + noise
        elif x in (1, 14) or y in (1, 14):
            r, g, b = 190, 130, 45  # Gold accent rim
        elif 4 <= x <= 11 and 4 <= y <= 11:
            r, g, b = 25 + noise, 22 + noise, 30 + noise
        else:
            r, g, b = 48 + noise, 42 + noise, 52 + noise
        base[idx:idx+4] = [r, g, b, 255]

with open(os.path.join(out_dir_block, "astrape_energy_pylon_base.png"), "wb") as f:
    f.write(create_png(16, 16, base))

# 2. PYLON PILLAR (16x16): Metallic rod with electric cyan/gold coils
pillar = bytearray(16 * 16 * 4)
for y in range(16):
    for x in range(16):
        idx = (y * 16 + x) * 4
        noise = ((x * 5 + y * 7) % 5) - 2
        # Center core vs struts
        if 5 <= x <= 10:
            if y % 4 == 0:
                # Induction coil band
                r, g, b = 255, 205, 50
            elif y % 4 == 1:
                # Electric glow
                r, g, b = 70, 220, 255
            else:
                # Obsidian casing core
                r, g, b = 40 + noise, 35 + noise, 45 + noise
        else:
            # Outer support struts
            if x in (2, 3, 12, 13):
                r, g, b = 65 + noise, 55 + noise, 70 + noise
            else:
                r, g, b = 0, 0, 0
                pillar[idx:idx+4] = [0, 0, 0, 0]
                continue
        pillar[idx:idx+4] = [r, g, b, 255]

with open(os.path.join(out_dir_block, "astrape_energy_pylon_pillar.png"), "wb") as f:
    f.write(create_png(16, 16, pillar))

# 3. PYLON ITEM ICON (16x16)
item_tex = bytearray(16 * 16 * 4)
for y in range(16):
    for x in range(16):
        idx = (y * 16 + x) * 4
        # Draw pylon silhouette
        if y >= 13 and 3 <= x <= 12:
            # Base
            r, g, b = 180, 120, 40
        elif 6 <= y <= 12 and 6 <= x <= 9:
            # Shaft
            if y % 2 == 0:
                r, g, b = 255, 215, 60
            else:
                r, g, b = 60, 210, 255
        elif 1 <= y <= 5 and 5 <= x <= 10:
            # Levitating crystal
            dx = abs(x - 7.5)
            dy = abs(y - 3.0)
            if dx + dy <= 3.2:
                if dx + dy <= 1.2:
                    r, g, b = 255, 255, 255
                else:
                    r, g, b = 255, 190, 40
            else:
                continue
        elif 4 <= y <= 9 and (x in (4, 11)):
            # Prongs
            r, g, b = 140, 100, 35
        else:
            continue
        item_tex[idx:idx+4] = [r, g, b, 255]

with open(os.path.join(out_dir_item, "astrape_energy_pylon.png"), "wb") as f:
    f.write(create_png(16, 16, item_tex))

print("Pylon textures generated successfully!")

