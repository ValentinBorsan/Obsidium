#!/usr/bin/env python3
"""Conservative recipe reachability from documented world, loot and machine sources.
Vanilla survival materials are assumed available; this is not a timed Survival playthrough.
"""
import json
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / 'src/main/resources/data/obsidium'
SOURCE = ROOT / 'src/main/java/net/valentin/obsidium'
ids = set(re.findall(r'registerItem\("([^"]+)"', (SOURCE / 'ObsidiumMod.java').read_text()))
known = {'obsidium:' + i for i in ids}
recipes = []
problems = []


def alternatives(value):
    if isinstance(value, list):
        return set().union(*(alternatives(v) for v in value))
    if 'item' in value:
        return {value['item']}
    tag = value.get('tag', '')
    if tag.startswith('minecraft:') or tag.startswith('c:'):
        # Native server suite separately checks actual tag expansion.
        return {'minecraft:tag/' + tag}
    if tag.startswith('obsidium:'):
        p = DATA / 'tags/item' / (tag.split(':')[1] + '.json')
        if p.exists():
            return set().union(*(alternatives({'tag':v[1:]} if v.startswith('#') else {'item':v})
                                 for v in json.loads(p.read_text())['values']))
    return set()


for p in sorted((DATA / 'recipe').glob('*.json')):
    d = json.loads(p.read_text())
    result = d.get('result', {})
    output = result if isinstance(result, str) else result.get('id')
    if not output:
        continue
    inputs = list(d.get('key', {}).values()) or d.get('ingredients') or [d.get('ingredient', {})]
    groups = [alternatives(x) for x in inputs]
    for group in groups:
        for item in group:
            if item.startswith('obsidium:') and item not in known:
                problems.append({'recipe': p.stem, 'unknown_item': item})
    recipes.append((output, groups, 'recipe/' + p.stem))

routes = []
def route(outputs, requirements, evidence):
    for item in outputs:
        item = item if ':' in item else 'obsidium:' + item
        if item in known:
            routes.append((item, [{v if ':' in v else 'obsidium:' + v} for v in requirements], evidence))

# Explicit non-crafting acquisition paths; nothing is inferred merely from a self-drop loot table.
route(['volcanite','volcanic_ash','tin_ore','deepslate_tin_ore','lead_ore','deepslate_lead_ore','silver_ore','deepslate_silver_ore','volcanite_ore'], [], 'worldgen/ModWorldgenAndFuels + VolcanicCalderaFeature')
for metal in ['tin','lead','silver']:
    route(['raw_'+metal], [metal+'_ore'], 'loot_table/blocks/'+metal+'_ore')
route(['cinder_core','cinder_stalker_skull','tartarean_fang'], [], 'loot_table/entities/cinder_stalker + tartarean_hound')
route(['volcanic_honeycomb','volcanic_honey_bottle'], [], 'VolcanicBee / VolcanicBeehiveBlock')
route(['stygian_key'], [], 'StygianDeathHandler')
for metal, vanilla in [('iron','raw_iron'),('gold','raw_gold'),('copper','raw_copper')]:
    route([metal+'_dust'], ['volcanite_grinder','minecraft:'+vanilla], 'VolcaniteGrinderBlockEntity.getRecipeResult')
for metal in ['tin','lead','silver','volcanite','feroplumb']:
    route([metal+'_dust'], ['volcanite_grinder',metal+'_ingot'], 'VolcaniteGrinderBlockEntity.getRecipeResult')
for species in ['midas_gold','ares_iron','hermes_copper','hephaestus_volcanite','cronus_lead','zeus_tin','artemis_silver','electrum','tartarean_netherite','pyretic_titan']:
    route([species+'_log',species+'_leaves',species+'_fruit'],[species+'_sapling'],'OreTreeSaplingBlock + MineralTreeLeavesBlock / Bonsai')
pairs={'hades_coal':['minecraft:oak_sapling','hephaestus_volcanite_sapling','demetrian_grafting_knife','demetrian_grafting_tape'],
       'astrape_redstone':['ares_iron_sapling','hades_coal_sapling','demetrian_grafting_knife','demetrian_grafting_tape'],
       'poseidon_lapis':['midas_gold_sapling','hermes_copper_sapling','demetrian_grafting_knife','demetrian_grafting_tape'],
       'apollo_quartz':['ares_iron_sapling','artemis_silver_sapling','demetrian_grafting_knife','demetrian_grafting_tape'],
       'plouton_emerald':['poseidon_lapis_sapling','midas_gold_sapling','demetrian_grafting_knife','demetrian_grafting_tape'],
       'dionysian_amethyst':['astrape_redstone_sapling','apollo_quartz_sapling','demetrian_grafting_knife','demetrian_grafting_tape'],
       'hyperion_glowstone':['astrape_redstone_sapling','midas_gold_sapling','demetrian_grafting_knife','demetrian_grafting_tape'],
       'adamantine_diamond':['tartarean_netherite_sapling','plouton_emerald_sapling','demetrian_grafting_knife','demetrian_grafting_tape'],
       'pyretic_titan':['tartarean_netherite_sapling','demetrian_grafting_knife','pollinating_brush']}
for species, req in pairs.items():
    route([species+'_sapling'],req,'GraftingRegistry / GraftedSaplingBlockEntity (Titan is a probabilistic mutation)')
route(['demetrian_scion'],['demetrian_grafting_knife','minecraft:oak_sapling'],'DemetrianGraftingKnifeItem')
route(['dew_droplet','magma_droplet'],['hydrothermal_condenser'],'HydrothermalCondenserBlockEntity')
for metal in ['volcanite','iron','gold','copper','coal','lapis','redstone','diamond','netherite','tin','lead','silver']:
    route([metal+'_petal'],[metal+'_seed'],'VolcanicOreCropBlock.harvest')
route(['dionysian_pomace','volcanic_cider'],['dionysian_press','minecraft:apple'],'DionysianPressBlockEntity')
route(['volcanic_tuica'],['dionysian_distillery','minecraft:apple','minecraft:glass_bottle'],'DionysianDistilleryBlockEntity')
route(['volcanic_tuica_1l'],['dionysian_distillery','minecraft:apple','empty_tuica_bottle_1l'],'DionysianDistilleryBlockEntity')
route(['vintage_volcanic_tuica'],['dionysian_aging_barrel','volcanic_tuica'],'DionysianAgingBarrelBlockEntity')
route(['grand_reserve_tuica'],['dionysian_aging_barrel','vintage_volcanic_tuica'],'DionysianAgingBarrelBlockEntity')
route(['bio_oil_bucket','bio_ethanol_bucket','biodiesel_bucket','pyro_diesel_bucket','pyroclastic_distillate_bucket','steam_bucket'],['volcanic_distillation_chamber','minecraft:bucket'],'VolcanicDistillationChamberBlockEntity / fluid container mapping')
route(['pyroclastic_flask'],['pyroclastic_distillate_bucket','volcanic_wood_liquid_barrel','minecraft:glass_bottle'],'LiquidTankBlockEntity / VolcanicWoodLiquidBarrelBlock')
route(['singularity_hadron_alloy'],['olympian_hadron_collider'],'OlympianHadronColliderBlockEntity collision outputs')

exempt = {i for i in known if i.startswith('obsidium:creative_') or i.endswith('_spawn_egg')}
# Generated multiblock parts and death containers are technical blocks, not craftable rewards.
for item in ['grafted_sapling','magma_forge_dummy','mega_volcanic_barrel_part','tartarus_piston_engine_part','dionysian_bio_caldera_part','stygian_cenotaph']:
    if 'obsidium:'+item in known: exempt.add('obsidium:'+item)

available = set()
paths = {}
for _ in range(len(known)+1):
    changed = False
    for output, groups, evidence in recipes + routes:
        if output in available: continue
        if all(any(v.startswith('minecraft:') or v in available for v in group) for group in groups):
            available.add(output);paths[output]=evidence;changed=True
    if not changed:break
missing = sorted(known-available-exempt)
report = {'assumptions':['Vanilla survival materials available','Documented machine/growth routes available with their declared prerequisites','Native recipe/tag validity checked separately','Not a proof of playthrough time or all possible modpack overrides'],
          'literal_registered_items':len(known),'reachable_survival_items':len(known & available),'exceptions':sorted(exempt),'unresolved':missing,'invalid_references':problems,'paths':{k:paths[k] for k in sorted(known & available)}}
out=ROOT/'build/reports/progression/obtainability.json';out.parent.mkdir(parents=True,exist_ok=True);out.write_text(json.dumps(report,indent=2)+'\n')
print(json.dumps({k:report[k] for k in ['literal_registered_items','reachable_survival_items','exceptions','unresolved','invalid_references']},indent=2))
raise SystemExit(1 if missing or problems else 0)
