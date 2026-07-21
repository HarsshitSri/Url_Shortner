(() => {
  function apiBase() {
    return (window.SHORTLINK_API_BASE || "http://localhost:8080").replace(/\/$/, "");
  }

  function oauthStartUrl(provider) {
    return `${apiBase()}/oauth2/authorization/${provider}`;
  }

  async function loadProviders() {
    try {
      const res = await fetch(`${apiBase()}/api/v1/auth/providers`);
      if (!res.ok) return [];
      const body = await res.json();
      return body?.data?.providers || [];
    } catch {
      return [];
    }
  }

  function renderOAuthButtons(container, providers) {
    if (!container || !providers.length) {
      if (container) container.hidden = true;
      return;
    }

    container.hidden = false;
    container.innerHTML = "";

    const divider = document.createElement("p");
    divider.className = "oauth-divider";
    divider.textContent = "or continue with";
    container.appendChild(divider);

    const row = document.createElement("div");
    row.className = "oauth-row";

    for (const provider of providers) {
      const btn = document.createElement("a");
      btn.className = `btn btn-oauth btn-oauth-${provider}`;
      btn.href = oauthStartUrl(provider);
      btn.textContent = provider === "google" ? "Google" : "GitHub";
      btn.setAttribute("aria-label", `Continue with ${btn.textContent}`);
      row.appendChild(btn);
    }

    container.appendChild(row);
  }

  async function mountOAuth(containerId) {
    const container = document.getElementById(containerId);
    if (!container) return;
    const providers = await loadProviders();
    renderOAuthButtons(container, providers);
  }

  function readOAuthErrorFromQuery() {
    const params = new URLSearchParams(window.location.search);
    return params.get("error");
  }

  window.ShortLinkOAuth = {
    mountOAuth,
    readOAuthErrorFromQuery,
    oauthStartUrl,
  };
})();
