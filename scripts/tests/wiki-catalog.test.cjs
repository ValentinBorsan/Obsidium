const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const root = path.resolve(__dirname, '../..');
const data = JSON.parse(fs.readFileSync(path.join(root, 'docs/wiki-catalog/materials.json'), 'utf8'));
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
  const context = {window:{}, location:{hash:''}, document:{getElementById:() => null}, requestAnimationFrame:callback => callback(), fetch:async () => ({ok, json:async () => payload})};
  vm.runInNewContext(script, context);
  await context.window.WikiCatalog.mount(container);
  return {search, grid, count, groups, container};
}
(async () => {
  assert.equal(data.count, data.items.length);
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
    for (const route of item.acquisition) assert(fs.existsSync(path.join(root, route.source)), route.source);
    for (const recipe of item.recipes) {
      recipes++;
      const source = JSON.parse(fs.readFileSync(path.join(root, recipe.source), 'utf8'));
      const exported = JSON.parse(fs.readFileSync(path.join(root, 'docs', recipe.json), 'utf8'));
      assert.deepEqual(exported, source);
      assert.equal(recipe.output.id, source.result.id || source.result.item || source.result);
      assert.equal(recipe.count, typeof source.result === 'object' ? source.result.count || 1 : 1);
      if (source.pattern) assert.deepEqual(recipe.pattern, source.pattern);
    }
  }
  const rendered = await mount(data);
  assert.equal((rendered.grid.innerHTML.match(/class="catalog-card"/g) || []).length, data.count);
  assert.equal(rendered.count.textContent, `${data.count} of ${data.count} items`);
  rendered.search.value = 'obsidium:volcanic_alloy'; rendered.search.events.input();
  assert.equal(rendered.count.textContent, `1 of ${data.count} items`);
  assert(rendered.grid.innerHTML.includes('Volcanite Ingot or Volcanite'));
  assert(rendered.grid.innerHTML.includes('Crafting · shaped'));
  assert(rendered.grid.innerHTML.includes('View recipe JSON'));
  rendered.search.value = ''; rendered.search.events.input();
  const dusts = rendered.groups.find(button => button.dataset.group === 'Dusts'); dusts.events.click();
  assert.equal(rendered.count.textContent, `${data.items.filter(item => item.group === 'Dusts').length} of ${data.count} items`);
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
