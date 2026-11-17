const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const root = path.resolve(__dirname, '../..');
const category = process.argv[2] || 'materials';
const data = JSON.parse(fs.readFileSync(path.join(root, `docs/wiki-catalog/${category}.json`), 'utf8'));
const script = fs.readFileSync(path.join(root, 'docs/wiki-catalog/catalog.js'), 'utf8');
class Element {
  constructor(group) {this.dataset = group ? {group} : {}; this.events = {}; this.attributes = {}; this.value = ''; this.innerHTML = ''; this.textContent = '';}
  addEventListener(name, callback) {this.events[name] = callback;}
  setAttribute(key, value) {this.attributes[key] = value;}
}
async function mount(payload, ok = true) {
  const search = new Element(); const grid = new Element(); const count = new Element();
  const groups = ['All', ...new Set(payload.items.map(item => item.group))].map(group => new Element(group));
  const container = new Element();
  container.querySelector = selector => ({'#catalogSearch':search, '#catalogGrid':grid, '#catalogCount':count})[selector];
  container.querySelectorAll = () => groups;
  const context = {window:{}, location:{hash:''}, document:{getElementById:() => null}, requestAnimationFrame:callback => callback(), fetch:async url => {assert.equal(url, `wiki-catalog/${category}.json`); return {ok, json:async () => payload};}};
  vm.runInNewContext(script, context);
  await context.window.WikiCatalog.mount(container, category);
  return {search, grid, count, groups, container};
}
(async () => {
  assert.equal(data.count, data.items.length);
  if (category === 'building') {
    const metadata = JSON.parse(fs.readFileSync(path.join(root, 'scripts/wiki-building.json')));
    const forms = data.items.filter(item => /_(stairs|slab|fence|fence_gate)$/.test(item.id));
    assert(forms.length > 0);
    for (const item of forms) {assert.equal(item.imageKind, 'rendered block model'); assert(item.image.endsWith('.svg'));}
    const variants = ['stairs','slab','fence','fence_gate'].map(shape => fs.readFileSync(path.join(root, 'docs/assets/catalog-icons/obsidium/midas_gold_' + shape + '.svg'), 'utf8'));
    assert.equal(new Set(variants).size, 4);
    assert.deepEqual(data.items.map(item => item.id).sort(), Object.keys(metadata).map(id => 'obsidium:' + id).sort());
    const registered = fs.readFileSync(path.join(root, 'src/main/java/net/valentin/obsidium/ObsidiumMod.java'), 'utf8');
    [...registered.matchAll(/registerItem\("([^"]+)"/g)].map(match => match[1]).filter(id => /_(log|planks|slab|stairs|fence|fence_gate|leaves|ore)$/.test(id)).forEach(id => assert(metadata[id], id));
  }
  if (category === 'equipment') {
    const metadata = JSON.parse(fs.readFileSync(path.join(root, 'scripts/wiki-equipment.json')));
    assert.deepEqual(data.items.map(item => item.id).sort(), Object.keys(metadata).map(id => 'obsidium:' + id).sort());
    const registered = fs.readFileSync(path.join(root, 'src/main/java/net/valentin/obsidium/ObsidiumMod.java'), 'utf8');
    const tools = [...registered.matchAll(/registerItem\("((?:promethean|tartarean|typhonic|hephaestean|faraday)_[^"]+)"/g)].map(match => match[1]).filter(id => /_(sword|pickaxe|axe|shovel|hoe|paxel|helmet|chestplate|leggings|boots)$/.test(id));
    tools.forEach(id => assert(metadata[id], id));
  }
  if (category === 'energy') {
    const metadata = JSON.parse(fs.readFileSync(path.join(root, 'scripts/wiki-energy.json')));
    assert.deepEqual(data.items.map(item => item.id).sort(), Object.keys(metadata).map(id => 'obsidium:' + id).sort());
    for (const id of ['volcanite_energy_core','astrape_energy_pylon','supreme_volcanite_overclock_upgrade','volcanite_supreme_pipe_energy_upgrade']) assert(data.items.some(item => item.id === 'obsidium:' + id));
  }
  if (category === 'botany') {
    const metadata = JSON.parse(fs.readFileSync(path.join(root, 'scripts/wiki-botany.json')));
    assert.deepEqual(data.items.map(item => item.id).sort(), Object.keys(metadata).map(id => 'obsidium:' + id).sort());
    for (const id of ['demetrian_botany_book','magma_forge_manual','pyretic_titan_sapling','grafted_sapling']) assert(data.items.some(item => item.id === 'obsidium:' + id));
  }
  if (category === 'storage') {
    const metadata = JSON.parse(fs.readFileSync(path.join(root, 'scripts/wiki-storage.json')));
    assert.deepEqual(data.items.map(item => item.id).sort(), Object.keys(metadata).map(id => 'obsidium:' + id).sort());
    for (const id of ['quantum_link_module','volcanite_modular_chest','mega_volcanic_barrel','volcanite_storage_remote']) assert(data.items.some(item => item.id === 'obsidium:' + id));
  }
  if (category === 'transport') {
    const registered = fs.readFileSync(path.join(root, 'src/main/java/net/valentin/obsidium/ObsidiumMod.java'), 'utf8');
    const expected = [...registered.matchAll(/registerBlock\("([^"]*(?:pipe|wire|conduit))"/g)].map(match => 'obsidium:' + match[1]).sort();
    assert.deepEqual(data.items.map(item => item.id).sort(), expected);
  }
  assert.equal(new Set(data.items.map(item => item.id)).size, data.count);
  function checkImages(value) {
    if (!value || typeof value !== 'object') return;
    if (value.image) assert(fs.existsSync(path.join(root, 'docs', value.image)), value.image);
    Object.values(value).forEach(checkImages);
  }
  checkImages(data);
  let recipes = 0;
  for (const item of data.items) {
    assert(fs.existsSync(path.join(root, 'docs', item.image)), item.id);
    assert(item.recipes.length || item.acquisition.length, item.id);
    if (category !== 'materials') {if (!['botany','building','mythology','supplies'].includes(category)) assert(item.recipes.length, item.id); assert(fs.existsSync(path.join(root, item.source)), item.source);}
    for (const route of item.acquisition) assert(fs.existsSync(path.join(root, route.source)), route.source);
    for (const recipe of item.recipes) {
      recipes++;
      const source = JSON.parse(fs.readFileSync(path.join(root, recipe.source), 'utf8'));
      const exported = JSON.parse(fs.readFileSync(path.join(root, 'docs', recipe.json), 'utf8'));
      assert.deepEqual(exported, source);
      assert.equal(recipe.output.id, source.result.id || source.result.item || source.result);
      assert.equal(recipe.count, typeof source.result === 'object' ? source.result.count || 1 : 1);
      if (source.pattern) assert.deepEqual(recipe.pattern, source.pattern);
      if (source.type.startsWith('minecraft:crafting_')) {assert(recipe.station); assert(fs.existsSync(path.join(root, recipe.stationSource)));}
      if (recipe.worktableRequired) assert.equal(recipe.station, 'Volcanite Worktable');
    }
  }
  const rendered = await mount(data);
  assert.equal((rendered.grid.innerHTML.match(/class="catalog-card"/g) || []).length, data.count);
  assert.equal(rendered.count.textContent, `${data.count} of ${data.count} items`);
  rendered.search.value = category === 'mythology' ? 'obsidium:olympian_altar' : category === 'supplies' ? 'obsidium:ash_briquette' : category === 'building' ? 'obsidium:midas_gold_planks' : category === 'equipment' ? 'obsidium:promethean_pickaxe' : category === 'energy' ? 'obsidium:volcanite_energy_cell' : category === 'botany' ? 'obsidium:demetrian_botany_book' : category === 'storage' ? 'obsidium:volcanite_modular_chest' : category === 'transport' ? 'obsidium:hermes_item_pipe' : category === 'machines' ? 'obsidium:volcanite_grinder' : 'obsidium:volcanic_alloy'; rendered.search.events.input();
  assert.equal(rendered.count.textContent, `1 of ${data.count} items`);
  if (category === 'materials') assert(rendered.grid.innerHTML.includes('Volcanite Ingot or Volcanite'));
  assert(rendered.container.innerHTML.includes('wiki.html?page=Machine-Catalog'));
  assert(rendered.grid.innerHTML.includes(['botany','building'].includes(category) ? 'Crafting · any order' : 'Crafting · shaped'));
  assert(!rendered.grid.innerHTML.includes('View recipe JSON'));
  assert(!rendered.grid.innerHTML.includes('Implementation reference'));
  assert(!rendered.grid.innerHTML.includes('Source reference'));
  assert(rendered.grid.innerHTML.includes('Station:') || rendered.grid.innerHTML.includes('Requires:'));
  rendered.search.value = ''; rendered.search.events.input();
  const group = category === 'mythology' ? 'Divine scrolls' : category === 'supplies' ? 'Fluid containers' : category === 'building' ? 'Wood shapes' : category === 'equipment' ? 'Armor' : category === 'energy' ? 'Energy storage' : category === 'botany' ? 'Manuals' : category === 'storage' ? 'Item disks' : category === 'transport' ? 'Energy wires' : category === 'machines' ? 'Processing' : 'Dusts';
  const dusts = rendered.groups.find(button => button.dataset.group === group); dusts.events.click();
  assert.equal(rendered.count.textContent, `${data.items.filter(item => item.group === group).length} of ${data.count} items`);
  assert.equal(dusts.attributes['aria-pressed'], 'true');
  rendered.search.value = 'no-such-item'; rendered.search.events.input();
  assert(rendered.grid.innerHTML.includes('No matching items'));
  const untrusted = structuredClone(data); untrusted.items[0].name = '<img src=x onerror=alert(1)>';
  const escaped = await mount(untrusted);
  assert(!escaped.grid.innerHTML.includes('<img src=x onerror='));
  assert(escaped.grid.innerHTML.includes('&lt;img'));
  const failed = await mount(data, false); assert(failed.container.innerHTML.includes('Catalog unavailable'));
  console.log(`PASS: ${data.count} item cards, ${recipes} exact source recipes, acquisition evidence, search, groups, empty/error states and escaping.`);
})().catch(error => {console.error(error); process.exitCode = 1;});
