#!/usr/bin/env python3
"""Build the first wiki item category from checked-in registrations, models and recipes."""
import argparse
import json
import re
import shutil
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
DOCS = ROOT / 'docs'
MC_JAR = Path.home() / '.gradle/caches/fabric-loom/1.21/minecraft-client.jar'
EXTRA = ['volcanite', 'volcanic_ash', 'volcanic_alloy', 'supreme_volcanic_alloy',
         'astral_volcanic_catalyst', 'volcanite_binding', 'volcanic_plate',
         'volcanite_thermo_core', 'volcanite_flux_mechanism', 'cinder_core',
         'cinder_stalker_skull', 'tartarean_fang', 'singularity_hadron_alloy']


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--minecraft-jar', type=Path, default=MC_JAR, help='Local Minecraft 1.21 client JAR for vanilla names/textures')
    args = parser.parse_args()
    if not args.minecraft_jar.is_file():
        parser.error('Needs the Minecraft 1.21 client JAR; pass --minecraft-jar. No generated files were changed.')
    registered = set(re.findall(r'registerItem\("([^"]+)"', (ROOT / 'src/main/java/net/valentin/obsidium/ObsidiumMod.java').read_text()))
    selected = {name for name in registered if name.endswith(('_ingot', '_dust', '_nugget')) or name.startswith('raw_') and not name.endswith('_block')} | set(EXTRA)
    assert selected <= registered, selected - registered
    names = json.loads((RES / 'assets/obsidium/lang/en_us.json').read_text())
    vanilla = zipfile.ZipFile(args.minecraft_jar)
    if vanilla:
        names.update(json.loads(vanilla.read('assets/minecraft/lang/en_us.json')))
    cache = {}
    missing = []

    def read_asset(path):
        local = RES / path
        if local.is_file():
            return local.read_bytes()
        if vanilla:
            try:
                return vanilla.read(path)
            except KeyError:
                pass
        return None

    def label(identifier):
        namespace, name = identifier.split(':', 1)
        return names.get('item.' + namespace + '.' + name, names.get('block.' + namespace + '.' + name, name.replace('_', ' ').title()))

    def icon(identifier):
        namespace, name = identifier.split(':', 1)
        model_ref = identifier.replace(':', ':item/', 1)
        seen = set()
        textures = {}
        while model_ref and model_ref not in seen:
            seen.add(model_ref)
            ns, path = model_ref.split(':', 1) if ':' in model_ref else ('minecraft', model_ref)
            raw = read_asset(f'assets/{ns}/models/{path}.json')
            if not raw:
                break
            model = json.loads(raw)
            textures = {**model.get('textures', {}), **textures}
            model_ref = model.get('parent')
        texture = next((textures[k] for k in ['layer0', 'all', 'front', 'side', 'top', 'particle'] if k in textures), None)
        visited = set()
        while texture and texture.startswith('#') and texture not in visited:
            visited.add(texture)
            texture = textures.get(texture[1:])
        if not texture or texture.startswith('#'):
            missing.append(identifier)
            return None, 'missing'
        ns, path = texture.split(':', 1) if ':' in texture else ('minecraft', texture)
        raw = read_asset(f'assets/{ns}/textures/{path}.png')
        if not raw:
            missing.append(identifier)
            return None, 'missing'
        relative = f'assets/catalog-icons/{namespace}/{name}.png'
        target = DOCS / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(raw)
        return relative, 'block texture' if path.startswith('block/') else 'item texture'

    def item(identifier):
        if identifier not in cache:
            image, kind = icon(identifier)
            cache[identifier] = {'id': identifier, 'name': label(identifier), 'image': image, 'imageKind': kind}
        return cache[identifier]

    def ingredient(value):
        if isinstance(value, list):
            return {'alternatives': [ingredient(v) for v in value]}
        if 'item' in value:
            return {'item': item(value['item'])}
        if 'tag' in value:
            return {'tag': value['tag']}
        return {'unknown': value}

    recipes_by_output = {}
    for path in sorted((RES / 'data/obsidium/recipe').glob('*.json')):
        raw = json.loads(path.read_text())
        result = raw.get('result', {})
        output = result.get('id', result.get('item')) if isinstance(result, dict) else result
        if not output or output.split(':', 1)[-1] not in selected or not output.startswith('obsidium:'):
            continue
        recipe = {'id': 'obsidium:' + path.stem, 'type': raw['type'], 'output': item(output),
                  'count': result.get('count', 1) if isinstance(result, dict) else 1,
                  'source': 'src/main/resources/data/obsidium/recipe/' + path.name,
                  'json': 'assets/catalog-recipes/' + path.name}
        target = DOCS / recipe['json']
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(path, target)
        if 'pattern' in raw:
            recipe.update(pattern=raw['pattern'], key={k: ingredient(v) for k, v in raw['key'].items()})
        if 'ingredients' in raw:
            recipe['ingredients'] = [ingredient(v) for v in raw['ingredients']]
        if 'ingredient' in raw:
            recipe['ingredient'] = ingredient(raw['ingredient'])
        for field in ['cookingtime', 'experience']:
            if field in raw:
                recipe[field] = raw[field]
        if not any(k in raw for k in ['pattern', 'ingredient', 'ingredients']):
            recipe['raw'] = raw
        recipes_by_output.setdefault(output, []).append(recipe)

    routes = {}
    def route(name, kind, note, source, inputs=None, count=None):
        record = {'kind': kind, 'note': note, 'source': source}
        if count is not None:
            record['count'] = count
        if inputs:
            record['inputs'] = [item(i) for i in inputs]
        routes.setdefault(name, []).append(record)
    route('volcanite', 'Mining', 'Mine Volcanite Ore. The ore loot table supplies the natural resource.', 'src/main/resources/data/obsidium/loot_table/blocks/volcanite_ore.json')
    for metal in ['tin', 'lead', 'silver']:
        route('raw_' + metal, 'Mining', 'Mine the corresponding ore. Block-unpacking recipes below are recycling routes, not the original source.', 'src/main/resources/data/obsidium/loot_table/blocks/' + metal + '_ore.json')
    for metal in ['iron', 'gold', 'copper', 'tin', 'lead', 'silver', 'volcanite', 'pyric_bronze', 'feroplumb']:
        route(metal + '_dust', 'Volcanite Grinder', 'Grind one matching ingot into one dust.', 'src/main/java/net/valentin/obsidium/block/VolcaniteGrinderBlockEntity.java', ['minecraft:' + metal + '_ingot' if metal in ['iron', 'gold', 'copper'] else 'obsidium:' + metal + '_ingot'])
    for metal in ['iron', 'gold', 'copper', 'tin', 'lead', 'silver', 'volcanite']:
        namespace = 'minecraft' if metal in ['iron', 'gold', 'copper'] else 'obsidium'
        raw = 'volcanite' if metal == 'volcanite' else 'raw_' + metal
        route(metal + '_dust', 'Volcanite Grinder', 'Grind one raw resource into two dusts. Matching ores are also supported by the machine.', 'src/main/java/net/valentin/obsidium/block/VolcaniteGrinderBlockEntity.java', [namespace + ':' + raw], count=2)
    for name in ['cinder_core', 'cinder_stalker_skull']:
        route(name, 'Mob drop', 'Obtain from Cinder Stalkers; loot-table conditions and randomness apply.', 'src/main/resources/data/obsidium/loot_table/entities/cinder_stalker.json')
    route('tartarean_fang', 'Mob drop', 'Obtain from Tartarean Hounds; loot-table conditions and randomness apply.', 'src/main/resources/data/obsidium/loot_table/entities/tartarean_hound.json')
    route('singularity_hadron_alloy', 'Hadron Collider', 'A collision of 4 Supreme Volcanic Alloy and 4 Astral Volcanic Catalysts has a 10% success chance. This is not a guaranteed crafting recipe. Cryogenic bonus drops are another possible route; see the collider guide.', 'src/main/java/net/valentin/obsidium/block/OlympianHadronColliderBlockEntity.java', ['obsidium:supreme_volcanic_alloy', 'obsidium:astral_volcanic_catalyst'])
    descriptions = {
        'volcanite': 'The natural volcanic resource at the beginning of Obsidium metallurgy.',
        'volcanic_ash': 'A reusable volcanic ingredient for crafting and industrial recipes.',
        'volcanic_alloy': 'A crafted alloy used throughout the workshop and its machinery.',
        'supreme_volcanic_alloy': 'An advanced alloy whose crafting recipe includes a Nether Star.',
        'astral_volcanic_catalyst': 'An advanced catalyst used in rare-resource and industrial progression.',
        'volcanite_binding': 'A structural binding component for Obsidium crafting.',
        'volcanic_plate': 'A manufactured plate used as a crafting component.',
        'volcanite_thermo_core': 'A heat-themed component used by machinery and higher-tier materials.',
        'volcanite_flux_mechanism': 'A mechanical component used by automation and progression recipes.',
        'cinder_core': 'A hostile-mob resource used as a special crafting component.',
        'cinder_stalker_skull': 'A Cinder Stalker trophy with progression uses.',
        'tartarean_fang': 'A Tartarean Hound resource used in special crafting.',
        'singularity_hadron_alloy': 'A rare late-game collider product with probabilistic acquisition.'}
    entries = []
    for name in sorted(selected, key=lambda name: (label('obsidium:' + name).casefold(), name)):
        identifier = 'obsidium:' + name
        category = 'Dusts' if name.endswith('_dust') else 'Nuggets' if name.endswith('_nugget') else 'Metals & raw resources' if name.endswith('_ingot') or name.startswith('raw_') or name == 'volcanite' else 'Alloys & components'
        description = descriptions.get(name, 'A powdered processing material.' if name.endswith('_dust') else 'A small material unit used in conversion recipes.' if name.endswith('_nugget') else 'A metal resource for crafting and processing.')
        entry = {**item(identifier), 'group': category, 'description': description,
                 'recipes': recipes_by_output.get(identifier, []), 'acquisition': routes.get(name, [])}
        assert entry['image'], 'Missing item image: ' + identifier
        assert entry['recipes'] or entry['acquisition'], 'No documented acquisition: ' + identifier
        entries.append(entry)
    result = {'schema': 1, 'category': 'materials', 'title': 'Materials & Components',
              'note': 'First documented category. Images are copied from game textures; block ingredients use a representative block face. Recipes reflect the checked-in source and may be overridden by modpacks.',
              'scope': 'All registered ingots, dusts, nuggets and raw metal items (excluding storage blocks), plus the explicitly listed core components. Other wiki categories are added in later batches.',
              'count': len(entries), 'items': entries}
    output = DOCS / 'wiki-catalog/materials.json'
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n')
    report = {'category': 'materials', 'items': len(entries), 'recipe_variants': sum(len(e['recipes']) for e in entries), 'missing_ingredient_icons': sorted(set(missing)), 'ids': [e['id'] for e in entries]}
    (DOCS / 'wiki-catalog/coverage.json').write_text(json.dumps(report, indent=2) + '\n')
    if vanilla:
        vanilla.close()
    print(json.dumps({k: v for k, v in report.items() if k != 'ids'}))


if __name__ == '__main__':
    main()
