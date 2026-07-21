(() => {
  const statusEl = document.getElementById("callbackStatus");
  const errorEl = document.getElementById("authError");

  function fail(message) {
    if (statusEl) statusEl.textContent = "Sign-in failed.";
    if (errorEl) {
      errorEl.hidden = false;
      errorEl.textContent = message;
    }
  }

  try {
    const params = new URLSearchParams(window.location.search);
    const token = params.get("token");
    const email = params.get("email");
    const id = params.get("id");

    if (!token || !email || !id) {
      fail("Missing OAuth token. Try signing in again.");
      return;
    }

    window.ShortLinkAuth.setSession(token, { id, email });
    window.history.replaceState({}, document.title, window.location.pathname);
    window.location.replace("index.html");
  } catch (err) {
    fail(err.message || "Could not complete OAuth sign-in.");
  }
})();
