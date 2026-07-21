(() => {
  const STORAGE_KEY = "shortlink_theme";

  const TOGGLE_MARKUP = `
    <span class="theme-toggle__track" aria-hidden="true">
      <span class="theme-toggle__sky">
        <span class="theme-toggle__stars"></span>
        <span class="theme-toggle__glow"></span>
      </span>
      <span class="theme-toggle__orb">
        <span class="theme-toggle__sun"></span>
        <span class="theme-toggle__moon"></span>
      </span>
    </span>
    <span class="visually-hidden">Toggle color theme</span>
  `;

  function preferredTheme() {
    const saved = localStorage.getItem(STORAGE_KEY);
    if (saved === "light" || saved === "dark") return saved;
    return window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
  }

  function applyTheme(theme) {
    document.documentElement.setAttribute("data-theme", theme);
    localStorage.setItem(STORAGE_KEY, theme);
    syncToggle(theme);
  }

  function syncToggle(theme) {
    const btn = document.getElementById("themeToggle");
    if (!btn) return;
    const isDark = theme === "dark";
    btn.classList.toggle("is-dark", isDark);
    btn.setAttribute("aria-pressed", isDark ? "true" : "false");
    btn.setAttribute(
      "aria-label",
      isDark ? "Switch to light theme" : "Switch to dark theme"
    );
    btn.title = isDark ? "Light theme" : "Dark theme";
  }

  function toggleTheme() {
    const current = document.documentElement.getAttribute("data-theme") || preferredTheme();
    applyTheme(current === "dark" ? "light" : "dark");
  }

  applyTheme(preferredTheme());

  document.addEventListener("DOMContentLoaded", () => {
    const btn = document.getElementById("themeToggle");
    if (!btn) return;
    btn.innerHTML = TOGGLE_MARKUP;
    syncToggle(document.documentElement.getAttribute("data-theme") || preferredTheme());
    btn.addEventListener("click", toggleTheme);
  });

  window.ShortLinkTheme = { applyTheme, toggleTheme, preferredTheme };
})();
