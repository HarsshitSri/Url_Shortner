(() => {
  const TOKEN_KEY = "shortlink_token";
  const USER_KEY = "shortlink_user";

  function getToken() {
    return localStorage.getItem(TOKEN_KEY);
  }

  function getUser() {
    try {
      return JSON.parse(localStorage.getItem(USER_KEY) || "null");
    } catch {
      return null;
    }
  }

  function setSession(token, user) {
    localStorage.setItem(TOKEN_KEY, token);
    localStorage.setItem(USER_KEY, JSON.stringify(user));
  }

  function clearSession() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  }

  function isLoggedIn() {
    return Boolean(getToken());
  }

  function authHeaders() {
    const token = getToken();
    return token ? { Authorization: `Bearer ${token}` } : {};
  }

  function requireAuthOrRedirect() {
    if (!isLoggedIn()) {
      window.location.href = "login.html";
      return false;
    }
    return true;
  }

  function apiBase() {
    return (window.SHORTLINK_API_BASE || "http://localhost:8080").replace(/\/$/, "");
  }

  function explainFetchError(err) {
    const api = apiBase();
    const onLocalhost = /localhost|127\.0\.0\.1/.test(window.location.hostname);
    const apiIsLocal = /localhost|127\.0\.0\.1/.test(api);
    if (!onLocalhost && apiIsLocal) {
      return (
        "Frontend is calling " + api + ". On Vercel set SHORTLINK_API_BASE to your Railway HTTPS URL and redeploy."
      );
    }
    if (err && (err.name === "TypeError" || /NetworkError|Failed to fetch/i.test(String(err.message || err)))) {
      return (
        "Cannot reach API at " + api +
        ". Check Railway is up (/actuator/health), CORS_ALLOWED_ORIGINS includes this site, and SHORTLINK_API_BASE is correct."
      );
    }
    return (err && err.message) || "Request failed";
  }

  window.ShortLinkAuth = {
    getToken,
    getUser,
    setSession,
    clearSession,
    isLoggedIn,
    authHeaders,
    requireAuthOrRedirect,
    apiBase,
    explainFetchError,
  };
})();