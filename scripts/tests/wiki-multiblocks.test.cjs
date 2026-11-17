const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const root = path.resolve(__dirname, '../..');
const data = JSON.parse(fs.readFileSync(path.join(root, 'docs/wiki-catalog/multiblocks.json')));
const source = JSON.parse(fs.readFileSync(path.join(root, 'scripts/wiki-multiblocks.json')));
const script = fs.readFileSync(path.join(root, 'docs/wiki-catalog/multiblocks.js'), 'utf8');
const context = {window:{}}; vm.runInNewContext(script, context);
assert.equal(data.items.length, 9);
assert.equal(new Set(data.items.map(item => item.id)).size, 9);
assert(fs.existsSync(path.join(root, data.manual)));
const volumes = {'magma-forge':9,'energy-matrix':9,keraunos:27,hadron:16,tartarus:8,'bio-caldera':8,'mega-barrel':8,poseidon:8,altar:5};
for (const item of data.items) {
  const original = source.items.find(value => value.id === item.id);
  assert.deepEqual(item.layers, original.layers);
  assert(item.steps.length >= 3 && item.rules.length);
  item.sources.forEach(file => assert(fs.existsSync(path.join(root, file)), file));
  const counts = {};
  for (const layer of item.layers) {
    assert(layer.rows.every(row => row.length === layer.rows[0].length));
    for (const row of layer.rows) for (const symbol of row) {
      assert(item.parts[symbol] || symbol === '.' || symbol === ' ', symbol);
      if (item.parts[symbol]) counts[symbol] = (counts[symbol] || 0) + 1;
    }
  }
  assert.equal(Object.values(counts).reduce((a,b) => a+b, 0), volumes[item.id]);
  for (const [symbol,part] of Object.entries(item.parts)) {
    assert.equal(part.id, original.parts[symbol]);
    assert(counts[symbol] > 0);
    assert(fs.existsSync(path.join(root, 'docs', part.image)), part.image);
  }
  if (item.id === 'hadron') {
    assert.equal(counts.C, 12);
    assert.equal(item.layers[0].rows.join('').split('.').length - 1, 9);
  }
  if (item.id === 'keraunos') assert.equal(counts.K, 1);
}
const rendered = context.window.WikiMultiblocks.render(data);
assert(!rendered.includes('Manual & implementation references'));
assert(!rendered.includes('src/main/'));
assert.equal((rendered.match(/class="multiblock-guide"/g) || []).length, 9);
assert.equal((rendered.match(/<figure\b/g) || []).length, 15);
const untrusted = structuredClone(data); untrusted.items[0].title = '<script>bad()</script>';
assert(!context.window.WikiMultiblocks.render(untrusted).includes('<script>bad()'));
assert(context.window.WikiMultiblocks.render(untrusted).includes('&lt;script&gt;'));
console.log('PASS: 9 structures, 15 layers, blueprint counts, component assets, source references and HTML escaping.');
