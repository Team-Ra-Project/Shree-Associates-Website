# Shree Associates — Multi-Page Website

A complete, responsive **multi-page** frontend website for **Shree Associates** (Legal, Society &amp; Property Compliance Consultant), built from the content in the original digital card and the additional services/testimonials briefs supplied. Every page shares one header, one footer and one design system.

## Pages

| Page | File | Contents |
|---|---|---|
| Home | `index.html` | Hero, trust stats, services preview, why-us preview, team preview, reviews preview, offices preview, final CTA |
| About Us | `about.html` | Founder bio, credentials, experience stats, vision, mission, company promise |
| Services | `services.html` | All services in four tabbed categories: Society, Property, Legal, Redevelopment |
| Why Us | `why-us.html` | Vision, mission, and the six reasons clients choose Shree Associates |
| Our Team | `team.html` | All seven team members with photos, designations and descriptions |
| Client Reviews | `reviews.html` | The full 60-review dataset with search, service filter, rating filter, load-more pagination and a featured-reviews strip |
| Case Studies | `case-studies.html` | The four existing case studies, Problem → Solution → Result |
| Ulwe | `ulwe.html` | Local page: Ulwe office, the 10 services, and Problem → Professional Assistance cards |
| Contact | `contact.html` | All three offices, contact details, enquiry form, and FAQ |

## Project structure

```
shree-associates-website/
├── index.html
├── about.html
├── services.html
├── why-us.html
├── team.html
├── reviews.html
├── case-studies.html
├── ulwe.html
├── contact.html
├── sitemap.xml
├── robots.txt
├── css/
│   └── style.css            Design system + all page styling, fully responsive
├── js/
│   ├── partials.js           Injects the shared header/footer/floating widgets on every page
│   ├── main.js                Nav, animations, WhatsApp/Email/vCard, contact form, FAQ, service tabs, counters
│   ├── reviews-data.js         The 60-review dataset (reviews.html only) — kept separate from markup/logic
│   └── reviews.js               Renders/filters/paginates reviews.js data (reviews.html only)
├── images/                      Logo, founder & team photos (from the original digital card)
└── README.md                      This file
```

## How the shared header/footer work (no duplicated markup)

`js/partials.js` defines the header and footer HTML **once** and injects it into every page's empty `<header id="siteHeader"></header>` / `<footer id="siteFooter"></footer>` containers when the page loads. Each page only needs:

```html
<body data-page="about">   <!-- tells partials.js which nav link to mark active -->
  <header class="site-header" id="siteHeader"></header>
  <main id="main-content"> ...page content... </main>
  <footer class="site-footer" id="siteFooter"></footer>
  <div id="siteWidgets"></div>  <!-- back-to-top + WhatsApp floating button -->
  <script src="js/partials.js"></script>
  <script src="js/main.js"></script>
</body>
```

To change the navigation, logo, or footer copy for the **entire site**, edit `js/partials.js` in one place — every page picks it up automatically. This uses plain `<script>` tags (not `fetch`), so it works both from a local double-click (`file://`) and from any static host, with no build step.

> Note: because the header/footer are injected by JavaScript, they appear a moment after the page starts rendering rather than being present in the raw HTML. This is a deliberate trade-off for zero-build-step reusability; if you later add a build/templating step, the same `partials.js` content can be pre-rendered into each HTML file instead.

## Running it locally

```bash
npx serve .
```
(or simply double-click `index.html` — it works from `file://` too, since nothing depends on `fetch`.)

## The Reviews page (`reviews.html`)

- **Data**: `js/reviews-data.js` holds the exact 60 reviews supplied — nothing added, removed, reworded, or re-rated. It's a plain array kept separate from HTML/CSS so it's easy to update.
- **Rendering**: `js/reviews.js` renders cards dynamically instead of 60 hand-written HTML blocks. It:
  - Shows a real count badge ("60 Client Reviews", computed from the data, not hard-coded)
  - Builds the **service filter** options automatically from the distinct `service` values in the data
  - Builds the **rating filter** options only for ratings actually present in the data (this dataset has 5★, 4★, and 2★ — no 1★ or 3★ options are shown, since none exist)
  - Renders real stars per review (e.g. a rating of 2 shows `★★☆☆☆`, not `★★★★★`)
  - Shows the actual review text; when a review's text is empty, it shows the reviewer, rating and service instead of an empty quote box
  - Shows 9 reviews initially with a **Load More** button (9 more per click) and a running "Showing X of 60 reviews" line
  - Has a small **featured** strip at the top (the 3 reviews with the longest text, chosen programmatically — not labelled "best" and not re-ranking customers)
  - Supports **search** across name, society, service and review text, combined with the two filters

## Contact form: WhatsApp + Email (frontend-only)

The enquiry form (`#contactForm` in `contact.html`) has **no backend and no database** — just HTML, CSS, JavaScript and Web3Forms. Fields: Name, Phone, Email, Subject / Service, Message (all required and validated with specific messages).

| Button | What happens |
|---|---|
| **Send via WhatsApp** | JavaScript builds the message, opens `https://wa.me/<number>?text=...` in a new tab, and shows "WhatsApp opened with your enquiry. Please press Send to submit it." The visitor presses Send in WhatsApp — the site never sends it automatically. |
| **Send via Email** | JavaScript posts the form (`FormData`) to `https://api.web3forms.com/submit`. The button shows "Sending..." and is disabled while the request runs; on success the form resets and a thank-you message appears; on failure a generic error is shown and the technical detail is only logged to the browser console. |

Pressing Enter inside a field never submits the form natively, and a guard prevents duplicate sends while a request is in progress. A hidden honeypot field (`website`) silently ignores bots.

**Configuration** — everything is at the top of `js/main.js`:

```js
const ENQUIRY_WHATSAPP_NUMBER = '919004390564';   // digits only, country code first, no "+"
const WEB3FORMS_ENDPOINT = 'https://api.web3forms.com/submit';
const WEB3FORMS_ACCESS_KEY = 'ec60b2e5-a409-47c4-bc78-6fa8c6b8ed3f';
```

Emails are delivered to whichever inbox is registered for the Web3Forms access key (manage it at web3forms.com).

Note: the site-wide WhatsApp buttons (floating button, "WhatsApp Us", etc.) use the separate `WHATSAPP_NUMBER` constant just above it. The enquiry form has its own number so the two can differ.

## WhatsApp, Email &amp; Save Contact

- **WhatsApp** buttons/links (`data-whatsapp`) open `wa.me` with the real Shree Associates number and a message that names the specific service clicked (e.g. clicking "Enquire Now" on the Conveyance card opens WhatsApp already mentioning Conveyance).
- **Email Us** buttons (`data-email`) open the visitor's email client addressed to `shree.associates.entp@gmail.com`.
- **Save Contact** (Home page) downloads a `.vcf` vCard with the founder's real contact details.
- On the Services page, each "Enquire Now" link instead sends the visitor to `contact.html?service=...`, which **pre-selects that service** in the enquiry form's dropdown.

All of this logic lives in `js/main.js` in a small config block (`WHATSAPP_NUMBER`, `CONTACT_EMAIL`, and the enquiry-form settings) if a number or address ever needs to change — one edit updates every page.

## Content source

All company information, service descriptions, team details, office addresses, phone numbers, FAQ answers, case studies and the 60 client reviews are taken directly from the supplied digital card and briefs. No information was invented, and the review dataset was used exactly as supplied (verified: 60 entries, no duplicates, ratings/services/text unchanged, blank reviews stay blank).

## Testing performed

Every page was checked for balanced HTML tags and working internal links/asset paths. `js/main.js`, `js/partials.js`, `js/reviews.js` and `js/reviews-data.js` were syntax-checked, and the site was exercised end-to-end in a headless DOM (real `<script src>` loading, not a shortcut) to confirm: header/footer injection and correct active-nav-link per page, WhatsApp/vCard/FAQ/contact-form behaviour, service tab switching, stat counters, and the reviews page's search, service filter, rating filter, load-more pagination, star rendering, and empty-review fallback — all producing the expected results with no console errors.


## Home page additions

- **Problems We Solve** (`#problems-we-solve`, sits right after the intro line and before Services): eight problem → solution cards, an audience line, the nine service-area chips, and a CTA to the contact page / WhatsApp.
- **Our Ulwe Office** (`#ulwe-office`): address, phone, hours and Get Directions / Call Us / WhatsApp Us buttons.
- **Why Choose Us image**: `images/why-choose-us.jpg` (+ `.webp`, served through `<picture>`). It is 1000 × 1250 px (4:5), so it fills its frame without cropping the text baked into the artwork.

## Local SEO for Ulwe

Ulwe gets stronger emphasis, but nothing says it is the only office or service area. Kamothe, Fort and all Navi Mumbai / Mumbai references remain.

- New page `ulwe.html` with its own title, meta description, canonical URL, Open Graph tags and structured data. It is in the main navigation.
- Home hero, Home services line, Contact page, Services page and the footer (on every page) reference Ulwe naturally, with internal links between them.
- **Structured data** (`application/ld+json`) on Home, Contact and Ulwe: the organisation plus each office as a `LegalService` with the verified address, phone and hours (Mon–Sun, 10:00–18:00). No ratings, review counts, coordinates, prices or awards are included.
- `sitemap.xml` and `robots.txt` are included.

### Replace the placeholder domain before going live

The original digital card contains no website address, so canonical URLs, Open Graph URLs, structured data, `sitemap.xml` and `robots.txt` use the placeholder **`https://www.shreeassociates.in`**. Search the project for that string and replace it with the real domain (files: `index.html`, `contact.html`, `ulwe.html`, `sitemap.xml`, `robots.txt`).

### Maps links

The card has one Google Maps short link (used for the Kamothe / general "Get Directions") and the Google Business listing link (used for reviews). It has no Ulwe-specific link, so the Ulwe "Get Directions" buttons open Google Maps with the verified Ulwe address as the search text. If you have a share link for the Ulwe office, replace the `google.com/maps/search/?api=1&query=...` URL in `index.html`, `ulwe.html` and `contact.html`.

### Navigation and WhatsApp notes

- The nav now has nine links; the mobile menu switches on below 980px so nothing crowds on tablets.
- `services.html#society`, `#property`, `#legal` and `#redevelopment` open that tab directly.
- WhatsApp buttons can carry a custom message with `data-wa-message="..."`. All WhatsApp buttons use the single number configured in `js/main.js`.
