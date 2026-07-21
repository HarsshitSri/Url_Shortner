(() => {
  const apiBase = (window.SHORTLINK_API_BASE || "http://localhost:8080").replace(/\/$/, "");
  const form = document.getElementById("registerForm");
  const errorBox = document.getElementById("authError");
  const button = document.getElementById("registerBtn");
  const emailInput = document.getElementById("email");
  const passwordInput = document.getElementById("password");

  if (window.ShortLinkAuth.isLoggedIn()) {
    window.location.href = "index.html";
    return;
  }

  window.ShortLinkOAuth.mountOAuth("oauthButtons");

  function formatApiError(body, fallback) {
    if (!body || !body.error) return fallback;
    const details = Array.isArray(body.error.details) ? body.error.details.filter(Boolean) : [];
    if (details.length) return details.join(" · ");
    return body.error.message || fallback;
  }

  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    errorBox.hidden = true;

    const email = emailInput.value.trim();
    const password = passwordInput.value;

    if (!email) {
      errorBox.hidden = false;
      errorBox.textContent = "Email is required.";
      return;
    }
    if (password.length < 8) {
      errorBox.hidden = false;
      errorBox.textContent = "Password must be at least 8 characters.";
      return;
    }

    button.disabled = true;
    button.textContent = "Creating…";

    try {
      const response = await fetch(`${apiBase}/api/v1/auth/register`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, password }),
      });
      const body = await response.json().catch(() => null);
      if (!response.ok || !body || body.success === false) {
        throw new Error(formatApiError(body, "Registration failed"));
      }
      window.ShortLinkAuth.setSession(body.data.token, body.data.user);
      window.location.href = "index.html";
    } catch (err) {
      errorBox.hidden = false;
      errorBox.textContent = err.message || "Registration failed";
    } finally {
      button.disabled = false;
      button.textContent = "Register";
    }
  });
})();
