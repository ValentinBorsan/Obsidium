/* Source-backed category renderer. No external dependencies. */
window.WikiCatalog = (() => {
  const escape = value => String(value ?? '').replace(/[&<>"']/g, ch => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[ch]));
  const image = (item, size = '') => item.image
    ? `<img class="catalog-icon ${size}" src="${escape(item.image)}" alt="${escape(item.name)}" title="${escape(item.name)} · ${escape(item.imageKind)}" loading="lazy" width="40" height="40">`
    : '<span class="catalog-no-image" aria-hidden="true">#</span>';
  const ingredientText = ingredient => ingredient.item ? ingredient.item.name : ingredient.tag ? '#' + ingredient.tag
    : ingredient.alternatives ? ingredient.alternatives.map(ingredientText).join(' or ') : 'See source JSON';
  const firstItem = ingredient => ingredient?.item || (ingredient?.alternatives ? firstItem(ingredient.alternatives[0]) : null);
  function cell(ingredient) {
    if (!ingredient) return '<span class="catalog-cell catalog-cell-empty" aria-label="Empty slot"></span>';
    const item = firstItem(ingredient);
    const alternatives = ingredient.alternatives?.length || 0;
    return `<span class="catalog-cell" title="${escape(ingredientText(ingredient))}">${item ? image(item) : '<span class="catalog-tag">#</span>'}${alternatives > 1 ? `<small>+${alternatives - 1}</small>` : ''}</span>`;
  }
  const methodName = type => ({'minecraft:crafting_shaped':'Crafting · shaped','minecraft:crafting_shapeless':'Crafting · any order',
    'minecraft:smelting':'Furnace','minecraft:blasting':'Blast furnace','minecraft:campfire_cooking':'Campfire','minecraft:smoking':'Smoker'})[type] || type;
  function recipe(recipe) {
    let ingredients = [];
    let diagram = '';
    let legend = [];
    if (recipe.pattern) {
      for (let row = 0; row < 3; row++) for (let col = 0; col < 3; col++) {
        const symbol = recipe.pattern[row]?.[col] || ' ';
        ingredients.push(symbol === ' ' ? null : recipe.key[symbol]);
      }
      for (const [symbol, ingredient] of Object.entries(recipe.key)) {
        const count = recipe.pattern.join('').split('').filter(ch => ch === symbol).length;
        legend.push(`${count} × ${ingredientText(ingredient)}`);
      }
    } else if (recipe.ingredients) {
      ingredients = [...recipe.ingredients];
      legend = ingredients.map(ingredientText);
      while (ingredients.length < 9) ingredients.push(null);
    } else if (recipe.ingredient) {
      ingredients = [recipe.ingredient];
      legend = [ingredientText(recipe.ingredient)];
    }
    if (ingredients.length) {
      diagram = `<div class="catalog-recipe-flow"><div class="catalog-recipe-grid ${recipe.ingredient ? 'catalog-single-slot' : ''}" aria-label="Ingredients">${ingredients.map(cell).join('')}</div><span class="catalog-arrow" aria-hidden="true">→</span><div class="catalog-cell catalog-output" title="${escape(recipe.output.name)}">${image(recipe.output)}<small>${recipe.count}</small></div></div>`;
    }
    return `<details class="catalog-recipe"><summary>${escape(methodName(recipe.type))} <span>→ ${recipe.count}</span></summary>${diagram}
      ${legend.length ? `<ul class="catalog-ingredients">${legend.map(text => `<li>${escape(text)}</li>`).join('')}</ul>` : ''}
      ${recipe.cookingtime != null ? `<p class="catalog-process">${recipe.cookingtime / 20} s${recipe.experience != null ? ` · ${recipe.experience} XP` : ''}</p>` : ''}
      ${recipe.raw ? `<pre>${escape(JSON.stringify(recipe.raw, null, 2))}</pre>` : ''}
      <a class="catalog-source" href="${escape(recipe.json)}">View recipe JSON</a></details>`;
  }
  function route(route) {
    return `<div class="catalog-route"><strong>${escape(route.kind)}</strong><p>${escape(route.note)}</p>
      ${route.inputs ? `<div class="catalog-route-inputs">${route.inputs.map(item => `<span>${image(item)}${escape(item.name)}</span>`).join('')}</div>` : ''}
      <details class="catalog-evidence"><summary>Source reference</summary><code>${escape(route.source)}</code></details></div>`;
  }
  function card(item) {
    return `<article class="catalog-card" id="item-${escape(item.id.replace(':','-'))}">
      <header class="catalog-card-header">${image(item, 'catalog-portrait')}<div><h3>${escape(item.name)}</h3><code>${escape(item.id)}</code></div></header>
      <p class="catalog-description">${escape(item.description)}</p><span class="catalog-group-tag">${escape(item.group)}</span>
      ${item.acquisition.map(route).join('')}
      ${item.recipes.length ? `<p class="catalog-variant-count">${item.recipes.length} recipe ${item.recipes.length === 1 ? 'variant' : 'variants'} · expand to inspect</p>${item.recipes.map(recipe).join('')}` : '<p class="catalog-process">No crafting JSON. Use the acquisition route above.</p>'}
      <a class="catalog-permalink" href="#item-${escape(item.id.replace(':','-'))}">Link to item</a></article>`;
  }
  async function mount(container) {
    container.innerHTML = '<p role="status">Loading item catalog…</p>';
    try {
      const response = await fetch('wiki-catalog/materials.json');
      if (!response.ok) throw new Error('Catalog unavailable');
      const data = await response.json();
      const groups = ['All', ...new Set(data.items.map(item => item.group))];
      const totalRecipes = data.items.reduce((count, item) => count + item.recipes.length, 0);
      container.innerHTML = `<div class="catalog-intro"><span class="catalog-eyebrow">Item catalog · Category 01</span><h1>${escape(data.title)}</h1>
        <p>${escape(data.note)}</p><p class="catalog-metrics">${data.count} items · ${totalRecipes} recipe variants · actual game textures</p></div>
        <div class="catalog-toolbar"><label for="catalogSearch">Find an item</label><input id="catalogSearch" type="search" placeholder="Name or item ID…" autocomplete="off">
        <div class="catalog-filters" aria-label="Material groups">${groups.map(group => `<button type="button" data-group="${escape(group)}" aria-pressed="${group === 'All'}">${escape(group)}</button>`).join('')}</div>
        <p id="catalogCount" role="status" aria-live="polite"></p></div><div class="catalog-grid" id="catalogGrid"></div>
        <aside class="catalog-scope"><h2>Category coverage</h2><p>${escape(data.scope)}</p><p>Next batches: ores & blocks, machines & energy, logistics & storage, botany, equipment, mythology and the separate Rails addon.</p></aside>`;
      let selected = 'All';
      const search = container.querySelector('#catalogSearch');
      const grid = container.querySelector('#catalogGrid');
      function update() {
        const query = search.value.trim().toLowerCase();
        const items = data.items.filter(item => (selected === 'All' || item.group === selected) && `${item.name} ${item.id}`.toLowerCase().includes(query));
        grid.innerHTML = items.map(card).join('') || '<p class="catalog-empty">No matching items. Try a different search or group.</p>';
        container.querySelector('#catalogCount').textContent = `${items.length} of ${data.count} items`;
        container.querySelectorAll('[data-group]').forEach(button => button.setAttribute('aria-pressed', String(button.dataset.group === selected)));
      }
      search.addEventListener('input', update);
      container.querySelectorAll('[data-group]').forEach(button => button.addEventListener('click', () => {selected = button.dataset.group; update();}));
      update();
      if (location.hash.startsWith('#item-')) requestAnimationFrame(() => {
        const target = document.getElementById(location.hash.slice(1));
        if (target) {target.scrollIntoView({block:'start'}); target.classList.add('catalog-highlight');}
      });
    } catch (error) {
      container.innerHTML = '<h1>Catalog unavailable</h1><p>Could not load the catalog data. Serve the docs folder over HTTP and try again.</p>';
    }
  }
  return {mount};
})();
