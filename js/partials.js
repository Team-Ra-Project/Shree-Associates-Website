/* ==========================================================================
   SHREE ASSOCIATES — partials.js
   Injects the shared header, footer and floating widgets on every page so
   that markup lives in ONE place. Loaded with a plain <script> tag (not
   fetch), so it works from a local file:// double-click AND from any host —
   no build step, no server required.

   Usage: put <body data-page="about"> on each page (the value matches one
   of the NAV_LINKS hrefs below, without ".html") so the right nav item is
   marked active.
   ========================================================================== */

(function () {
  const NAV_LINKS = [
    { href: 'index.html', label: 'Home', key: 'index' },
    { href: 'about.html', label: 'About', key: 'about' },
    { href: 'services.html', label: 'Services', key: 'services' },
    { href: 'why-us.html', label: 'Why Us', key: 'why-us' },
    { href: 'team.html', label: 'Team', key: 'team' },
    { href: 'reviews.html', label: 'Reviews', key: 'reviews' },
    { href: 'case-studies.html', label: 'Case Studies', key: 'case-studies' },
    { href: 'ulwe.html', label: 'Ulwe', key: 'ulwe' },
    { href: 'contact.html', label: 'Contact', key: 'contact', cta: true }
  ];

  const currentPage = document.body.dataset.page || 'index';

  const navHtml = NAV_LINKS.map(link => {
    const classes = [link.cta ? 'nav-cta' : '', link.key === currentPage ? 'active' : ''].filter(Boolean).join(' ');
    const classAttr = classes ? ` class="${classes}"` : '';
    const current = link.key === currentPage ? ' aria-current="page"' : '';
    return `<a href="${link.href}"${classAttr}${current}>${link.label}</a>`;
  }).join('');

  const headerHtml = `
    <div class="container header-inner">
      <a href="index.html" class="brand">
        <img src="images/logo.jpeg" alt="Shree Associates logo" class="brand-mark">
        <span class="brand-text">
          <span class="brand-name">Shree Associates</span>
          <span class="brand-sub">Legal · Society · Property</span>
        </span>
      </a>
      <nav class="nav" id="mainNav">${navHtml}</nav>
      <button class="nav-toggle" id="navToggle" aria-label="Toggle navigation" aria-expanded="false" aria-controls="mainNav">
        <span></span><span></span><span></span>
      </button>
    </div>`;

  const footerHtml = `
    <div class="container footer-inner">
      <div class="footer-brand">
        <img src="images/logo.jpeg" alt="Shree Associates logo">
        <span>Shree Associates</span>
      </div>
      <p class="footer-tagline">Legal, Society &amp; Property Compliance Consultant — trusted since 2006.</p>
      <p class="footer-local">Serving Ulwe and Navi Mumbai with professional Society, Legal &amp; Property Services.</p>
      <nav class="footer-nav">
        ${NAV_LINKS.map(l => `<a href="${l.href}">${l.label}</a>`).join('')}
      </nav>
      <p class="footer-copy">&copy; <span id="year"></span> Shree Associates. All rights reserved. Offices in Kamothe, Ulwe &amp; Fort, Mumbai.</p>
    </div>`;

  const headerEl = document.getElementById('siteHeader');
  const footerEl = document.getElementById('siteFooter');
  if (headerEl) { headerEl.innerHTML = headerHtml; }
  if (footerEl) { footerEl.innerHTML = footerHtml; }

  // Floating widgets shared on every page (back-to-top + WhatsApp FAB)
  const widgetsEl = document.getElementById('siteWidgets');
  if (widgetsEl) {
    widgetsEl.innerHTML = `
      <button id="backToTop" class="back-to-top ripple" aria-label="Back to top">↑</button>
      <button id="fabWhatsapp" class="fab-whatsapp" aria-label="Chat on WhatsApp" data-whatsapp data-wa-service="general enquiry">
        <svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 2a10 10 0 0 0-8.6 15.1L2 22l5.05-1.36A10 10 0 1 0 12 2Zm5.6 14.3c-.24.66-1.4 1.27-1.93 1.32-.5.05-1.02.24-3.4-.72-2.87-1.17-4.7-4.05-4.85-4.24-.14-.19-1.16-1.54-1.16-2.94 0-1.4.73-2.08 1-2.36.26-.28.57-.35.76-.35h.55c.18 0 .43-.03.66.5.24.57.8 1.98.87 2.12.07.15.12.32.02.5-.1.2-.15.32-.3.5-.15.17-.3.38-.44.51-.15.14-.3.3-.13.6.17.3.75 1.24 1.62 2.01 1.12.99 2.05 1.3 2.35 1.45.3.14.47.12.65-.08.17-.2.74-.86.94-1.16.2-.3.4-.24.66-.15.27.1 1.7.8 2 .95.28.14.47.21.54.33.07.13.07.72-.16 1.38Z"/></svg>
      </button>`;
  }
})();
