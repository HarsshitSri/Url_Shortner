#!/bin/sh
set -eu

API_BASE="${SHORTLINK_API_BASE:-http://localhost:8080}"
GITHUB_URL="${SHORTLINK_GITHUB_URL:-https://github.com/HarsshitSri}"
LINKEDIN_URL="${SHORTLINK_LINKEDIN_URL:-https://www.linkedin.com/in/harsshit}"

cat > js/config.js <<EOF
window.SHORTLINK_API_BASE = "${API_BASE}";
window.SHORTLINK_GITHUB_URL = "${GITHUB_URL}";
window.SHORTLINK_LINKEDIN_URL = "${LINKEDIN_URL}";
EOF

echo "Wrote js/config.js with SHORTLINK_API_BASE=${API_BASE}"
