# ShortLink — URL Shortener

Resume-grade URL shortener built as a monorepo:

- **Backend:** Java 21, Spring Boot 3.4, Postgres, Flyway, Spring AI (Gemini)
- **Frontend:** HTML, CSS, basic JS
- **Deploy targets:** Vercel (UI) · Railway (API) · Neon (database)
- **Current release:** **v2.0.0** (link management). **v1.0.0** was create/redirect/tour.

## Features

### Version 1
- Create short URLs (random 6-char Base62)
- `302 Found` redirects
- Metadata lookup
- Soft Gemini safety + HTTP / AI-down warnings
- Welcome UI with tour + create form + example dropdown

### Version 2
- Consistent API envelope (`success`, `data`, `meta`, `warnings`, `error`)
- List links with **pagination**, **sorting**, **search** (`q`), **filters** (`status`, `safetyStatus`)
- **PATCH** edit destination and/or enable/disable
- **DELETE** hard-remove a short URL
- Links dashboard UI (search/filter/sort/pager + edit/disable/delete)
- Nullable `owner_id` column + `com.urlshortener.auth` package placeholder for **v3 JWT auth**

### Planned Version 3
- JWT register/login (Spring Security)
- Enforce `owner_id` on create/list/update/delete
- Private “my links” scoped to the authenticated user

### Still out of scope
- Click analytics, custom aliases, rate limiting

## Repository layout

```
Url_Shortner/
├── README.md
├── backend/
│   ├── docker-compose.yml          Postgres on host port 5434
│   ├── pom.xml
│   └── src/main/java/com/urlshortener/
│       ├── auth/                   v3 placeholder (package-info only)
│       ├── config/
│       ├── domain/
│       ├── repository/             JPA + Specifications
│       ├── service/
│       ├── safety/
│       ├── tour/
│       ├── web/ + dto/             Envelope + controllers
│       └── exception/
└── frontend/                       Welcome + Links dashboard
```

## Architecture

```
Vercel (frontend) --CORS--> Railway (Spring Boot)
                               │
                             Neon Postgres
```

Locally: `Browser :5500 → API :8080 → Postgres :5434`

## Quick start (local)

```bash
# DB
cd backend && docker compose up -d

# API
cp .env.example .env
export $(grep -v '^#' .env | xargs)
mvn spring-boot:run

# UI
cd ../frontend && python3 -m http.server 5500
```

Open `http://localhost:5500`.  
Gemini optional: set `GEMINI_API_KEY` and `SPRING_AI_MODEL_CHAT=google-genai`.

## API

All JSON endpoints use the envelope:

```json
{
  "success": true,
  "data": {},
  "meta": null,
  "warnings": [],
  "error": null
}
```

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v1/urls` | Create |
| `GET` | `/api/v1/urls` | List (`page`, `size`, `sort`, `q`, `status`, `safetyStatus`) |
| `GET` | `/api/v1/urls/{code}` | Metadata (ACTIVE only) |
| `PATCH` | `/api/v1/urls/{code}` | Update `originalUrl` and/or `status` (`ACTIVE`/`DISABLED`) |
| `DELETE` | `/api/v1/urls/{code}` | Delete |
| `GET` | `/api/v1/tour` | Product tour |
| `GET` | `/{code}` | 302 redirect (ACTIVE only) |
| `GET` | `/actuator/health` | Health |

### List example

```bash
curl -s 'http://localhost:8080/api/v1/urls?q=example&page=0&size=10&sort=createdAt,desc'
```

### Patch example

```bash
curl -s -X PATCH http://localhost:8080/api/v1/urls/aB92xK \
  -H 'Content-Type: application/json' \
  -d '{"originalUrl":"https://example.com/new","status":"DISABLED"}'
```

### DTOs
`CreateShortUrlRequest`, `UpdateShortUrlRequest`, `ShortUrlResponse`, `TourResponse`, `ApiResponse`, `PageMeta`, `ErrorBody`, `ShortUrlResult`

## Data model (`short_urls`)

| Column | Notes |
|--------|--------|
| `id` | UUID PK |
| `short_code` | Unique |
| `original_url` | Destination |
| `status` | ACTIVE / BLOCKED / DISABLED |
| `safety_status` | SAFE / SUSPICIOUS / UNSAFE / UNKNOWN |
| `owner_id` | UUID NULL — **v3 auth placeholder** |
| `created_at` / `updated_at` | Timestamps |

Migrations: `V1__create_short_urls.sql`, `V2__add_owner_id_and_indexes.sql`

## Environment variables

| Variable | Purpose |
|----------|---------|
| `DATABASE_URL` | JDBC URL (local default port **5434**) |
| `DATABASE_USERNAME` / `DATABASE_PASSWORD` | DB credentials |
| `APP_BASE_URL` | Public API URL in `shortUrl` |
| `CORS_ALLOWED_ORIGINS` | Frontend origins |
| `GEMINI_API_KEY` | Google AI Studio key |
| `SPRING_AI_MODEL_CHAT` | `none` (default) or `google-genai` |
| `PORT` | Server port |

Frontend: set `window.SHORTLINK_API_BASE` in `frontend/js/config.js`.

## Tests

```bash
cd backend && mvn test
```

Unit + Testcontainers integration (skipped if Docker unavailable).

## Troubleshooting

| Symptom | Fix |
|---------|-----|
| Auth failed for `urlshortener` on 5432 | Use Docker on **5434** |
| GenAI project-id must be set | Keep `SPRING_AI_MODEL_CHAT=none` or set key + `google-genai` |
| CORS errors | Add UI origin to `CORS_ALLOWED_ORIGINS`; allow PATCH/DELETE |
