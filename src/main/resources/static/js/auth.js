/* ---------------------------------------------------------------------------
 * Enviro365 - shared auth/session helpers used by every page.
 *
 * NOTE: this is a DEMO login layer only. The Spring Boot backend has no
 * authentication endpoints (out of scope for this assessment) - real
 * investor identity still comes from the investor records already exposed
 * by GET /api/investors. Session state simply lives in localStorage on the
 * browser so the rest of the frontend (dashboard, admin page) knows who is
 * "logged in" and can gate access to pages. See README section 7 for the
 * demo credentials and the security caveats of this approach.
 * ------------------------------------------------------------------------ */

const SESSION_KEY = "e365_session";

function getSession() {
  try {
    const raw = localStorage.getItem(SESSION_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch (err) {
    return null;
  }
}

function setSession(session) {
  localStorage.setItem(SESSION_KEY, JSON.stringify(session));
}

function clearSession() {
  localStorage.removeItem(SESSION_KEY);
}

function logout() {
  clearSession();
  window.location.href = "/enviro365/login";
}

/**
 * Call at the top of any protected page. Redirects to /enviro365/login if there's
 * no session, or if the session's role doesn't match what the page requires.
 * Returns the session so the caller can use it immediately.
 */
function requireRole(role) {
  const session = getSession();
  if (!session || session.role !== role) {
    window.location.href = "/enviro365/login";
    return null;
  }
  return session;
}

/** Wires up the shared header user-menu dropdown (Settings / Logout) present on every protected page. */
function initUserMenu(displayName) {
  const trigger = document.getElementById("userMenuTrigger");
  const menu = document.getElementById("userMenuDropdown");
  const nameEl = document.getElementById("userMenuName");
  const logoutBtn = document.getElementById("logoutBtn");

  if (nameEl) nameEl.textContent = displayName;

  if (trigger && menu) {
    trigger.addEventListener("click", (e) => {
      e.stopPropagation();
      menu.classList.toggle("open");
    });
    document.addEventListener("click", () => menu.classList.remove("open"));
    menu.addEventListener("click", (e) => e.stopPropagation());
  }

  if (logoutBtn) {
    logoutBtn.addEventListener("click", logout);
  }
}
