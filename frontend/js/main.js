/* ==========================================================================
   SHREE ASSOCIATES — main.js
   Shared behaviour for every page: header, mobile nav, scroll reveal,
   ripple/tilt, WhatsApp/Email/vCard actions, contact form, FAQ accordion,
   service tabs, animated stat counters, back-to-top. Each block checks the
   relevant element exists before running, so one file safely serves every page.
   ========================================================================== */

document.addEventListener('DOMContentLoaded', () => {

  /* ---------- Config ---------- */
  const WHATSAPP_NUMBER = '919769573939'; // Shree Associates WhatsApp number (Kamothe office)
  const CONTACT_EMAIL = 'shree.associates.entp@gmail.com';
  // Base URL of the Spring Boot backend (see /backend). During local development the
  // backend runs on http://localhost:8080; update API_BASE_URL when you deploy the
  // backend to a real domain (e.g. https://api.shreeassociates.com).
  const API_BASE_URL = window.SHREE_API_BASE_URL || 'http://localhost:8080';
  const ENQUIRY_ENDPOINT = `${API_BASE_URL}/api/enquiries`; // falls back to mailto if unreachable

  /* ---------- Sticky header + back-to-top visibility ---------- */
  const header = document.getElementById('siteHeader');
  const backToTop = document.getElementById('backToTop');
  const onScroll = () => {
    if (header) header.classList.toggle('scrolled', window.scrollY > 12);
    if (backToTop) backToTop.classList.toggle('visible', window.scrollY > 500);
  };
  window.addEventListener('scroll', onScroll);
  onScroll();
  if (backToTop) backToTop.addEventListener('click', () => window.scrollTo({ top: 0, behavior: 'smooth' }));

  /* ---------- Mobile nav toggle ---------- */
  const navToggle = document.getElementById('navToggle');
  const mainNav = document.getElementById('mainNav');
  if (navToggle && mainNav) {
    navToggle.addEventListener('click', () => {
      const isOpen = mainNav.classList.toggle('open');
      navToggle.classList.toggle('open', isOpen);
      navToggle.setAttribute('aria-expanded', isOpen);
    });
    mainNav.querySelectorAll('a').forEach(link => {
      link.addEventListener('click', () => {
        mainNav.classList.remove('open');
        navToggle.classList.remove('open');
        navToggle.setAttribute('aria-expanded', 'false');
      });
    });
  }

  /* ---------- Soft page transition between internal pages ----------
     A brief cross-fade on <main> before following a same-site link, so
     moving between the 8 pages feels like one connected site rather than
     a hard page-cut. Arrival uses the #main-content "pageIn" animation
     already defined in style.css, so no extra work is needed there. */
  const mainContent = document.getElementById('main-content');
  if (mainContent) {
    document.addEventListener('click', (e) => {
      const link = e.target.closest('a');
      if (!link) return;
      if (link.hasAttribute('data-whatsapp') || link.hasAttribute('data-email')) return;
      const href = link.getAttribute('href');
      if (!href || href.startsWith('#') || href.startsWith('mailto:') || href.startsWith('tel:')) return;
      if (link.target === '_blank' || link.hasAttribute('download')) return;
      if (e.metaKey || e.ctrlKey || e.shiftKey || e.altKey) return; // let the browser open a new tab as usual
      let url;
      try { url = new URL(href, window.location.href); } catch (err) { return; }
      if (url.origin !== window.location.origin) return; // external link — no transition, navigate normally
      e.preventDefault();
      mainContent.classList.add('page-leaving');
      setTimeout(() => { window.location.href = href; }, 240);
    });
  }

  /* ---------- Scroll reveal with cascading stagger ----------
     Card grids get a "stamp settling into place" scale-reveal (fits the
     seal/registry motif); sequential lists (case studies, FAQ) keep a
     simple upward reveal so they read top-to-bottom. */
  const scaleStaggerGroups = ['.service-grid', '.team-grid', '.office-grid', '.preview-grid-4'];
  const upStaggerGroups = ['.case-list', '.accordion', '.mission-grid'];
  scaleStaggerGroups.forEach(selector => {
    document.querySelectorAll(`${selector} > *`).forEach((child, i) => {
      child.classList.add('fade-scale');
      child.style.setProperty('--delay', `${Math.min(i * 80, 400)}ms`);
    });
  });
  upStaggerGroups.forEach(selector => {
    document.querySelectorAll(`${selector} > *`).forEach((child, i) => {
      child.classList.add('fade-in');
      child.style.setProperty('--delay', `${Math.min(i * 80, 400)}ms`);
    });
  });

  const REVEAL_SELECTOR = '.fade-in, .fade-left, .fade-right, .fade-scale';
  let revealObserver;
  if ('IntersectionObserver' in window) {
    revealObserver = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          entry.target.classList.add('in-view');
          revealObserver.unobserve(entry.target);
        }
      });
    }, { threshold: 0.12, rootMargin: '0px 0px -40px 0px' });
    document.querySelectorAll(REVEAL_SELECTOR).forEach(item => revealObserver.observe(item));
  } else {
    // Very old browser without IntersectionObserver: skip the reveal animation, show content immediately.
    document.querySelectorAll(REVEAL_SELECTOR).forEach(item => item.classList.add('in-view'));
  }
  window.shreeRevealObserver = revealObserver; // exposed so reviews.js can observe cards it renders later (may be undefined)

  /* ---------- Button ripple effect ---------- */
  document.addEventListener('click', (e) => {
    const btn = e.target.closest('.ripple');
    if (!btn) return;
    const rect = btn.getBoundingClientRect();
    const ripple = document.createElement('span');
    const size = Math.max(rect.width, rect.height);
    ripple.className = 'ripple-effect';
    ripple.style.width = ripple.style.height = `${size}px`;
    ripple.style.left = `${e.clientX - rect.left - size / 2}px`;
    ripple.style.top = `${e.clientY - rect.top - size / 2}px`;
    btn.appendChild(ripple);
    setTimeout(() => ripple.remove(), 650);
  });

  /* ---------- Subtle card tilt on hover (desktop only) ---------- */
  if (window.matchMedia && window.matchMedia('(hover: hover)').matches) {
    document.querySelectorAll('[data-tilt]').forEach(el => {
      el.addEventListener('mousemove', (e) => {
        const rect = el.getBoundingClientRect();
        const x = (e.clientX - rect.left) / rect.width - 0.5;
        const y = (e.clientY - rect.top) / rect.height - 0.5;
        el.style.transform = `perspective(900px) rotateX(${(-y * 3).toFixed(2)}deg) rotateY(${(x * 3).toFixed(2)}deg)`;
      });
      el.addEventListener('mouseleave', () => { el.style.transform = ''; });
    });
  }

  /* ---------- Service category tabs (services.html) ---------- */
  const tabs = document.querySelectorAll('.service-tab');
  const panels = document.querySelectorAll('.service-panel');
  const tabsWrap = document.getElementById('serviceTabs');
  if (tabs.length) {
    // Sliding underline indicator: a small bar that glides beneath the
    // active tab rather than the tab just re-styling in place.
    let indicator = tabsWrap ? tabsWrap.querySelector('.tab-indicator') : null;
    if (tabsWrap && !indicator) {
      indicator = document.createElement('span');
      indicator.className = 'tab-indicator';
      tabsWrap.appendChild(indicator);
    }
    const moveIndicatorTo = (tab) => {
      if (!indicator || !tabsWrap) return;
      const wrapRect = tabsWrap.getBoundingClientRect();
      const tabRect = tab.getBoundingClientRect();
      indicator.style.left = `${tabRect.left - wrapRect.left}px`;
      indicator.style.width = `${tabRect.width}px`;
    };
    const activeTab = tabsWrap ? tabsWrap.querySelector('.service-tab.active') : tabs[0];
    // Position after layout has settled (fonts, etc.) rather than instantly.
    requestAnimationFrame(() => moveIndicatorTo(activeTab || tabs[0]));
    window.addEventListener('resize', () => {
      const current = tabsWrap.querySelector('.service-tab.active') || tabs[0];
      moveIndicatorTo(current);
    });

    tabs.forEach(tab => {
      tab.addEventListener('click', () => {
        const target = tab.dataset.tab;
        tabs.forEach(t => { t.classList.toggle('active', t === tab); t.setAttribute('aria-selected', t === tab); });
        panels.forEach(p => {
          const show = p.dataset.panel === target;
          p.classList.toggle('active', show);
          p.hidden = !show;
        });
        moveIndicatorTo(tab);
      });
    });
  }

  /* ---------- Animated stat counters (index.html) ---------- */
  const counters = document.querySelectorAll('.stat-num');
  const animateCounter = (el) => {
    const target = parseInt(el.dataset.count, 10);
    const suffix = el.dataset.suffix || '';
    const isPlain = el.dataset.plain === 'true';
    const duration = isPlain ? 900 : 1400;
    const start = performance.now();
    const step = (now) => {
      const progress = Math.min((now - start) / duration, 1);
      const eased = 1 - Math.pow(1 - progress, 3);
      const value = Math.round(target * eased);
      el.textContent = `${value}${suffix}`;
      if (progress < 1) requestAnimationFrame(step);
    };
    requestAnimationFrame(step);
  };
  if (counters.length) {
    if ('IntersectionObserver' in window) {
      const counterObserver = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
          if (entry.isIntersecting) {
            animateCounter(entry.target);
            counterObserver.unobserve(entry.target);
          }
        });
      }, { threshold: 0.5 });
      counters.forEach(c => counterObserver.observe(c));
    } else {
      counters.forEach(c => animateCounter(c));
    }
  }

  /* ---------- WhatsApp buttons (dynamic message per service) ---------- */
  document.addEventListener('click', (e) => {
    const btn = e.target.closest('[data-whatsapp]');
    if (!btn) return;
    e.preventDefault();
    const service = btn.dataset.waService || 'general enquiry';
    const message = service === 'general enquiry'
      ? 'Hello Shree Associates, I would like to enquire about your legal/property/society services.'
      : `Hello Shree Associates, I would like to enquire about ${service} services.`;
    window.open(`https://wa.me/${WHATSAPP_NUMBER}?text=${encodeURIComponent(message)}`, '_blank', 'noopener');
  });

  /* ---------- Email buttons ---------- */
  document.addEventListener('click', (e) => {
    const btn = e.target.closest('[data-email]');
    if (!btn) return;
    e.preventDefault();
    const subject = btn.dataset.emailSubject || 'Website Enquiry - Shree Associates';
    const body = 'Hello Shree Associates,\n\nI would like to enquire about your services. Please find my requirement below:\n\n[Please describe your requirement here]\n\nThank you.';
    window.location.href = `mailto:${CONTACT_EMAIL}?subject=${encodeURIComponent(subject)}&body=${encodeURIComponent(body)}`;
  });

  /* ---------- vCard download ---------- */
  const vcardBtn = document.getElementById('vcardBtn');
  if (vcardBtn) {
    vcardBtn.addEventListener('click', () => {
      const vcard = [
        'BEGIN:VCARD',
        'VERSION:3.0',
        'N:Ranjane;Shrikant;;;',
        'FN:Shrikant Prabhakar Ranjane',
        'ORG:Shree Associates',
        'TITLE:Founder & Proprietor',
        'TEL;TYPE=WORK,VOICE:+91-97695-73939',
        'TEL;TYPE=WORK,VOICE:+91-97693-73939',
        'EMAIL:shree.associates.entp@gmail.com',
        'ADR;TYPE=WORK:;;Kohinoor Residency CHS Ltd, Office No. 01, Plot No. 2, Sec. 11, Opp. AXIS Bank, Kamothe;Navi Mumbai;Maharashtra;410209;India',
        'URL:https://maps.app.goo.gl/RSpfndnPwmnnkJVf8',
        'END:VCARD'
      ].join('\n');
      const blob = new Blob([vcard], { type: 'text/vcard' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'shree-associates.vcf';
      document.body.appendChild(a);
      a.click();
      a.remove();
      URL.revokeObjectURL(url);
    });
  }

  /* ---------- FAQ: data-driven accordion (contact.html) ---------- */
  const faqData = [
    { q: 'What services does Shree Associates provide?', a: 'We offer end-to-end legal and property compliance services: society registration (Housing, Credit, Seva societies, Trusts & Mandals), conveyance and deemed conveyance, CIDCO flat/shop/plot transfer, stamp duty & registration, society accounting and statutory audit, redevelopment legal advisory, and civil, criminal, consumer, and co-operative society dispute resolution.' },
    { q: 'How long does housing society registration take?', a: 'With complete documentation, registration typically takes 2 to 3 months. We manage the full process — paperwork, government liaison, and follow-up — to avoid unnecessary delays.' },
    { q: 'What is Deemed Conveyance and do I need it?', a: "Deemed Conveyance transfers land and building ownership from the builder to the society even without the builder's cooperation. It is essential for societies whose builders have delayed or refused to execute a standard conveyance deed." },
    { q: 'Can you help with CIDCO flat, shop, or plot transfers?', a: 'Yes, we manage the complete CIDCO transfer process including lease matters, NOC, documentation, and mortgage clearance for flats, shops, and plots, ensuring a smooth and legally sound transfer.' },
    { q: 'Do you provide society audit and accounting services?', a: 'Yes, our team handles society accounts management, statutory audit, monthly billing, financial reporting, and administrative support, keeping your society fully compliant and transparent.' },
    { q: 'How can Shree Associates help with redevelopment?', a: 'We provide legal advisory throughout the redevelopment process, including PMC appointment guidance, developer agreement drafting and review, and protecting member interests at every stage.' },
    { q: 'What areas do you serve and where are your offices?', a: 'We serve Navi Mumbai and Mumbai, with offices in Kamothe, Ulwe, and Fort (Mumbai), open Monday to Sunday from 10:00 AM to 6:00 PM. We have assisted over 1000 societies across the region since 2006.' },
    { q: 'Do you handle individual property matters, or only societies?', a: 'Both. While a large part of our work involves cooperative societies, we also assist individual property owners with stamp duty, registration, transfers, and legal advisory.' },
    { q: 'What documents do I need to get started?', a: 'Requirements vary by service, but generally include society formation documents, prior agreements, identity proofs, and property-related paperwork. Our team will guide you on the exact list for your specific case.' },
    { q: 'How do I get in touch with Shree Associates?', a: 'You can reach us via the WhatsApp or Email buttons above, call any of our three offices directly, or fill out the contact form below — our team typically responds within 24 hours.' }
  ];
  const accordion = document.getElementById('accordion');
  if (accordion) {
    faqData.forEach((item, i) => {
      const el = document.createElement('div');
      el.className = 'accordion-item fade-in';
      el.style.setProperty('--delay', `${Math.min(i * 60, 400)}ms`);
      el.innerHTML = `
        <button class="accordion-question" aria-expanded="false">
          <span>${item.q}</span>
          <span class="accordion-icon" aria-hidden="true"></span>
        </button>
        <div class="accordion-answer"><p>${item.a}</p></div>`;
      accordion.appendChild(el);
      if (revealObserver) revealObserver.observe(el);
    });

    accordion.addEventListener('click', (e) => {
      const question = e.target.closest('.accordion-question');
      if (!question) return;
      const item = question.closest('.accordion-item');
      const answer = item.querySelector('.accordion-answer');
      const isOpen = item.classList.contains('open');

      accordion.querySelectorAll('.accordion-item.open').forEach(openItem => {
        if (openItem !== item) {
          openItem.classList.remove('open');
          openItem.querySelector('.accordion-question').setAttribute('aria-expanded', 'false');
          openItem.querySelector('.accordion-answer').style.maxHeight = null;
        }
      });

      if (isOpen) {
        item.classList.remove('open');
        question.setAttribute('aria-expanded', 'false');
        answer.style.maxHeight = null;
      } else {
        item.classList.add('open');
        question.setAttribute('aria-expanded', 'true');
        answer.style.maxHeight = `${answer.scrollHeight}px`;
      }
    });
  }

  /* ---------- Contact form: real submission to backend, graceful fallback ---------- */
  const contactForm = document.getElementById('contactForm');
  const formNote = document.getElementById('formNote');
  const submitBtn = document.getElementById('submitBtn');

  if (contactForm) {
    contactForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const formData = new FormData(contactForm);
      const payload = {
        name: formData.get('name')?.trim(),
        email: formData.get('email')?.trim(),
        phone: formData.get('phone')?.trim(),
        service: formData.get('service')?.trim() || 'Not specified',
        message: formData.get('message')?.trim(),
        website: formData.get('website')?.trim() || '' // honeypot field, must stay empty — hidden from real users via CSS
      };

      if (!payload.name || !payload.email || !payload.phone || !payload.message) {
        formNote.textContent = 'Please fill in all required fields.';
        formNote.className = 'form-note error';
        return;
      }

      submitBtn.disabled = true;
      submitBtn.querySelector('span').textContent = 'Sending…';
      formNote.textContent = '';
      formNote.className = 'form-note';

      try {
        const response = await fetch(ENQUIRY_ENDPOINT, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload)
        });

        let data = null;
        try { data = await response.json(); } catch (_) { /* non-JSON error body */ }

        if (!response.ok) {
          // Server understood the request but rejected it (validation error, duplicate,
          // rate limit, etc.) — show its message rather than falling back to email,
          // since the enquiry itself may be invalid rather than the server being unreachable.
          formNote.textContent = (data && data.message) || 'We could not submit your enquiry. Please check the form and try again.';
          formNote.className = 'form-note error';
          return;
        }

        formNote.textContent = (data && data.message) || 'Thank you. Your enquiry has been submitted successfully. Our team will get back to you soon.';
        formNote.className = 'form-note success';
        contactForm.reset();
      } catch (err) {
        // Network-level failure (server unreachable, offline, CORS, timeout) — fall back to email.
        const subject = encodeURIComponent(`Website Enquiry - Shree Associates (${payload.service})`);
        const body = encodeURIComponent(
          `Name: ${payload.name}\nEmail: ${payload.email}\nPhone: ${payload.phone}\nService: ${payload.service}\nMessage: ${payload.message}\nSubmitted: ${new Date().toISOString()}`
        );
        window.location.href = `mailto:${CONTACT_EMAIL}?subject=${subject}&body=${body}`;
        formNote.textContent = "We couldn't reach our server, so we've opened an email for you to send instead — please hit send in your email app to complete your enquiry.";
        formNote.className = 'form-note error';
      } finally {
        submitBtn.disabled = false;
        submitBtn.querySelector('span').textContent = 'Send Enquiry';
      }
    });
  }

  /* ---------- Pre-fill contact form's service field from a query string,
     e.g. contact.html?service=Society%20Registration (used by "Enquire Now"
     links on the services page) ---------- */
  const serviceSelect = document.getElementById('serviceSelect');
  if (serviceSelect) {
    const params = new URLSearchParams(window.location.search);
    const wanted = params.get('service');
    if (wanted) {
      const match = Array.from(serviceSelect.options).find(o => o.value === wanted);
      if (match) serviceSelect.value = wanted;
    }
  }

  /* ---------- Footer year ---------- */
  const yearEl = document.getElementById('year');
  if (yearEl) yearEl.textContent = new Date().getFullYear();

});
