const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const root = path.resolve(__dirname,'../..');
class Element {
  constructor(text='') {this.textContent=text;this.events={};this.value='';this.hidden=false;this.focused=false;}
  addEventListener(name,callback){this.events[name]=callback;}
  focus(){this.focused=true;}
}
const search=new Element(),clear=new Element(),status=new Element();
const cards=['Materials & Components Metals dusts alloys','Storage & Items Chests disks','Botany & Manuals Grafting'].map(text=>new Element(text));
const context={document:{getElementById:id=>({categorySearch:search,clearCategorySearch:clear,categorySearchStatus:status})[id],querySelectorAll:()=>cards}};
vm.runInNewContext(fs.readFileSync(path.join(root,'docs/landing.js'),'utf8'),context);
assert.equal(status.textContent,'3 categories to explore');
search.value='STORAGE';search.events.input();assert.equal(cards.filter(card=>!card.hidden).length,1);assert.equal(status.textContent,'1 category to explore');
search.value='not-a-category';search.events.input();assert(status.textContent.startsWith('No matching'));assert(cards.every(card=>card.hidden));
clear.events.click();assert(cards.every(card=>!card.hidden));assert(clear.hidden);assert(search.focused);
search.value='botany';search.events.input();search.events.keydown({key:'Escape'});assert.equal(search.value,'');assert(cards.every(card=>!card.hidden));
console.log('PASS: landing category search, case folding, empty results, clear/focus and Escape reset.');
