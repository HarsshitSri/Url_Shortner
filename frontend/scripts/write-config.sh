#!/bin/sh
set -eu

API_BASE="${SHORTLINK_API_BASE:-}"
GITHUB_URL="${SHORTLINK_GITHUB_URL:-https://github.com/HarsshitSri}"
LINKEDIN_URL="${SHORTLINK_LINKEDIN_URL:-https://www.linkedin.com/in/harsshit}"

# On Vercel, refuse to ship localhost API (causes browser NetworkError).
if [ "${VERCEL:-}" = "1" ] && [ -z "${API_BASE}" ]; then
  echo "ERROR: Set SHORTLINK_API_BASE in Vercel env to your Railway HTTPS URL, then redeploy." >&2
  exit 1
fi

if [ -z "${API_BASE}" ]; then
  API_BASE="http://localhost:8080"
fi

# Strip trailing slash
API_BASE=$(printf '%s' "$API_BASE" | sed 's:/*$::')

case "$API_BASE" in
  http://localhost*|http://127.0.0.1*)
    if [ "${VERCEL:-}" = "1" ]; then
      echo "ERROR: SHORTLINK_API_BASE cannot be localhost on Vercel ($API_BASE)." >&2
      exit 1
    fi
    ;;
  https://*)
    ;;
  http://*)
    echo "WARN: SHORTLINK_API_BASE is not HTTPS: $API_BASE" >&2
    ;;
  *)
    echo "ERROR: SHORTLINK_API_BASE must be an absolute URL, got: $API_BASE" >&2
    exit 1
    ;;
esac

cat > js/config.js <<EOF
window.SHORTLINK_API_BASE = "${API_BASE}";
window.SHORTLINK_GITHUB_URL = "${GITHUB_URL}";
window.SHORTLINK_LINKEDIN_URL = "${LINKEDIN_URL}";
EOF

echo "Wrote js/config.js with SHORTLINK_API_BASE=${API_BASE}"
