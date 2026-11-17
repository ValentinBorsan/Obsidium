#!/usr/bin/env python3
"""Build wiki item categories from checked-in registrations, models and recipes."""
import argparse
import json
import re
import shutil
import zipfile
from wiki_block_icons import render_block_icon
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
    machine_metadata = json.loads((ROOT / 'scripts/wiki-machines.json').read_text())
    transport_metadata = json.loads((ROOT / 'scripts/wiki-transport.json').read_text())
    storage_metadata = json.loads((ROOT / 'scripts/wiki-storage.json').read_text())
    botany_metadata = json.loads((ROOT / 'scripts/wiki-botany.json').read_text())
    energy_metadata = json.loads((ROOT / 'scripts/wiki-energy.json').read_text())
    equipment_metadata = json.loads((ROOT / 'scripts/wiki-equipment.json').read_text())
    building_metadata = json.loads((ROOT / 'scripts/wiki-building.json').read_text())
    mythology_metadata = json.loads((ROOT / 'scripts/wiki-mythology.json').read_text())
    supplies_metadata = json.loads((ROOT / 'scripts/wiki-supplies.json').read_text())
    materials = set(selected)
    blocks = set(re.findall(r'registerBlock\("([^"]+)"', (ROOT / 'src/main/java/net/valentin/obsidium/ObsidiumMod.java').read_text()))
    assert materials <= registered, materials - registered
    assert set(machine_metadata) <= blocks, set(machine_metadata) - blocks
    assert set(transport_metadata) <= blocks, set(transport_metadata) - blocks
    assert set(storage_metadata) <= registered, set(storage_metadata) - registered
    assert set(botany_metadata) <= registered, set(botany_metadata) - registered
    assert set(energy_metadata) <= registered, set(energy_metadata) - registered
    assert set(equipment_metadata) <= registered, set(equipment_metadata) - registered
    assert set(building_metadata) <= registered, set(building_metadata) - registered
    assert set(mythology_metadata) | set(supplies_metadata) <= registered
    selected |= set(mythology_metadata) | set(supplies_metadata) | set(building_metadata) | set(equipment_metadata) | set(energy_metadata) | set(machine_metadata) | set(transport_metadata) | set(storage_metadata) | set(botany_metadata)
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
        elements = None
        while model_ref and model_ref not in seen:
            seen.add(model_ref)
            ns, path = model_ref.split(':', 1) if ':' in model_ref else ('minecraft', model_ref)
            raw = read_asset(f'assets/{ns}/models/{path}.json')
            if not raw:
                break
            model = json.loads(raw)
            if elements is None and 'elements' in model:
                elements = model['elements']
            textures = {**model.get('textures', {}), **textures}
            model_ref = model.get('parent')
        if namespace == 'obsidium' and name.endswith(('_stairs', '_slab', '_fence', '_fence_gate')) and elements:
            relative = f'assets/catalog-icons/{namespace}/{name}.svg'
            target = DOCS / relative
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text(render_block_icon(elements, textures, read_asset))
            return relative, 'rendered block model'
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

    crafting_rules_source = 'src/main/java/net/valentin/obsidium/mixin/CraftingMenuMixin.java'
    crafting_rules = (ROOT / crafting_rules_source).read_text().split('private static boolean isEarlyGameRecipe(String path)')[1]
    early_prefixes = re.findall(r'path.startsWith\("([^"]+)"\)', crafting_rules)
    early_contains = re.findall(r'path.contains\("([^"]+)"\)', crafting_rules)
    early_exact = re.findall(r'path.equals\("([^"]+)"\)', crafting_rules)
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
        if raw['type'] in ['minecraft:crafting_shaped', 'minecraft:crafting_shapeless']:
            early = any(path.stem.startswith(v) for v in early_prefixes) or any(v in path.stem for v in early_contains) or path.stem in early_exact
            ingredients_raw = raw.get('key', raw.get('ingredients', []))
            def has_mod_item(value):
                if isinstance(value, dict):
                    return str(value.get('item', '')).startswith('obsidium:') or any(has_mod_item(v) for v in value.values())
                return isinstance(value, list) and any(has_mod_item(v) for v in value)
            blocked = not early
            recipe['station'] = 'Volcanite Worktable' if blocked and has_mod_item(ingredients_raw) else 'Crafting table / Volcanite Worktable'
            recipe['worktableRequired'] = blocked and has_mod_item(ingredients_raw)
            recipe['stationSource'] = crafting_rules_source
            if blocked:
                recipe['stationNote'] = 'The vanilla crafting table blocks this recipe when the grid contains an Obsidium item. Use the Volcanite Worktable for that case. Other crafting implementations may differ.'
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
    for category_id, selected, title, scope in [
        ('materials', materials, 'Materials & Components', 'Registered ingots, dusts, nuggets and raw metal items, plus core components. Storage blocks are covered separately.'),
        ('machines', set(machine_metadata), 'Machines & Generators', 'Processing machines, generators, industrial controllers, pumps, botanical automation and craftable multiblock components. Storage, transport, ritual blocks and hidden formed parts are covered separately.'),
        ('transport', set(transport_metadata), 'Pipes & Wires', 'All registered core item pipes, fluid pipes, storage-network pipes, energy wires and universal conduits. Upgrades and wireless infrastructure are covered separately.'),
        ('storage', set(storage_metadata), 'Storage & Items', 'Item and fluid storage, backpacks, disks, storage-network blocks, access components and storage upgrades. Processing barrels, energy storage, creative-only blocks and transport pipes are covered separately.'),
        ('botany', set(botany_metadata), 'Botany & Manuals', 'Resource crop seeds and petals, living-tree saplings and fruit, grafting tools, beekeeping, botanical automation and both in-game manuals. Decorative tree wood blocks are covered separately.'),
        ('energy', set(energy_metadata), 'Energy & Upgrades', 'Energy cells, cores, portable batteries, wireless pylons, charging, configuration tools and machine/storage/conduit upgrades. Generators and wires have dedicated categories; creative-only items are covered separately.'),
        ('equipment', set(equipment_metadata), 'Equipment & Exploration', 'Registered tool and armor families, mythic wearable/handheld equipment, exploration instruments and dielectric work tools. Ritual scrolls, coins and ritual structures are covered separately.'),
        ('building', set(building_metadata), 'Building Blocks', 'Registered tree logs, planks, leaves and wooden shapes, ore blocks, compact metal/raw-resource blocks, glass, cement and lighting. Machines and functional storage blocks have dedicated categories.'),
        ('mythology', set(mythology_metadata), 'Mythology & Rituals', 'Divine scrolls, ritual structures, ritual materials and Stygian recovery items. Mythic weapons and wearables are in Equipment.'),
        ('supplies', set(supplies_metadata), 'Fluids, Supplies & Utilities', 'Fluid containers, consumables, processing byproducts, fuels, teleporters and explicitly labeled Creative/testing items.')]:
        entries = []
        for name in sorted(selected, key=lambda name: (label('obsidium:' + name).casefold(), name)):
            identifier = 'obsidium:' + name
            category = 'Dusts' if name.endswith('_dust') else 'Nuggets' if name.endswith('_nugget') else 'Metals & raw resources' if name.endswith('_ingot') or name.startswith('raw_') or name == 'volcanite' else 'Alloys & components'
            description = descriptions.get(name, 'A powdered processing material.' if name.endswith('_dust') else 'A small material unit used in conversion recipes.' if name.endswith('_nugget') else 'A metal resource for crafting and processing.')
            metadata = machine_metadata if category_id == 'machines' else transport_metadata if category_id == 'transport' else storage_metadata if category_id == 'storage' else botany_metadata if category_id == 'botany' else energy_metadata if category_id == 'energy' else equipment_metadata if category_id == 'equipment' else building_metadata if category_id == 'building' else mythology_metadata if category_id == 'mythology' else supplies_metadata if category_id == 'supplies' else None
            if metadata is not None:
                category = metadata[name]['group']
                description = metadata[name]['description']
            entry = {**item(identifier), 'group': category, 'description': description,
                     'recipes': recipes_by_output.get(identifier, []), 'acquisition': routes.get(name, [])}
            if metadata is not None:
                entry['source'] = metadata[name]['source']
                entry['acquisition'] += metadata[name].get('acquisition', [])
            assert entry['image'], 'Missing item image: ' + identifier
            assert entry['recipes'] or entry['acquisition'], 'No documented acquisition: ' + identifier
            entries.append(entry)
        result = {'schema': 1, 'category': category_id, 'title': title,
                  'note': 'Images are copied from game textures; machine and block images use a representative face. Crafting recipes may be overridden by modpacks. Machine crafting recipes describe how to build the machine, not its processing recipes.',
                  'scope': scope, 'count': len(entries), 'items': entries}
        output = DOCS / f'wiki-catalog/{category_id}.json'
        output.parent.mkdir(parents=True, exist_ok=True)
        output.write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n')
        report = {'category': category_id, 'items': len(entries), 'recipe_variants': sum(len(e['recipes']) for e in entries), 'missing_ingredient_icons': sorted(set(missing)), 'ids': [e['id'] for e in entries]}
        report_name = 'coverage.json' if category_id == 'materials' else f'{category_id}-coverage.json'
        (DOCS / 'wiki-catalog' / report_name).write_text(json.dumps(report, indent=2) + '\n')
        print(json.dumps({k: v for k, v in report.items() if k != 'ids'}))
    covered = set()
    for category in ['materials','machines','transport','storage','botany','energy','equipment','building','mythology','supplies']:
        covered.update(e['id'].split(':',1)[1] for e in json.loads((DOCS / f'wiki-catalog/{category}.json').read_text())['items'])
    missing_items = sorted(registered - covered)
    assert not missing_items, missing_items
    (DOCS / 'wiki-catalog/completion.json').write_text(json.dumps({'scope':'Literal core item registrations in ObsidiumMod.java; aliases and dynamically registered content are not separate entries.', 'registered':len(registered), 'covered':len(registered & covered), 'missing':missing_items}, indent=2) + '\n')
    structures = json.loads((ROOT / 'scripts/wiki-multiblocks.json').read_text())
    for structure in structures['items']:
        structure['parts'] = {symbol: item(identifier) for symbol, identifier in structure['parts'].items()}
        for component in structure['parts'].values():
            assert component['image'], component['id']
    (DOCS / 'wiki-catalog/multiblocks.json').write_text(json.dumps(structures, ensure_ascii=False, indent=2) + '\n')
    if vanilla:
        vanilla.close()


if __name__ == '__main__':
    main()
