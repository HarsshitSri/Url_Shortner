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

  const linksFilterForm = document.getElementById("linksFilterForm");
  const linksTableBody = document.getElementById("linksTableBody");
  const linksError = document.getElementById("linksError");
  const linksWarnings = document.getElementById("linksWarnings");
  const pageInfo = document.getElementById("pageInfo");
  const prevPageBtn = document.getElementById("prevPageBtn");
  const nextPageBtn = document.getElementById("nextPageBtn");
  const refreshLinksBtn = document.getElementById("refreshLinksBtn");

  const listState = {
    page: 0,
    totalPages: 0,
  };

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
        body = { success: false, error: { message: text } };
      }
    }

    if (!response.ok || (body && body.success === false)) {
      const err = body && body.error ? body.error : null;
      const details = err && Array.isArray(err.details) ? err.details.join(" ") : "";
      const message = (err && err.message) || (body && body.message) || `Request failed (${response.status})`;
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

  function showLinksError(message) {
    linksError.hidden = false;
    linksError.textContent = message;
  }

  function clearLinksError() {
    linksError.hidden = true;
    linksError.textContent = "";
  }

  function renderWarnings(target, warnings) {
    target.innerHTML = "";
    if (!warnings || !warnings.length) {
      target.hidden = true;
      return;
    }
    target.hidden = false;
    warnings.forEach((warning) => {
      const li = document.createElement("li");
      li.textContent = warning;
      target.append(li);
    });
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

  function buildListQuery(page) {
    const params = new URLSearchParams();
    const q = document.getElementById("searchQ").value.trim();
    const status = document.getElementById("filterStatus").value;
    const safetyStatus = document.getElementById("filterSafety").value;
    const sort = document.getElementById("sortBy").value;
    const size = document.getElementById("pageSize").value;

    if (q) params.set("q", q);
    if (status) params.set("status", status);
    if (safetyStatus) params.set("safetyStatus", safetyStatus);
    params.set("page", String(page));
    params.set("size", size);

    const [sortField, sortDir] = sort.split(",");
    params.set("sort", `${sortField},${sortDir}`);

    return params.toString();
  }

  function formatDate(value) {
    if (!value) return "—";
    try {
      return new Date(value).toLocaleString();
    } catch {
      return value;
    }
  }

  async function loadLinks(page = 0) {
    clearLinksError();
    linksTableBody.innerHTML = `<tr><td colspan="6" class="empty">Loading links…</td></tr>`;

    const requestedPage = Math.max(0, Number(page) || 0);

    try {
      const envelope = await fetchJson(`/api/v1/urls?${buildListQuery(requestedPage)}`);
      const rows = envelope.data || [];
      const meta = envelope.meta || {};
      listState.page = Number(meta.page ?? requestedPage);
      listState.totalPages = Number(meta.totalPages ?? 0);
      const totalElements = Number(meta.totalElements ?? 0);

      const displayPages = Math.max(listState.totalPages, 1);
      pageInfo.textContent = `Page ${listState.page + 1} of ${displayPages} · ${totalElements} total`;

      // Keep buttons clickable; only reflect boundary state visually.
      const atStart = listState.page <= 0;
      const atEnd = listState.totalPages <= 1 || listState.page >= listState.totalPages - 1;
      prevPageBtn.classList.toggle("is-inactive", atStart);
      nextPageBtn.classList.toggle("is-inactive", atEnd);
      prevPageBtn.setAttribute("aria-disabled", atStart ? "true" : "false");
      nextPageBtn.setAttribute("aria-disabled", atEnd ? "true" : "false");
      prevPageBtn.disabled = false;
      nextPageBtn.disabled = false;

      if (!rows.length) {
        linksTableBody.innerHTML = `<tr><td colspan="6" class="empty">No links found.</td></tr>`;
        return;
      }

      linksTableBody.innerHTML = "";
      rows.forEach((row) => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
          <td class="code"><a href="${row.shortUrl}" target="_blank" rel="noopener noreferrer">${row.shortCode}</a></td>
          <td class="dest">${row.originalUrl}</td>
          <td>${row.status}</td>
          <td>${row.safetyStatus}</td>
          <td>${formatDate(row.createdAt)}</td>
          <td class="row-actions"></td>
        `;

        const actions = tr.querySelector(".row-actions");

        const editBtn = document.createElement("button");
        editBtn.type = "button";
        editBtn.className = "btn btn-secondary btn-tiny";
        editBtn.textContent = "Edit";
        editBtn.addEventListener("click", () => editLink(row));

        const toggleBtn = document.createElement("button");
        toggleBtn.type = "button";
        toggleBtn.className = "btn btn-secondary btn-tiny";
        toggleBtn.textContent = row.status === "ACTIVE" ? "Disable" : "Enable";
        toggleBtn.addEventListener("click", () => toggleStatus(row));

        const deleteBtn = document.createElement("button");
        deleteBtn.type = "button";
        deleteBtn.className = "btn btn-secondary btn-tiny";
        deleteBtn.textContent = "Delete";
        deleteBtn.addEventListener("click", () => deleteLink(row));

        actions.append(editBtn, toggleBtn, deleteBtn);
        linksTableBody.append(tr);
      });
    } catch (err) {
      linksTableBody.innerHTML = `<tr><td colspan="6" class="empty">Could not load links.</td></tr>`;
      showLinksError(err.message || "Could not load links.");
    }
  }

  async function editLink(row) {
    const nextUrl = window.prompt("New destination URL", row.originalUrl);
    if (nextUrl == null) return;
    const trimmed = nextUrl.trim();
    if (!trimmed) {
      showLinksError("Destination URL cannot be empty.");
      return;
    }

    try {
      const envelope = await fetchJson(`/api/v1/urls/${row.shortCode}`, {
        method: "PATCH",
        body: JSON.stringify({ originalUrl: trimmed }),
      });
      renderWarnings(linksWarnings, envelope.warnings);
      await loadLinks(listState.page);
    } catch (err) {
      showLinksError(err.message || "Could not update link.");
    }
  }

  async function toggleStatus(row) {
    const nextStatus = row.status === "ACTIVE" ? "DISABLED" : "ACTIVE";
    try {
      await fetchJson(`/api/v1/urls/${row.shortCode}`, {
        method: "PATCH",
        body: JSON.stringify({ status: nextStatus }),
      });
      await loadLinks(listState.page);
    } catch (err) {
      showLinksError(err.message || "Could not update status.");
    }
  }

  async function deleteLink(row) {
    if (!window.confirm(`Delete short link ${row.shortCode}? This cannot be undone.`)) {
      return;
    }
    try {
      await fetchJson(`/api/v1/urls/${row.shortCode}`, { method: "DELETE" });
      await loadLinks(listState.page);
    } catch (err) {
      showLinksError(err.message || "Could not delete link.");
    }
  }

  tourBtn.addEventListener("click", async () => {
    clearError();
    tourBtn.disabled = true;
    tourBtn.textContent = "Loading tour…";
    try {
      const envelope = await fetchJson("/api/v1/tour");
      renderTour(envelope.data);
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
      const envelope = await fetchJson("/api/v1/urls", {
        method: "POST",
        body: JSON.stringify({ originalUrl }),
      });
      const result = envelope.data;

      shortUrlEl.href = result.shortUrl;
      shortUrlEl.textContent = result.shortUrl;
      safetyStatusEl.textContent = `Safety status: ${result.safetyStatus}`;
      renderWarnings(warningsList, envelope.warnings);

      resultBox.hidden = false;
      resultBox.scrollIntoView({ behavior: "smooth", block: "nearest" });
      await loadLinks(0);
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

  linksFilterForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    await loadLinks(0);
  });

  refreshLinksBtn.addEventListener("click", () => loadLinks(listState.page));

  function goToPage(delta) {
    const target = listState.page + delta;
    if (listState.totalPages <= 0) {
      return;
    }
    if (target < 0 || target >= listState.totalPages) {
      return;
    }
    loadLinks(target);
  }

  prevPageBtn.addEventListener("click", (event) => {
    event.preventDefault();
    goToPage(-1);
  });
  nextPageBtn.addEventListener("click", (event) => {
    event.preventDefault();
    goToPage(1);
  });

  document.getElementById("pageSize").addEventListener("change", () => {
    loadLinks(0);
  });

  loadLinks(0);
})();
