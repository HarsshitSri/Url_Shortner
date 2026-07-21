# =============================================================================
# Manual deploy checklist — Neon → Railway → Vercel
# Env templates: deploy/railway.env.example · frontend/.env.example
# =============================================================================

## 1. Neon
1. Create a project and database.
2. Copy host / db / user / password.
3. Build JDBC URL (no user/pass in the URL; no `channel_binding`):
   `jdbc:postgresql://<host>/<db>?sslmode=require`
   (use the `-pooler` host when available.)
4. Paste into Railway vars: `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`.
   The API also normalizes Neon libpq URLs if pasted by mistake.

## 2. Railway (API)
1. New service from GitHub `HarsshitSri/Url_Shortner`.
2. Root directory: `backend` (uses `Dockerfile` + `railway.toml`).
3. Import vars from `deploy/railway.env.example` (replace every `REPLACE_ME`).
4. Deploy → note public URL → set `APP_BASE_URL` to that URL → redeploy if needed.
5. Smoke: `GET https://<api>/actuator/health` → `{"status":"UP"}`.
   Flyway applies V1–V4 on first boot.

## 3. Vercel (UI)
1. Import same repo. Root Directory: `frontend`.
2. Set Production env from `frontend/.env.example`:
   `SHORTLINK_API_BASE=https://<your-railway-host>`
3. Deploy → note `https://….vercel.app`.

## 4. Cross-link (required)
On Railway, set then redeploy:
- `CORS_ALLOWED_ORIGINS=https://<vercel-app>.vercel.app`
- `FRONTEND_BASE_URL=https://<vercel-app>.vercel.app`
- `APP_BASE_URL=https://<railway-host>` (if not already)

## 5. Smoke test
1. Open Vercel UI → Register → create a short link.
2. Open `https://<railway-host>/<code>` → 302 to destination.
3. Login/logout still works; OAuth buttons stay hidden if client IDs empty.
