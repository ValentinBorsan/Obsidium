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

out_dir = "/Users/valentinborsan/DEV/ObsidianMod/src/main/resources/assets/obsidium/textures/block"
os.makedirs(out_dir, exist_ok=True)

# 1. TOP UNLIT: Volcanite dark frame with copper/gold induction ring
top_unlit = bytearray(16 * 16 * 4)
for y in range(16):
    for x in range(16):
        idx = (y * 16 + x) * 4
        # Distance from center (7.5, 7.5)
        dx = x - 7.5
        dy = y - 7.5
        dist = (dx*dx + dy*dy) ** 0.5

        # Border rim
        if x in (0, 15) or y in (0, 15):
            r, g, b = 45, 38, 48
        elif x in (1, 14) or y in (1, 14):
            r, g, b = 70, 58, 75
        elif dist > 5.5:
            # Outer plate surface
            noise = ((x * 7 + y * 13) % 7) - 3
            r, g, b = 35 + noise, 30 + noise, 40 + noise
        elif 3.5 <= dist <= 5.5:
            # Induction copper coil
            if (x + y) % 2 == 0:
                r, g, b = 200, 115, 35
            else:
                r, g, b = 160, 85, 20
        elif 1.8 <= dist < 3.5:
            # Intermediate insulation plate
            r, g, b = 50, 42, 54
        else:
            # Core emitter node (unlit gold / dormant crystal)
            r, g, b = 180, 130, 40
            if dist < 0.9:
                r, g, b = 220, 170, 60
        
        top_unlit[idx:idx+4] = [r, g, b, 255]

with open(os.path.join(out_dir, "hephaestean_charging_plate_top.png"), "wb") as f:
    f.write(create_png(16, 16, top_unlit))

# 2. TOP LIT: Glowing energetic induction ring with electric sparks
top_lit = bytearray(16 * 16 * 4)
for y in range(16):
    for x in range(16):
        idx = (y * 16 + x) * 4
        dx = x - 7.5
        dy = y - 7.5
        dist = (dx*dx + dy*dy) ** 0.5

        if x in (0, 15) or y in (0, 15):
            r, g, b = 55, 45, 60
        elif x in (1, 14) or y in (1, 14):
            r, g, b = 110, 85, 115
        elif dist > 5.5:
            noise = ((x * 7 + y * 13) % 7) - 3
            r, g, b = 45 + noise, 38 + noise, 52 + noise
        elif 3.2 <= dist <= 5.8:
            # Vibrant glowing induction coil
            if (x + y) % 2 == 0:
                r, g, b = 255, 220, 90
            else:
                r, g, b = 255, 165, 40
        elif 1.8 <= dist < 3.2:
            r, g, b = 70, 55, 80
        else:
            # Radiant center node
            if dist < 1.0:
                r, g, b = 255, 255, 255
            elif dist < 1.8:
                r, g, b = 120, 240, 255
            else:
                r, g, b = 255, 210, 70

        top_lit[idx:idx+4] = [r, g, b, 255]

with open(os.path.join(out_dir, "hephaestean_charging_plate_top_lit.png"), "wb") as f:
    f.write(create_png(16, 16, top_lit))

# 3. SIDE: Dark metallic bevel with gold vents
side = bytearray(16 * 16 * 4)
for y in range(16):
    for x in range(16):
        idx = (y * 16 + x) * 4
        if y < 14:
            # Not part of the 2-pixel plate UV height, but texture can be full
            r, g, b = 40, 34, 45
        elif y == 14:
            # Top edge of side
            if x % 3 == 0:
                r, g, b = 200, 140, 45
            else:
                r, g, b = 80, 65, 85
        else:
            # Bottom edge of side
            r, g, b = 30, 25, 35
        side[idx:idx+4] = [r, g, b, 255]

with open(os.path.join(out_dir, "hephaestean_charging_plate_side.png"), "wb") as f:
    f.write(create_png(16, 16, side))

# 4. BOTTOM: Heavy volcanite reinforcement
bottom = bytearray(16 * 16 * 4)
for y in range(16):
    for x in range(16):
        idx = (y * 16 + x) * 4
        noise = ((x * 11 + y * 17) % 9) - 4
        if x in (0, 1, 14, 15) or y in (0, 1, 14, 15):
            r, g, b = 35 + noise, 30 + noise, 38 + noise
        else:
            r, g, b = 25 + noise, 20 + noise, 28 + noise
        bottom[idx:idx+4] = [r, g, b, 255]

with open(os.path.join(out_dir, "hephaestean_charging_plate_bottom.png"), "wb") as f:
    f.write(create_png(16, 16, bottom))

print("Charging plate textures generated successfully!")

