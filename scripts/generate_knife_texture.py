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

out_dir = "/Users/valentinborsan/DEV/ObsidianMod/src/main/resources/assets/obsidium/textures/item"
os.makedirs(out_dir, exist_ok=True)

# 16x16 Demetrian Grafting Knife (curved botanical grafting sickle)
knife = bytearray(16 * 16 * 4)

def set_pixel(x, y, r, g, b, a=255):
    if 0 <= x < 16 and 0 <= y < 16:
        idx = (y * 16 + x) * 4
        knife[idx:idx+4] = [r, g, b, a]

# Handle (Bottom-left to center: diagonal from (2, 14) to (6, 9))
# Wooden grip with gold pommel & ferrule
set_pixel(1, 14, 215, 160, 45) # Gold pommel
set_pixel(2, 14, 255, 210, 70)
set_pixel(2, 13, 215, 160, 45)

set_pixel(3, 12, 110, 50, 30) # Dark mahogany/volcanic wood
set_pixel(4, 12, 145, 75, 45)
set_pixel(3, 11, 145, 75, 45)
set_pixel(4, 11, 110, 50, 30)

set_pixel(5, 10, 110, 50, 30)
set_pixel(6, 10, 145, 75, 45)
set_pixel(5, 9, 145, 75, 45)

# Brass / Gold Ferrule Guard
set_pixel(6, 9, 255, 215, 60)
set_pixel(7, 9, 215, 160, 45)
set_pixel(7, 8, 255, 215, 60)

# Curved Grafting Blade (Volcanic Steel + Emerald/Nature Green inlay)
# Spine (back of curved blade)
set_pixel(8, 7, 65, 55, 75)
set_pixel(9, 6, 85, 70, 95)
set_pixel(10, 5, 110, 90, 120)
set_pixel(11, 4, 140, 115, 150)
set_pixel(12, 3, 165, 140, 175)
set_pixel(13, 2, 200, 180, 210)
set_pixel(12, 1, 230, 220, 240)
set_pixel(11, 1, 255, 255, 255) # Sharp curved tip hook

# Blade Body (Sharpened cutting edge below spine)
set_pixel(7, 7, 200, 150, 50)  # Demeter gold accent
set_pixel(8, 6, 40, 180, 110)  # Nature green rune
set_pixel(9, 5, 220, 230, 240) # Silver steel
set_pixel(10, 4, 255, 255, 255)# Razor edge
set_pixel(11, 3, 220, 230, 240)
set_pixel(10, 3, 180, 190, 205)

set_pixel(8, 5, 240, 245, 255)
set_pixel(9, 4, 255, 255, 255)
set_pixel(10, 2, 255, 255, 255)

with open(os.path.join(out_dir, "demetrian_grafting_knife.png"), "wb") as f:
    f.write(create_png(16, 16, knife))

print("Grafting knife texture generated successfully!")

