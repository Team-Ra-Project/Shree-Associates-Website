# Shree Associates — Website Enquiry Backend

This project has three parts, kept deliberately separate:

```
shree-associates-website/
├── frontend/     ← your existing static website (unchanged design/content)
├── backend/      ← new Java 17 + Spring Boot REST API + WhatsApp integration
└── database/     ← MySQL schema (schema.sql)
```

The frontend's enquiry form on `contact.html` now submits to the backend
(`POST /api/enquiries`) instead of only opening an email draft. The backend
validates and stores the enquiry, then sends a WhatsApp notification to the
Shree Associates office number using the official **WhatsApp Business Cloud
API**. If WhatsApp sending fails for any reason, the enquiry is still saved
(marked `FAILED`) — nothing is ever silently lost.

---

## 0. Quick Start — What to change before running, and how to run

This section assumes you're serving the frontend the way you're currently
doing it: opening **`frontend/index.html` via a static server such as VS
Code "Live Server", launched from the project root**, so the site loads at
`http://127.0.0.1:5500/frontend/index.html`. Everything below matches that
setup out of the box — no CORS or path changes needed for it specifically.

### 0.1 What you MUST change before running

| # | What | Where | Why |
|---|---|---|---|
| 1 | WhatsApp credentials: `WHATSAPP_ACCESS_TOKEN`, `WHATSAPP_PHONE_NUMBER_ID`, `WHATSAPP_RECIPIENT_NUMBER` | Environment variables (see §0.2 step 2 — `.env` alone isn't enough) | Currently blank placeholders — required for any WhatsApp notification to send. `WHATSAPP_RECIPIENT_NUMBER` must be the real Shree Associates WhatsApp number; it was never invented for you. |
| 2 | Create + get approved the WhatsApp message template (`website_enquiry_notification`) | Meta Business Manager → WhatsApp Manager → Message Templates | Meta requires an approved template for this kind of business-initiated notification (see §6). Without it, sends will fail and enquiries will be stored with `whatsapp_status = FAILED`. |
| 3 | MySQL credentials: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Environment variables (see §0.2 step 2) | Defaults assume `root` with no password on `localhost:3306` — this almost never matches a real MySQL install, so you must set your real username/password one of the ways in §0.2, every time, unless you use the "permanent" or "hardcode" option there. |
| 4 | Run the schema (or let Hibernate auto-create it) | `database/schema.sql` | Creates the `shree_associates` database and `enquiries` table. Optional if `DDL_AUTO=update` (the default) — Hibernate creates it for you on first run. |
| 5 | `CORS_ALLOWED_ORIGINS` — only if you serve the frontend differently | `backend/.env` / `application.properties` | The default already includes `http://127.0.0.1:5500` and `http://localhost:5500`, so **no change is needed** if you keep serving from Live Server on port 5500 as described above. Change this only if you use a different port, a different tool, or deploy to a real domain. |
| 6 | `API_BASE_URL` — only for production | `frontend/js/main.js` (top of file) | Defaults to `http://localhost:8080`, which is correct for local testing against the backend on your machine. Change this (or set `window.SHREE_API_BASE_URL`) only once you deploy the backend somewhere other than your own laptop. |

Nothing else needs to change for a first local run.

### 0.2 How to run it, step by step

**1. Start MySQL and create the database**
```bash
mysql -u root -p < database/schema.sql
```

**2. Configure the backend's secrets**

⚠️ **Important — `.env` is not loaded automatically.** Copying
`.env.example` to `.env` and filling in values does **nothing by itself** —
Spring Boot only reads real environment variables (or a properties file),
never a `.env` file directly. `.env` is just a convenient place to *keep*
your values so you remember what to set; you still have to load them one
of these ways:

<details>
<summary><b>Windows (Command Prompt) — per session</b></summary>

```cmd
set DB_USERNAME=root
set DB_PASSWORD=your_real_mysql_password
set WHATSAPP_ACCESS_TOKEN=your_real_token
set WHATSAPP_PHONE_NUMBER_ID=your_real_phone_number_id
set WHATSAPP_RECIPIENT_NUMBER=919XXXXXXXXX
mvn spring-boot:run
```
These only last for that one Command Prompt window — close it, and you'll need to `set` them again next time.
</details>

<details>
<summary><b>Windows — permanently (recommended if you don't want to retype every time)</b></summary>

1. Press the **Windows key**, search **"Environment Variables"**, open **"Edit the system environment variables"**.
2. Click **Environment Variables...** → under **User variables**, click **New...** for each of: `DB_USERNAME`, `DB_PASSWORD`, `WHATSAPP_ACCESS_TOKEN`, `WHATSAPP_PHONE_NUMBER_ID`, `WHATSAPP_RECIPIENT_NUMBER`.
3. Click OK on all dialogs, then **close and reopen** Command Prompt (it only picks up new variables in new windows).

After this, every new terminal already has these values — no `set` needed again.
</details>

<details>
<summary><b>Mac / Linux</b></summary>

```bash
cd backend
cp .env.example .env
# edit .env with your real values
export $(grep -v '^#' .env | xargs)
mvn spring-boot:run
```
</details>

<details>
<summary><b>Simplest for local-only use, any OS: hardcode the defaults directly</b></summary>

If you don't want to deal with environment variables on your own machine at
all, edit `backend/src/main/resources/application.properties` and put your
real values after the `:` in each placeholder:

```properties
spring.datasource.password=${DB_PASSWORD:your_real_mysql_password}
whatsapp.access-token=${WHATSAPP_ACCESS_TOKEN:your_real_token}
```

This works with a plain `mvn spring-boot:run` and no environment variables
at all. Just don't push this file with real secrets in it to a public GitHub
repo — keep using environment variables for anything shared or deployed.
</details>

Or, if you use an IDE (IntelliJ/Eclipse/VS Code), set the same variables in its **Run Configuration → Environment Variables** panel instead — that's remembered per-project without touching your OS settings at all.

**3. Build and run the backend**
```bash
mvn clean install
mvn spring-boot:run
```
Confirm it's up: open `http://localhost:8080/actuator/health` — should say `{"status":"UP"}`.

**4. Serve the frontend exactly as you are now**

In VS Code, right-click `frontend/index.html` → **Open with Live Server**, launched from the project root, so it opens at:
```
http://127.0.0.1:5500/frontend/index.html
```
This origin (`http://127.0.0.1:5500`) is already in the backend's default CORS allow-list, so the enquiry form will be able to call the backend with no further configuration.

**5. Submit a test enquiry**

Go to the Contact page, fill in the form, and submit. Then check, in order:
- the browser console / Network tab for the `POST /api/enquiries` call (should return `201`),
- the backend logs,
- the `enquiries` table in MySQL (a new row, with `whatsapp_status` = `SENT` or `FAILED`),
- the configured WhatsApp number for the actual notification.

If `whatsapp_status` shows `FAILED`, the enquiry is still safely saved — check the backend logs for the reason (usually an unapproved template, wrong `WHATSAPP_PHONE_NUMBER_ID`, or an expired token).

---



| Tool | Version |
|---|---|
| Java (JDK) | 17+ |
| Maven | 3.9+ (or use the wrapper if you add one) |
| MySQL | 8.0+ |
| A Meta / Facebook Developer account with a WhatsApp Business app | — |

---

## 2. Project structure (backend)

```
backend/
├── src/main/java/com/shreeassociates/
│   ├── controller/    EnquiryController          → POST /api/enquiries
│   ├── service/       EnquiryService              → validation → save → notify
│   │                   WhatsAppNotificationService → dedicated WhatsApp Cloud API client
│   ├── repository/    EnquiryRepository           → Spring Data JPA
│   ├── entity/        Enquiry                     → JPA entity / DB row
│   ├── dto/            EnquiryRequest / EnquiryResponse  → API contracts (entities never exposed directly)
│   ├── config/         CorsConfig, WhatsAppProperties, RestTemplateConfig, RateLimitFilter
│   └── exception/      GlobalExceptionHandler + custom exceptions
├── src/main/resources/application.properties
├── src/test/java/...   EnquiryControllerTest (MockMvc + H2, WhatsApp mocked)
├── pom.xml
├── .env.example
└── .gitignore
```

---

## 3. Database setup

1. Make sure MySQL is running locally.
2. Create the database and table:
   ```bash
   mysql -u root -p < database/schema.sql
   ```
   This creates the `shree_associates` database and the `enquiries` table with columns:
   `id, name, phone, email, service, subject, message, created_at, whatsapp_status, whatsapp_message_id, client_ip`.

   Alternatively, for local development you can skip this step: with
   `spring.jpa.hibernate.ddl-auto=update` (the default in `application.properties`),
   Hibernate will create/update the table automatically the first time you run the
   backend. For production, run `schema.sql` yourself and set `DDL_AUTO=validate`
   so the app never silently alters your production schema.

---

## 4. Environment variables

All secrets are read from environment variables — **never hard-coded** and
**never placed in frontend HTML/CSS/JS**.

Copy the example file and fill in real values:

```bash
cd backend
cp .env.example .env
```

Then either export them in your shell, set them in your IDE's run
configuration, or use a tool like `direnv`. `.env` itself is git-ignored and
is never read directly by Spring Boot — it's just a convenient place to keep
the values you'll export.

| Variable | Purpose |
|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | MySQL connection |
| `WHATSAPP_ACCESS_TOKEN` | Access token for the WhatsApp Business Cloud API |
| `WHATSAPP_PHONE_NUMBER_ID` | The **Phone Number ID** (not the phone number) of your WhatsApp sender, from Meta's App Dashboard |
| `WHATSAPP_RECIPIENT_NUMBER` | The Shree Associates WhatsApp number that should **receive** enquiry notifications, in E.164 digits without `+` (e.g. `9198XXXXXXXX`) — provide this yourself |
| `WHATSAPP_API_VERSION` | Graph API version, e.g. `v20.0` |
| `WHATSAPP_TEMPLATE_NAME` / `WHATSAPP_TEMPLATE_LANGUAGE` | The approved message template used to send the notification (see §6) |
| `WHATSAPP_USE_TEMPLATE` | `true` (default, recommended) to send via the approved template; `false` to attempt a free-form text message |
| `CORS_ALLOWED_ORIGINS` | Comma-separated list of frontend origins allowed to call the API |
| `RATE_LIMIT_MAX_REQUESTS` / `RATE_LIMIT_WINDOW_SECONDS` | Basic anti-spam rate limiting for the enquiry endpoint |

---

## 5. Running the backend locally

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

The API starts on `http://localhost:8080` by default (`SERVER_PORT`).

Health check: `GET http://localhost:8080/actuator/health`

---

## 6. WhatsApp Business Cloud API setup

The backend uses the **official** WhatsApp Business Cloud API only — no
WhatsApp Web automation, browser scraping, or personal-account tools.

### 6.1 Create the app and get credentials

1. Go to [developers.facebook.com/apps](https://developers.facebook.com/apps) and create (or reuse) an app with the **WhatsApp** product added.
2. Under **WhatsApp → API Setup** you'll find:
   - A **temporary access token** (for quick testing — expires in ~24h) and, for production, a **permanent token** generated via a System User in Meta Business Settings.
   - The **Phone Number ID** of the sender number Meta gives you (this is what goes in `WHATSAPP_PHONE_NUMBER_ID` — it is *not* the same as the phone number itself).
3. Add the real **Shree Associates WhatsApp number** as the recipient by setting `WHATSAPP_RECIPIENT_NUMBER`. (This project does not invent or assume that number — you provide and configure it.)

### 6.2 Why a message template is required

WhatsApp only allows **free-form text** messages to a number that has
messaged your business within the last 24 hours (the "customer service
window"). An enquiry notification is *business-initiated* — the website is
messaging the Shree Associates number, which usually hasn't messaged first —
so Meta requires an **approved message template** for reliable delivery.
`whatsapp.use-template=true` (the default) uses this approach.

### 6.3 Create the template in Meta

In **Meta Business Manager → WhatsApp Manager → Message Templates → Create Template**:

| Field | Value |
|---|---|
| Name | `website_enquiry_notification` (must match `WHATSAPP_TEMPLATE_NAME`) |
| Category | `UTILITY` |
| Language | `English` (`en`) — must match `WHATSAPP_TEMPLATE_LANGUAGE` |
| Body | ```New website enquiry.\nName: {{1}}\nContact: {{2}}\nEmail: {{3}}\nService: {{4}}\nMessage: {{5}}\nReceived: {{6}}``` |

Submit it for review. Meta typically approves utility templates within
minutes to a few hours.

The backend sends the six body variables in this exact order:
`name, phone, email, service, message, received-at` — see
`WhatsAppNotificationService.buildTemplatePayload()`. If you design a
different template (different wording or variable count/order), update that
method to match.

### 6.4 Local testing without an approved template

For quick local testing only, you can set `WHATSAPP_USE_TEMPLATE=false` and
send a free-form text message — but this only works if the recipient number
has sent *your* test WhatsApp number a message in the last 24 hours (open the
chat from the recipient's phone and send anything first). Don't rely on this
in production.

---

## 7. API

### `POST /api/enquiries`

**Request body:**
```json
{
  "name": "Tanvi Thakur",
  "phone": "+91XXXXXXXXXX",
  "email": "customer@email.com",
  "service": "Society Registration",
  "message": "I need assistance with society registration."
}
```

**Success response — `201 Created`:**
```json
{
  "success": true,
  "message": "Your enquiry has been submitted successfully.",
  "enquiryId": 42
}
```

**Validation error — `400 Bad Request`:**
```json
{
  "success": false,
  "message": "Please correct the highlighted fields and try again.",
  "errors": { "email": "Email must be a valid email address" }
}
```

**Duplicate submission — `409 Conflict`**, **rate limited — `429 Too Many Requests`**,
**server/database/WhatsApp-config problem — `5xx`**: all return the same
`{ "success": false, "message": "..." }` shape, with a safe, user-friendly
message. No stack traces, SQL errors, or credentials are ever included in a
response.

The enquiry is saved to the database *before* the WhatsApp call — if
WhatsApp sending fails, the row still exists with `whatsapp_status = FAILED`
so nothing is lost and you can retry manually or add a retry job later.

---

## 8. Connecting the frontend

`frontend/js/main.js` already calls the backend:

```js
const API_BASE_URL = window.SHREE_API_BASE_URL || 'http://localhost:8080';
const ENQUIRY_ENDPOINT = `${API_BASE_URL}/api/enquiries`;
```

- **Local development:** serve `frontend/` with any static server (e.g. VS
  Code "Live Server", or `python -m http.server 5500` from inside
  `frontend/`) and run the backend on `localhost:8080` — no changes needed.
- **Production:** once the backend is deployed, either edit the
  `API_BASE_URL` fallback in `main.js`, or set
  `window.SHREE_API_BASE_URL = 'https://api.yourdomain.com';` in a small
  inline `<script>` before `main.js` loads on each page. Also update
  `CORS_ALLOWED_ORIGINS` on the backend to the real frontend domain — never
  leave it as `*`.

The form (`contact.html`):
- Disables the submit button and shows "Sending…" while the request is in flight.
- On success, shows "Thank you! Your enquiry has been submitted successfully..." without a full page reload.
- On a validation/duplicate/rate-limit error from the server, shows that server message inline.
- On a genuine network failure (server unreachable), falls back to opening a pre-filled `mailto:` draft, as before.
- Includes a hidden honeypot field (`website`) as a first line of anti-spam defence; the backend also rate-limits and checks for duplicate submissions.

---

## 9. Security measures implemented

- Server-side Bean Validation on every field (name, phone, email format, message length) — the frontend's own validation is never trusted alone.
- Input sanitization (`InputSanitizer`) strips HTML tags/control characters from free-text fields before storage/forwarding.
- SQL injection prevented by Spring Data JPA / Hibernate parameter binding (no string-concatenated SQL anywhere).
- CORS restricted to an explicit allow-list (`app.cors.allowed-origins`), never `*`.
- All secrets (DB credentials, WhatsApp token) sourced from environment variables via `${VAR:default}` placeholders — never hard-coded, never in frontend code, never committed (`.gitignore` covers `.env*`).
- Centralized `GlobalExceptionHandler` ensures stack traces, SQL errors, and credentials are never returned to the client.
- Basic per-IP rate limiting (`RateLimitFilter`) on `POST /api/enquiries`.
- Duplicate-submission guard: an identical name+phone+message combination within a 60-second window is rejected with `409 Conflict` instead of creating a second row.
- Honeypot field on the frontend form for lightweight bot filtering.

---

## 10. Testing

Run the automated tests (uses an in-memory H2 database and a mocked WhatsApp service — no real network calls, no real credentials needed):

```bash
cd backend
mvn test
```

`EnquiryControllerTest` covers:
- ✅ Valid enquiry → saved, WhatsApp "sent", `201` returned
- ❌ Invalid email → rejected, nothing saved, WhatsApp never called
- ❌ Missing name → rejected
- ❌ Missing message → rejected
- ⚠️ WhatsApp API failure (mocked) → enquiry still saved, marked `FAILED`, request still returns success to the customer
- 🔁 Duplicate submission (same payload twice, quickly) → second attempt rejected with `409`

### Manual test with `curl`

```bash
curl -i -X POST http://localhost:8080/api/enquiries \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Tanvi Thakur",
    "phone": "+919876543210",
    "email": "tanvi@example.com",
    "service": "Society Registration",
    "message": "I need assistance with society registration."
  }'
```

---

## 11. Production deployment considerations

- **Secrets:** set all environment variables from §4 as real environment variables / secrets on your host (Docker env, systemd `EnvironmentFile`, your PaaS's secrets manager) — never bake them into the JAR or a committed properties file.
- **Database:** use a dedicated least-privilege MySQL user (see the commented-out `CREATE USER` block in `database/schema.sql`) rather than `root`; set `DDL_AUTO=validate` and manage schema changes explicitly through `schema.sql` / a migration tool (Flyway/Liquibase) rather than `update`.
- **HTTPS:** put the backend behind HTTPS (a reverse proxy like Nginx, or your hosting platform's TLS termination) — WhatsApp/Meta and browsers both expect it, and enquiry data includes personal information.
- **CORS:** set `CORS_ALLOWED_ORIGINS` to the exact production frontend domain(s), not `*` and not a `localhost` value.
- **WhatsApp token lifetime:** use a permanent System User access token (not the 24-hour temporary token from API Setup) so notifications don't silently stop working.
- **Rate limiting:** the built-in in-memory limiter is fine for a single instance; behind a load balancer or at higher traffic, move rate limiting to the edge (Nginx, Cloudflare, an API gateway) or swap in a shared-store limiter (e.g. Redis + Bucket4j).
- **Monitoring failed sends:** periodically query `SELECT * FROM enquiries WHERE whatsapp_status = 'FAILED'` (or build a small admin view) so a WhatsApp credential/template problem doesn't go unnoticed — the enquiry is safe in the database either way, but you'll want to know if notifications stop arriving.
- **Backups:** enable regular MySQL backups since `enquiries` is customer data.

---

## 12. What was intentionally left unchanged

Per the project requirements, the existing frontend design, pages, images,
copy, and all other functionality (WhatsApp "click to chat" buttons, email
buttons, vCard download, FAQ accordion, etc.) are untouched. Only the
enquiry form's submission logic in `frontend/js/main.js` and a small hidden
honeypot field in `frontend/contact.html` were modified to integrate with
the new backend. No contact details were invented — the real WhatsApp
recipient number is provided separately via `WHATSAPP_RECIPIENT_NUMBER`.