/* Layer-by-layer examples from the manual, checked against formation validators. */
window.WikiMultiblocks = (() => {
  const escape = value => String(value ?? '').replace(/[&<>"']/g, ch => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[ch]));
  function render(data) {
    const image = part => `<img src="${escape(part.image)}" alt="${escape(part.name)}" width="32" height="32" loading="lazy">`;
    return `<div class="catalog-intro"><span class="catalog-eyebrow">Build · Form · Operate</span><h1>${escape(data.title)}</h1>
      <p>Layer-by-layer examples from the in-game manual, checked against current formation code. Each square is one world block. Images show representative game textures.</p>
      <p>Build layers from bottom to top. All diagrams are top views; north is at the top and east is to the right unless stated otherwise. Empty diagram spaces are unused; dotted collider squares must be kept clear. On smaller screens, scroll a layer horizontally to see its full layout.</p>
      <nav class="multiblock-toc" aria-label="Structures">${data.items.map(item => `<a href="#${escape(item.id)}">${escape(item.title)}</a>`).join('')}</nav></div>
      ${data.items.map(item => {
        const counts = {};
        for (const layer of item.layers) for (const row of layer.rows) for (const symbol of row) if (item.parts[symbol]) counts[symbol] = (counts[symbol] || 0) + 1;
        return `<article class="multiblock-guide" id="${escape(item.id)}"><header><span class="catalog-eyebrow">${escape(item.size)}</span><h2>${escape(item.title)}</h2></header>
          <div class="multiblock-layers">${item.layers.map(layer => `<figure tabindex="0" aria-label="${escape(item.title)} — ${escape(layer.label)}"><figcaption>${escape(layer.label)}</figcaption><div class="multiblock-blueprint" style="--columns:${layer.rows[0].length}" aria-label="${escape(layer.label)}">${layer.rows.map(row => [...row].map(symbol => {
            const part = item.parts[symbol];
            return part ? `<span class="multiblock-slot" title="${escape(part.name)}">${image(part)}<b>${escape(symbol)}</b></span>` : `<span class="multiblock-slot ${symbol === '.' ? 'multiblock-air' : 'multiblock-unused'}" aria-label="${symbol === '.' ? 'Keep clear' : 'Unused position'}">${symbol === '.' ? '·' : ''}</span>`;
          }).join('')).join('')}</div></figure>`).join('')}</div>
          <h3>Blocks for this example</h3><ul class="multiblock-parts">${Object.entries(item.parts).map(([symbol, part]) => `<li>${image(part)}<span><b>${escape(symbol)}</b> · ${counts[symbol]} × ${escape(part.name)}</span></li>`).join('')}</ul>
          <h3>How to form it</h3><ol>${item.steps.map(step => `<li>${escape(step)}</li>`).join('')}</ol>
          <h3>Operation</h3>${item.details.map(text => `<p>${escape(text)}</p>`).join('')}
          <aside class="catalog-route"><strong>Accepted layouts & troubleshooting</strong>${item.rules.map(text => `<p>${escape(text)}</p>`).join('')}<p>If formation fails, check every layer, block type, height and controller orientation before supplying inputs.</p></aside>
          <a class="catalog-source" href="wiki.html?page=Machine-Catalog">Machine crafting recipes</a>
          </article>`;
      }).join('')}`;
  }
  async function mount(container) {
    container.innerHTML = '<p role="status">Loading construction guide…</p>';
    try {
      const response = await fetch('wiki-catalog/multiblocks.json');
      if (!response.ok) throw new Error('Guide unavailable');
      container.innerHTML = render(await response.json());
      if (location.hash) requestAnimationFrame(() => document.getElementById(location.hash.slice(1))?.scrollIntoView({block:'start'}));
    } catch (_) {container.innerHTML = '<h1>Guide unavailable</h1><p>Serve the docs folder over HTTP and try again.</p>';}
  }
  return {mount, render};
})();
