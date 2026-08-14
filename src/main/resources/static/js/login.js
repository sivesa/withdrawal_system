/* ---------------------------------------------------------------------------
 * Login page logic. Two tabs: Investor (pick a real seeded investor from the
 * backend) and Administrator (fixed demo credentials, since the backend has
 * no auth endpoints - see auth.js for the caveat).
 * ------------------------------------------------------------------------ */

const API_BASE_URL = "/api"; // same-origin now that the frontend is served by this Spring Boot app

const tabs = document.querySelectorAll(".auth-tab");
const investorForm = document.getElementById("investorLoginForm");
const adminForm = document.getElementById("adminLoginForm");
const investorSelect = document.getElementById("investorLoginSelect");
const loginError = document.getElementById("loginError");

const ADMIN_USERNAME = "admin";
const ADMIN_PASSWORD = "Enviro365Admin!";

document.addEventListener("DOMContentLoaded", init);

async function init() {
  // If already logged in, skip straight to the right page.
  const existing = getSession();
  if (existing) {
    window.location.href = existing.role === "admin" ? "/enviro365/admin" : "/enviro365/investor/dashboard";
    return;
  }

  tabs.forEach((tab) => tab.addEventListener("click", () => switchTab(tab.dataset.tab)));
  investorForm.addEventListener("submit", onInvestorLogin);
  adminForm.addEventListener("submit", onAdminLogin);

  await loadInvestorOptions();
}

function switchTab(tabName) {
  tabs.forEach((t) => t.classList.toggle("active", t.dataset.tab === tabName));
  investorForm.hidden = tabName !== "investor";
  adminForm.hidden = tabName !== "admin";
  hideError();
}

async function loadInvestorOptions() {
  try {
    const response = await fetch(`${API_BASE_URL}/investors`);
    if (!response.ok) throw new Error("Failed to load investors");
    const investors = await response.json();

    investorSelect.innerHTML = "";
    investors.forEach((inv) => {
      const opt = document.createElement("option");
      opt.value = inv.id;
      opt.dataset.name = inv.fullName;
      opt.textContent = `${inv.fullName} (${inv.email})`;
      investorSelect.appendChild(opt);
    });
  } catch (err) {
    console.error(err);
    showError("Could not reach the backend at " + API_BASE_URL + ". Is it running?");
  }
}

function onInvestorLogin(evt) {
  evt.preventDefault();
  hideError();

  const option = investorSelect.selectedOptions[0];
  const password = document.getElementById("investorLoginPassword").value;

  if (!option) {
    showError("No investor accounts are available. Please try again once the backend is running.");
    return;
  }
  if (!password) {
    showError("Please enter a password.");
    return;
  }

  setSession({
    role: "investor",
    investorId: Number(option.value),
    name: option.dataset.name,
    loginAt: new Date().toISOString(),
  });
  window.location.href = "/enviro365/investor/dashboard";
}

function onAdminLogin(evt) {
  evt.preventDefault();
  hideError();

  const username = document.getElementById("adminUsername").value.trim();
  const password = document.getElementById("adminPassword").value;

  if (username !== ADMIN_USERNAME || password !== ADMIN_PASSWORD) {
    showError("Invalid administrator username or password.");
    return;
  }

  setSession({
    role: "admin",
    name: "Administrator",
    loginAt: new Date().toISOString(),
  });
  window.location.href = "/enviro365/admin";
}

function showError(message) {
  loginError.textContent = message;
  loginError.hidden = false;
}
function hideError() {
  loginError.hidden = true;
}
