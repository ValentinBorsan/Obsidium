"""Render the visible cuboid faces of Minecraft building-item models as SVG icons."""
import base64
from io import BytesIO
from PIL import Image, ImageDraw


def render_block_icon(elements, textures, read_asset):
    faces = []
    def project(p):
        x, y, z = p
        return ((x - z) * 2, -(x + z) - y * 2)
    for element in elements:
        if element.get('rotation'):
            raise ValueError('Rotated elements need a dedicated renderer')
        x0, y0, z0 = element['from']; x1, y1, z1 = element['to']
        planes = {
            'up': [(x0,y1,z0),(x1,y1,z0),(x1,y1,z1),(x0,y1,z1)],
            'north': [(x1,y1,z0),(x0,y1,z0),(x0,y0,z0),(x1,y0,z0)],
            'west': [(x0,y1,z0),(x0,y1,z1),(x0,y0,z1),(x0,y0,z0)]}
        for direction, points in planes.items():
            face = element.get('faces', {}).get(direction)
            if not face:
                continue
            texture = face['texture']; visited = set()
            while texture.startswith('#') and texture not in visited:
                visited.add(texture); texture = textures.get(texture[1:], '')
            if not texture or texture.startswith('#'):
                raise ValueError('Unresolved model texture')
            ns, name = texture.split(':',1) if ':' in texture else ('minecraft',texture)
            raw = read_asset(f'assets/{ns}/textures/{name}.png')
            if raw is None:
                raise ValueError('Missing face texture: ' + texture)
            uv = face.get('uv', [0,0,16,16])
            if face.get('rotation', 0):
                raise ValueError('Rotated UVs need a dedicated renderer')
            depth = sum(-x-z+y for x,y,z in points) / 4
            faces.append((depth, direction, [project(p) for p in points], uv, raw))
    if not faces:
        raise ValueError('No renderable model faces')
    points = [p for _,_,quad,_,_ in faces for p in quad]
    left=min(p[0] for p in points)-3; top=min(p[1] for p in points)-3
    width=max(p[0] for p in points)-left+3; height=max(p[1] for p in points)-top+3
    scale = 116 / max(width, height)
    tx = (128 - width * scale) / 2 - left * scale
    ty = (128 - height * scale) / 2 - top * scale
    canvas = Image.new('RGBA', (128,128))
    for _,direction,quad,uv,raw in sorted(faces, key=lambda face:face[0]):
        quad=[(x*scale+tx,y*scale+ty) for x,y in quad]
        a,b,_,d=quad; u0,v0,u1,v1=uv
        texture=Image.open(BytesIO(raw)).convert('RGBA')
        crop=texture.crop((round(u0*texture.width/16),round(v0*texture.height/16),round(u1*texture.width/16),round(v1*texture.height/16)))
        ex,ey=b[0]-a[0],b[1]-a[1]; fx,fy=d[0]-a[0],d[1]-a[1]
        determinant=ex*fy-fx*ey
        if not determinant: continue
        coeff=(fy/determinant*crop.width,-fx/determinant*crop.width,(-fy*a[0]+fx*a[1])/determinant*crop.width,
               -ey/determinant*crop.height,ex/determinant*crop.height,(ey*a[0]-ex*a[1])/determinant*crop.height)
        face=crop.transform((128,128),Image.Transform.AFFINE,coeff,resample=Image.Resampling.NEAREST)
        mask=Image.new('L',(128,128));ImageDraw.Draw(mask).polygon(quad,fill=255)
        factor={'up':1,'west':.82,'north':.68}[direction]
        r,g,b,alpha=face.split()
        face=Image.merge('RGBA',(r.point(lambda v:int(v*factor)),g.point(lambda v:int(v*factor)),b.point(lambda v:int(v*factor)),alpha))
        face=Image.composite(face,Image.new('RGBA',(128,128)),mask)
        canvas.alpha_composite(face)
    encoded=BytesIO();canvas.save(encoded,format='PNG')
    png=base64.b64encode(encoded.getvalue()).decode()
    return f'<svg xmlns="http://www.w3.org/2000/svg" width="128" height="128" viewBox="0 0 128 128" style="image-rendering:pixelated"><image width="128" height="128" href="data:image/png;base64,{png}"/></svg>\n'
