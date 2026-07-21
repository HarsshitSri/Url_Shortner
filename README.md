# ShortLink — URL Shortener

Resume-grade URL shortener built as a monorepo:

- **Backend:** Java 21, Spring Boot 3.4, Postgres, Flyway, Spring AI (Gemini)
- **Frontend:** HTML, CSS, basic JS
- **Deploy targets:** Vercel (UI) · Railway (API) · Neon (database)

## Features (Phase 1)

- Create short URLs (random 6-char Base62, uniqueness retries)
- `302 Found` redirects
- Metadata lookup by short code
- Soft Gemini URL safety classification (never blocks creation)
- Warnings for:
  - HTTP (non-HTTPS) destinations
  - AI outage: *Looks like our AI assistant is down. We can't verify this URL — proceed at your own risk.*
- Welcome UI with create form, example-link dropdown, and AI product tour (canned fallback)
- Bean validation on create requests
- Global exception handling with a shared error JSON shape
- CORS via `CORS_ALLOWED_ORIGINS`
- Actuator health endpoint

### Explicitly out of scope (Phase 1)

- Auth / ownership
- Pagination, sorting, search, filter (no list endpoint yet)
- Unified success response envelope (`{ data, meta }`) — success payloads return DTOs directly
- Click analytics, custom aliases, rate limiting

## Repository layout

```
Url_Shortner/
├── README.md
├── backend/                 Spring Boot API
│   ├── docker-compose.yml   Local Postgres on host port 5434
│   ├── pom.xml
│   ├── .env.example
│   └── src/main/java/com/urlshortener/
│       ├── config/          App properties + CORS
│       ├── domain/          Entity + enums
│       ├── repository/      Spring Data JPA
│       ├── service/         Create / redirect / short-code gen
│       ├── safety/          Gemini + fallback classifiers
│       ├── tour/            AI / canned tour
│       ├── web/             Controllers + GlobalExceptionHandler
│       │   └── dto/         Request/response DTOs
│       └── exception/
└── frontend/                Static UI (Vercel-ready)
    ├── index.html
    ├── css/styles.css
    ├── js/config.js         API base URL
    ├── js/app.js
    └── vercel.json
```

## Architecture

```
Vercel (frontend) --CORS--> Railway (Spring Boot)
                               │
                             Neon Postgres
```

Locally:

```
Browser :5500  -->  API :8080  -->  Postgres :5434
```

Short links / redirects always hit the **API** host (`APP_BASE_URL`), not Vercel.

## Quick start (local)

### 1. Database

Host port **`5434`** is used so it does not clash with a system Postgres on `5432` (or other containers on `5433`).

```bash
cd backend
docker compose up -d
docker compose ps
# expect: url-shortener-postgres ... 0.0.0.0:5434->5432/tcp (healthy)
```

### 2. Backend

```bash
cd backend
cp .env.example .env
export $(grep -v '^#' .env | xargs)
mvn spring-boot:run
```

API: `http://localhost:8080`  
Health: `http://localhost:8080/actuator/health`

**Gemini is off by default** (`SPRING_AI_MODEL_CHAT=none`) so the app starts without an API key. Safety and tour soft-fallbacks still work.

Enable live Gemini:

```bash
export GEMINI_API_KEY=your-key
export SPRING_AI_MODEL_CHAT=google-genai
mvn spring-boot:run
```

### 3. Frontend

```bash
cd frontend
python3 -m http.server 5500
```

Open `http://localhost:5500`.  
`frontend/js/config.js` should point at `http://localhost:8080` (default).

UI includes:

- Brand + intro
- **Take the tour** → `GET /api/v1/tour`
- Create form with **Examples** dropdown on the Destination URL label row
- Short link result, safety status, warnings, copy button

## API

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v1/urls` | Create short URL |
| `GET` | `/api/v1/urls/{code}` | Metadata |
| `GET` | `/api/v1/tour` | AI / canned product tour |
| `GET` | `/{code}` | 302 redirect |
| `GET` | `/actuator/health` | Health |

### Create example

```bash
curl -s -X POST http://localhost:8080/api/v1/urls \
  -H 'Content-Type: application/json' \
  -d '{"originalUrl":"https://example.com/some/long/path"}'
```

Example success body:

```json
{
  "id": "...",
  "shortCode": "aB92xK",
  "shortUrl": "http://localhost:8080/aB92xK",
  "originalUrl": "https://example.com/some/long/path",
  "status": "ACTIVE",
  "safetyStatus": "UNKNOWN",
  "createdAt": "...",
  "warnings": [
    "Looks like our AI assistant is down. We can't verify this URL — proceed at your own risk."
  ]
}
```

### DTOs

| Class | Role |
|-------|------|
| `CreateShortUrlRequest` | Create body + bean validation (`http`/`https`, max 2048) |
| `ShortUrlResponse` | Create / metadata response |
| `TourResponse` | Tour steps + `fromAi` / `notice` |
| `ApiErrorResponse` | Global error shape (`timestamp`, `status`, `error`, `message`, `path`, `details`) |

### Backend quality checklist (current)

| Concern | Status |
|---------|--------|
| Bean validation | Yes |
| Global exception handling | Yes |
| Consistent error JSON | Yes |
| Consistent success envelope | No (raw DTOs) |
| Pagination | No |
| Sorting | No |
| Search | No |
| Filter | No |

## Data model

Table `short_urls` (Flyway `V1__create_short_urls.sql`):

| Column | Notes |
|--------|--------|
| `id` | UUID PK |
| `short_code` | Unique, 6-char Base62 at generation time |
| `original_url` | Destination |
| `status` | `ACTIVE` / `BLOCKED` / `DISABLED` |
| `safety_status` | `SAFE` / `SUSPICIOUS` / `UNSAFE` / `UNKNOWN` |
| `created_at` / `updated_at` | Timestamps |

## Environment variables

### Backend

| Variable | Purpose |
|----------|---------|
| `DATABASE_URL` | JDBC URL. Default local: `jdbc:postgresql://localhost:5434/urlshortener`. For Neon, use `jdbc:postgresql://...` with `sslmode=require` |
| `DATABASE_USERNAME` | DB user (local default `urlshortener`) |
| `DATABASE_PASSWORD` | DB password (local default `urlshortener`) |
| `APP_BASE_URL` | Public API URL embedded in `shortUrl` |
| `CORS_ALLOWED_ORIGINS` | Comma-separated UI origins |
| `GEMINI_API_KEY` | Google AI Studio key |
| `SPRING_AI_MODEL_CHAT` | `none` (default) or `google-genai` |
| `GEMINI_MODEL` | Optional, default `gemini-2.0-flash` |
| `PORT` | Server port (Railway sets this) |

### Frontend

Edit `frontend/js/config.js` before Vercel deploy:

```js
window.SHORTLINK_API_BASE = "https://your-app.up.railway.app";
```

## Deployment

1. **Neon** — create DB; put JDBC URL / user / password into Railway.
2. **Railway** — deploy `backend/`; set env vars; use the public URL as `APP_BASE_URL`.
3. **Vercel** — deploy `frontend/`; point `SHORTLINK_API_BASE` at Railway; add the Vercel origin to `CORS_ALLOWED_ORIGINS`.

## Design decisions

- Soft AI safety: store status + warn; never reject create on classification.
- AI optional at runtime: no key → fallback classifier + canned tour.
- No auth in Phase 1 (public create + redirect).
- Random Base62 codes (length 6) with collision retries.
- Accept `http` and `https`; HTTP gets a warning.

## Tests

```bash
cd backend
mvn test
```

- Unit tests: `UrlService`, `TourService`
- Integration tests: Testcontainers Postgres (`UrlApiIntegrationTest`), skipped if Docker is unavailable
- Test profile stubs safety classification (`TestUrlSafetyService`) and disables chat model (`spring.ai.model.chat=none`)

## Troubleshooting (local)

| Symptom | Fix |
|---------|-----|
| `password authentication failed for user "urlshortener"` | You are hitting system Postgres on `5432`. Use Docker on **`5434`** (`docker compose up -d`). |
| `Google GenAI project-id must be set` | Leave `SPRING_AI_MODEL_CHAT=none`, or set both `GEMINI_API_KEY` and `SPRING_AI_MODEL_CHAT=google-genai`. |
| Frontend cannot call API (CORS) | Ensure UI origin is in `CORS_ALLOWED_ORIGINS` and `config.js` API base is correct. |
| Port `8080` busy | Stop the other Java process, or run with `PORT=8081`. |
