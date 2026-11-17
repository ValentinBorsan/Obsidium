const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const root = path.resolve(__dirname,'../..');
const source = fs.readFileSync(path.join(root,'src/main/java/net/valentin/obsidium/ObsidiumMod.java'),'utf8');
const registered = [...new Set([...source.matchAll(/registerItem\("([^"]+)"/g)].map(match => 'obsidium:'+match[1]))].sort();
const categories = ['materials','machines','transport','storage','botany','energy','equipment','building','mythology','supplies'];
const items = categories.flatMap(category => JSON.parse(fs.readFileSync(path.join(root,`docs/wiki-catalog/${category}.json`))).items);
assert.deepEqual([...new Set(items.map(item=>item.id))].sort(),registered);
const recipes = items.flatMap(item=>item.recipes);
function recipe(id) {const found=recipes.find(recipe=>recipe.id === 'obsidium:'+id);assert(found,id);return found;}
assert.equal(recipe('volcanite_wrench').worktableRequired,false);
assert.equal(recipe('volcanite_worktable').worktableRequired,false);
assert.equal(recipe('typhonic_pickaxe').worktableRequired,true);
assert.equal(recipe('olympian_altar').worktableRequired,true);
for (const item of items.filter(item=>item.group==='Creative & testing')) {
  assert.equal(item.recipes.length,0);
  assert(item.acquisition.some(route=>route.kind==='Creative inventory'));
}
const completion=JSON.parse(fs.readFileSync(path.join(root,'docs/wiki-catalog/completion.json')));
assert.equal(completion.covered,registered.length);assert.deepEqual(completion.missing,[]);
console.log(`PASS: ${registered.length} unique core registrations covered; workstation bootstrap/restriction fixtures and Creative availability labels.`);
