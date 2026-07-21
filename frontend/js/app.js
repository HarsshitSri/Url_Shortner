(() => {
  const apiBase = (window.SHORTLINK_API_BASE || "http://localhost:8080").replace(/\/$/, "");

  const tourBtn = document.getElementById("tourBtn");
  const tourPanel = document.getElementById("tour");
  const tourTitle = document.getElementById("tourTitle");
  const tourNotice = document.getElementById("tourNotice");
  const tourSteps = document.getElementById("tourSteps");
  const createForm = document.getElementById("createForm");
  const submitBtn = document.getElementById("submitBtn");
  const errorBox = document.getElementById("errorBox");
  const resultBox = document.getElementById("resultBox");
  const shortUrlEl = document.getElementById("shortUrl");
  const safetyStatusEl = document.getElementById("safetyStatus");
  const warningsList = document.getElementById("warningsList");
  const copyBtn = document.getElementById("copyBtn");
  const apiBaseLabel = document.getElementById("apiBaseLabel");
  const originalUrlInput = document.getElementById("originalUrl");
  const exampleLinks = document.getElementById("exampleLinks");

  apiBaseLabel.textContent = apiBase;

  exampleLinks.addEventListener("change", () => {
    if (!exampleLinks.value) return;
    originalUrlInput.value = exampleLinks.value;
    originalUrlInput.focus();
    exampleLinks.selectedIndex = 0;
  });

  async function fetchJson(path, options) {
    const response = await fetch(`${apiBase}${path}`, {
      headers: { "Content-Type": "application/json", ...(options && options.headers) },
      ...options,
    });

    let body = null;
    const text = await response.text();
    if (text) {
      try {
        body = JSON.parse(text);
      } catch {
        body = { message: text };
      }
    }

    if (!response.ok) {
      const details = body && Array.isArray(body.details) ? body.details.join(" ") : "";
      const message = (body && body.message) || `Request failed (${response.status})`;
      throw new Error(details ? `${message} ${details}` : message);
    }

    return body;
  }

  function showError(message) {
    errorBox.hidden = false;
    errorBox.textContent = message;
  }

  function clearError() {
    errorBox.hidden = true;
    errorBox.textContent = "";
  }

  function renderTour(tour) {
    tourPanel.hidden = false;
    tourTitle.textContent = tour.title || "Tour";
    tourSteps.innerHTML = "";

    if (tour.notice) {
      tourNotice.hidden = false;
      tourNotice.textContent = tour.notice;
    } else {
      tourNotice.hidden = true;
      tourNotice.textContent = "";
    }

    (tour.steps || []).forEach((step) => {
      const li = document.createElement("li");
      const content = document.createElement("div");
      const heading = document.createElement("h3");
      const body = document.createElement("p");
      heading.textContent = step.heading;
      body.textContent = step.body;
      content.append(heading, body);
      li.append(content);
      tourSteps.append(li);
    });

    tourPanel.scrollIntoView({ behavior: "smooth", block: "start" });
  }

  tourBtn.addEventListener("click", async () => {
    clearError();
    tourBtn.disabled = true;
    tourBtn.textContent = "Loading tour…";
    try {
      const tour = await fetchJson("/api/v1/tour");
      renderTour(tour);
    } catch (err) {
      showError(err.message || "Could not load the tour.");
    } finally {
      tourBtn.disabled = false;
      tourBtn.textContent = "Take the tour";
    }
  });

  createForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    clearError();
    resultBox.hidden = true;

    const originalUrl = originalUrlInput.value.trim();
    if (!originalUrl) {
      showError("Please enter a URL.");
      return;
    }

    submitBtn.disabled = true;
    submitBtn.textContent = "Shortening…";

    try {
      const result = await fetchJson("/api/v1/urls", {
        method: "POST",
        body: JSON.stringify({ originalUrl }),
      });

      shortUrlEl.href = result.shortUrl;
      shortUrlEl.textContent = result.shortUrl;
      safetyStatusEl.textContent = `Safety status: ${result.safetyStatus}`;

      warningsList.innerHTML = "";
      (result.warnings || []).forEach((warning) => {
        const li = document.createElement("li");
        li.textContent = warning;
        warningsList.append(li);
      });

      resultBox.hidden = false;
      resultBox.scrollIntoView({ behavior: "smooth", block: "nearest" });
    } catch (err) {
      showError(err.message || "Could not create short URL.");
    } finally {
      submitBtn.disabled = false;
      submitBtn.textContent = "Shorten";
    }
  });

  copyBtn.addEventListener("click", async () => {
    const value = shortUrlEl.textContent;
    if (!value) return;
    try {
      await navigator.clipboard.writeText(value);
      copyBtn.textContent = "Copied";
      setTimeout(() => {
        copyBtn.textContent = "Copy link";
      }, 1400);
    } catch {
      showError("Could not copy to clipboard.");
    }
  });
})();
