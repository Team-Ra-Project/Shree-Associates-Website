/* ==========================================================================
   SHREE ASSOCIATES — reviews.js
   Renders the `reviews` array (js/reviews-data.js) into the Reviews page:
   featured strip, search, service/rating filters, and Load More pagination.
   Runs only on reviews.html (checks for #reviewGrid before doing anything).
   ========================================================================== */

document.addEventListener('DOMContentLoaded', () => {
  const grid = document.getElementById('reviewGrid');
  if (!grid || typeof reviews === 'undefined') return; // not on the reviews page

  const PAGE_SIZE = 9;
  let visibleCount = PAGE_SIZE;
  let currentSearch = '';
  let currentService = 'all';
  let currentRating = 'all';

  const searchInput = document.getElementById('reviewSearch');
  const serviceSelect = document.getElementById('serviceFilter');
  const ratingSelect = document.getElementById('ratingFilter');
  const loadMoreBtn = document.getElementById('loadMoreBtn');
  const metaLine = document.getElementById('reviewsMetaLine');
  const countLine = document.getElementById('reviewsCountLine');
  const emptyState = document.getElementById('reviewsEmpty');
  const totalBadge = document.getElementById('reviewTotalBadge');
  const featuredGrid = document.getElementById('featuredGrid');

  if (totalBadge) totalBadge.textContent = `${reviews.length} Client Reviews`;

  /* ---------- Build service filter options from the data itself ---------- */
  if (serviceSelect) {
    const services = [...new Set(reviews.map(r => r.service))].sort();
    services.forEach(s => {
      const opt = document.createElement('option');
      opt.value = s;
      opt.textContent = s;
      serviceSelect.appendChild(opt);
    });
  }

  /* ---------- Build rating filter options from ratings actually present ---------- */
  if (ratingSelect) {
    const ratingsPresent = [...new Set(reviews.map(r => r.rating))].sort((a, b) => b - a);
    ratingsPresent.forEach(r => {
      const opt = document.createElement('option');
      opt.value = String(r);
      opt.textContent = `${r} Star${r === 1 ? '' : 's'}`;
      ratingSelect.appendChild(opt);
    });
  }

  /* ---------- Star rendering — respects the real rating, no rounding up ---------- */
  function starString(rating) {
    const r = Math.max(0, Math.min(5, Math.round(rating)));
    return '★'.repeat(r) + '☆'.repeat(5 - r);
  }

  function initials(name) {
    return name.split(' ').filter(Boolean).slice(0, 2).map(w => w[0]).join('').toUpperCase();
  }

  function reviewCardHtml(r, isFeatured) {
    const hasText = r.review && r.review.trim().length > 0;
    const quoteHtml = hasText
      ? `<p class="quote">&ldquo;${escapeHtml(r.review)}&rdquo;</p>`
      : `<p class="quote is-empty">${escapeHtml(r.name)} rated this ${r.rating} out of 5 for ${escapeHtml(r.service)}.</p>`;
    return `
      <div class="${isFeatured ? 'featured-card' : 'review-card'} fade-scale">
        <div class="stars" aria-label="${r.rating} out of 5 stars">${starString(r.rating)}</div>
        ${quoteHtml}
        <div class="client-row">
          <span class="client-avatar" aria-hidden="true">${initials(r.name)}</span>
          <span>
            <span class="client-name">${escapeHtml(r.name)}</span>
            <span class="client-meta">${escapeHtml(r.society)}</span>
          </span>
        </div>
        <div class="review-tags">
          <span class="review-tag">${escapeHtml(r.service)}</span>
        </div>
      </div>`;
  }

  function escapeHtml(str = '') {
    return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
  }

  /* ---------- Featured strip: pick reviews with the longest review text,
     without ranking or labelling customers — just a useful preview. ---------- */
  function renderFeatured() {
    if (!featuredGrid) return;
    const withText = reviews.filter(r => r.review && r.review.trim().length > 0);
    const featured = [...withText].sort((a, b) => b.review.length - a.review.length).slice(0, 3);
    featuredGrid.innerHTML = featured.map(r => reviewCardHtml(r, true)).join('');
    featuredGrid.querySelectorAll('.fade-scale').forEach(el => window.shreeRevealObserver && window.shreeRevealObserver.observe(el));
  }

  /* ---------- Filtering ---------- */
  function getFiltered() {
    const term = currentSearch.trim().toLowerCase();
    return reviews.filter(r => {
      if (currentService !== 'all' && r.service !== currentService) return false;
      if (currentRating !== 'all' && r.rating !== Number(currentRating)) return false;
      if (term) {
        const haystack = `${r.name} ${r.society} ${r.service} ${r.review}`.toLowerCase();
        if (!haystack.includes(term)) return false;
      }
      return true;
    });
  }

  function render() {
    const filtered = getFiltered();
    const slice = filtered.slice(0, visibleCount);

    grid.innerHTML = slice.map(r => reviewCardHtml(r, false)).join('');
    grid.querySelectorAll('.fade-scale').forEach((el, i) => {
      el.style.setProperty('--delay', `${Math.min(i * 60, 300)}ms`);
      if (window.shreeRevealObserver) window.shreeRevealObserver.observe(el);
      else el.classList.add('in-view');
    });

    if (emptyState) emptyState.hidden = filtered.length !== 0;
    grid.hidden = filtered.length === 0;

    if (metaLine) {
      const parts = [];
      if (currentService !== 'all') parts.push(currentService);
      if (currentRating !== 'all') parts.push(`${currentRating}★`);
      if (currentSearch) parts.push(`"${currentSearch}"`);
      metaLine.textContent = parts.length
        ? `Showing ${Math.min(visibleCount, filtered.length)} of ${filtered.length} reviews matching ${parts.join(', ')}`
        : `Showing ${Math.min(visibleCount, filtered.length)} of ${filtered.length} reviews`;
    }
    if (countLine) countLine.textContent = `Showing ${Math.min(visibleCount, filtered.length)} of ${filtered.length} reviews`;

    if (loadMoreBtn) loadMoreBtn.hidden = visibleCount >= filtered.length;
  }

  if (searchInput) {
    searchInput.addEventListener('input', () => {
      currentSearch = searchInput.value;
      visibleCount = PAGE_SIZE;
      render();
    });
  }
  if (serviceSelect) {
    serviceSelect.addEventListener('change', () => {
      currentService = serviceSelect.value;
      visibleCount = PAGE_SIZE;
      render();
    });
  }
  if (ratingSelect) {
    ratingSelect.addEventListener('change', () => {
      currentRating = ratingSelect.value;
      visibleCount = PAGE_SIZE;
      render();
    });
  }
  if (loadMoreBtn) {
    loadMoreBtn.addEventListener('click', () => {
      visibleCount += PAGE_SIZE;
      render();
    });
  }

  renderFeatured();
  render();
});
