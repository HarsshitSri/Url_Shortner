(() => {
  const apiBase = (window.SHORTLINK_API_BASE || "http://localhost:8080").replace(/\/$/, "");
  const form = document.getElementById("loginForm");
  const errorBox = document.getElementById("authError");
  const button = document.getElementById("loginBtn");

  if (window.ShortLinkAuth.isLoggedIn()) {
    window.location.href = "index.html";
    return;
  }

  window.ShortLinkOAuth.mountOAuth("oauthButtons");
  const oauthError = window.ShortLinkOAuth.readOAuthErrorFromQuery();
  if (oauthError) {
    errorBox.hidden = false;
    errorBox.textContent = oauthError;
  }

  function formatApiError(body, fallback) {
    if (!body || !body.error) return fallback;
    const details = Array.isArray(body.error.details) ? body.error.details.filter(Boolean) : [];
    if (details.length) return details.join(" · ");
    return body.error.message || fallback;
  }

  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    errorBox.hidden = true;
    button.disabled = true;
    button.textContent = "Logging in…";

    try {
      const response = await fetch(`${apiBase}/api/v1/auth/login`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          email: document.getElementById("email").value.trim(),
          password: document.getElementById("password").value,
        }),
      });
      const body = await response.json().catch(() => null);
      if (!response.ok || !body || body.success === false) {
        throw new Error(formatApiError(body, "Login failed"));
      }
      window.ShortLinkAuth.setSession(body.data.token, body.data.user);
      window.location.href = "index.html";
    } catch (err) {
      errorBox.hidden = false;
      errorBox.textContent = err.message || "Login failed";
    } finally {
      button.disabled = false;
      button.textContent = "Log in";
    }
  });
})();
