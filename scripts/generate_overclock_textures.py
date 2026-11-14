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

out_dir_item = "/Users/valentinborsan/DEV/ObsidianMod/src/main/resources/assets/obsidium/textures/item"
os.makedirs(out_dir_item, exist_ok=True)

tiers = [
    ("volcanite_overclock_upgrade.png", (245, 175, 40), (255, 235, 130), (160, 95, 20)),          # Gold / Orange
    ("advanced_volcanite_overclock_upgrade.png", (40, 205, 255), (160, 245, 255), (15, 110, 180)), # Cyan / Azure
    ("supreme_volcanite_overclock_upgrade.png", (210, 60, 255), (255, 180, 255), (120, 20, 180))   # Astral Violet
]

for filename, primary, highlight, shadow in tiers:
    data = bytearray(16 * 16 * 4)
    for y in range(16):
        for x in range(16):
            idx = (y * 16 + x) * 4
            dx = x - 7.5
            dy = y - 7.5
            dist = (dx*dx + dy*dy) ** 0.5

            # Circular Overdrive Processor Core
            if dist > 6.8:
                continue
            elif dist > 5.5:
                # Dark obsidian housing ring
                r, g, b = 38, 30, 44
            elif dist > 4.2:
                # Metallic conduction rim
                r, g, b = shadow
            elif dist > 2.2:
                # Glowing energy coil
                if (x + y) % 2 == 0:
                    r, g, b = primary
                else:
                    r, g, b = highlight
            else:
                # Super-heated singularity core
                if dist < 1.1:
                    r, g, b = 255, 255, 255
                else:
                    r, g, b = highlight

            # 4 Corner mounting teeth
            if (x in (2, 13) and y in (2, 13)):
                r, g, b = 70, 60, 75

            data[idx:idx+4] = [r, g, b, 255]

    with open(os.path.join(out_dir_item, filename), "wb") as f:
        f.write(create_png(16, 16, data))

print("Overdrive upgrade textures generated successfully!")

