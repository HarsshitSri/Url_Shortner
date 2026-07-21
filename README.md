# ShortLink — URL Shortener

Resume-grade URL shortener built as a monorepo:

- **Backend:** Java 21, Spring Boot 3.4, Postgres, Flyway, Spring Security + JWT, Spring AI (Gemini)
- **Frontend:** HTML, CSS, basic JS (`index`, `login`, `register`, OAuth callback)
- **Deploy targets:** Vercel (UI) · Railway (API) · Neon (database) · Docker Compose (full stack)
- **Current release:** **v3.0.0** (auth + ownership + optional OAuth). Tags: `v1.0.0`, `v2.0.0`, `v3.0.0`

## Features by version

### v1
- Create, 302 redirect, metadata, soft Gemini safety, tour UI

### v2
- API envelope, list with pagination/sort/search/filter, PATCH/DELETE, Links dashboard, `owner_id` placeholder

### v3
- Email/password register + login (JWT)
- Create / list / get / patch / delete require `Authorization: Bearer …`
- Redirects + tour + health stay public
- Each new link stores `owner_id`; list/manage are owner-scoped
- Legacy `owner_id = null` links remain orphaned (redirects still work; they do not appear in “My links”)
- Separate `login.html` / `register.html`; token in `localStorage`
- Optional Google + GitHub OAuth (Spring OAuth2 → same JWT → `oauth-callback.html`)

## Quick start (local)

### Option A — Docker Compose (recommended)

From the repo root:

```bash
docker compose up --build
```

- UI: `http://localhost:5500`
- API: `http://localhost:8080`
- Postgres: host port `5434`

Override host ports if needed: `API_PORT=18080 WEB_PORT=15500 POSTGRES_PORT=5435 docker compose up --build`. When changing `API_PORT`, also set `SHORTLINK_API_BASE` and `APP_BASE_URL` to match (for example `http://localhost:18080`).

Optional env overrides (JWT, OAuth, Gemini, CORS) can be passed via a root `.env` or your shell. The API image is also usable alone on Railway:

```bash
docker build -t shortlink-api ./backend
docker run --rm -p 8080:8080 \
  -e DATABASE_URL=jdbc:postgresql://… \
  -e DATABASE_USERNAME=… \
  -e DATABASE_PASSWORD=… \
  -e JWT_SECRET=… \
  -e APP_BASE_URL=https://your-api.example \
  -e CORS_ALLOWED_ORIGINS=https://your-ui.example \
  shortlink-api
```

Postgres-only (for local `mvn spring-boot:run`):

```bash
cd backend && docker compose up -d
```

### Option B — API + static UI without containers

```bash
cd backend && docker compose up -d
cp .env.example .env
export $(grep -v '^#' .env | xargs)
mvn spring-boot:run

cd ../frontend && python3 -m http.server 5500
```

Open `http://localhost:5500` → **Register** → create links.

Postgres host port: **5434**. API: **8080**.

## Auth API

| Method | Path | Auth | Notes |
|--------|------|------|-------|
| `POST` | `/api/v1/auth/register` | public | email + password (min 8) → JWT |
| `POST` | `/api/v1/auth/login` | public | → JWT |
| `GET` | `/api/v1/auth/me` | JWT | current user |
| `GET` | `/api/v1/auth/providers` | public | enabled OAuth providers (`google`, `github`) |
| Browser | `/oauth2/authorization/{google\|github}` | public | starts OAuth; redirects to frontend with JWT |

### Google / GitHub OAuth

1. Set `FRONTEND_BASE_URL` to your UI origin (local: `http://localhost:5500`).
2. Create OAuth apps and set redirect URIs to the **API** (not the frontend):
   - Google: `{APP_BASE_URL}/login/oauth2/code/google`
   - GitHub: `{APP_BASE_URL}/login/oauth2/code/github`
3. Put `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` and/or `GITHUB_CLIENT_ID` / `GITHUB_CLIENT_SECRET` in `backend/.env`.
4. Restart the API. Login/register pages call `/api/v1/auth/providers` and show only configured providers.
5. After consent, the API issues the same JWT and redirects to `{FRONTEND_BASE_URL}/oauth-callback.html?token=…`.

OAuth-only accounts have no password; password login tells the user to use Google/GitHub. Matching email links into an existing account.

Example:

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"you@example.com","password":"password1"}' | jq -r .data.token)

curl -s -X POST http://localhost:8080/api/v1/urls \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"originalUrl":"https://example.com"}'
```

## URL API (authenticated unless noted)

| Method | Path | Auth |
|--------|------|------|
| `POST` | `/api/v1/urls` | JWT |
| `GET` | `/api/v1/urls` | JWT (own links only) |
| `GET` | `/api/v1/urls/{code}` | JWT (own only) |
| `PATCH` | `/api/v1/urls/{code}` | JWT (own only) |
| `DELETE` | `/api/v1/urls/{code}` | JWT (own only) |
| `GET` | `/api/v1/tour` | public |
| `GET` | `/{code}` | public redirect |
| `GET` | `/actuator/health` | public |

All JSON uses the envelope: `success`, `data`, `meta`, `warnings`, `error`.

## Environment

| Variable | Purpose |
|----------|---------|
| `DATABASE_URL` | JDBC URL (local default port 5434) |
| `DATABASE_USERNAME` / `DATABASE_PASSWORD` | DB credentials |
| `APP_BASE_URL` | Public API URL in short links |
| `CORS_ALLOWED_ORIGINS` | Frontend origins |
| `JWT_SECRET` | HMAC secret (use a long random value in prod) |
| `JWT_EXPIRATION_MS` | Default `86400000` (1 day) |
| `FRONTEND_BASE_URL` | UI origin for OAuth success/error redirects |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | Optional Google OAuth |
| `GITHUB_CLIENT_ID` / `GITHUB_CLIENT_SECRET` | Optional GitHub OAuth |
| `GEMINI_API_KEY` / `SPRING_AI_MODEL_CHAT` | Optional AI (`none` by default) |
| `SHORTLINK_API_BASE` | Frontend (Vercel): Railway API origin — see `frontend/.env.example` |

## Manual production deploy

Templates and checklist (fill placeholders yourself):

- [`deploy/README.md`](deploy/README.md) — Neon → Railway → Vercel steps
- [`deploy/railway.env.example`](deploy/railway.env.example) — Railway API variables
- [`frontend/.env.example`](frontend/.env.example) — Vercel UI variables

## Data model

- `users` — id, email, nullable `password_hash`, `auth_provider`, `provider_subject`, timestamps
- `short_urls` — includes nullable `owner_id` (set on authenticated create)

Migrations: `V1` schema, `V2` owner_id/indexes, `V3` users, `V4` OAuth columns.

## Tests

```bash
cd backend && mvn test
```

Unit tests cover auth + ownership. Integration tests (Testcontainers) cover register → owned CRUD → cross-user 404 → public redirect/tour.

## Troubleshooting

| Symptom | Fix |
|---------|-----|
| 401 on create/list | Register/login and send `Authorization: Bearer …` |
| Empty “My links” | You only see links created while logged in; pre-auth orphans are hidden |
| GenAI project-id error | Keep `SPRING_AI_MODEL_CHAT=none` or set Gemini key + `google-genai` |
