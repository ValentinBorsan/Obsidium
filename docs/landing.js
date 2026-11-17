/* Category search enhances the static collection; all links work without JS. */
(() => {
  const search = document.getElementById('categorySearch');
  const clear = document.getElementById('clearCategorySearch');
  const status = document.getElementById('categorySearchStatus');
  const cards = [...document.querySelectorAll('#categoryCollection .category-card')];
  if (!search || !clear || !status) return;
  function update() {
    const query = search.value.trim().toLocaleLowerCase();
    let visible = 0;
    cards.forEach(card => {
      const match = card.textContent.toLocaleLowerCase().includes(query);
      card.hidden = !match;
      if (match) visible++;
    });
    clear.hidden = !query;
    status.textContent = visible ? `${visible} ${visible === 1 ? 'category' : 'categories'} to explore` : 'No matching categories. Try a different word or clear the search.';
  }
  search.addEventListener('input', update);
  search.addEventListener('keydown', event => {if (event.key === 'Escape') {search.value = ''; update();}});
  clear.addEventListener('click', () => {search.value = ''; update(); search.focus();});
  update();
})();
