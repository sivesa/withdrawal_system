/* ---------------------------------------------------------------------------
 * Enviro365 Admin Panel - add new investors (and optional opening holdings).
 * Talks to POST /api/investors and POST /api/investors/{id}/holdings.
 * ------------------------------------------------------------------------ */

const API_BASE_URL = "/api"; // same-origin now that the frontend is served by this Spring Boot app

let availableProducts = [];
let holdingRowCount = 0;

const addInvestorForm = document.getElementById("addInvestorForm");
const fullNameInput = document.getElementById("fullNameInput");
const emailInput = document.getElementById("emailInput");
const dobInput = document.getElementById("dobInput");
const holdingRows = document.getElementById("holdingRows");
const addHoldingRowBtn = document.getElementById("addHoldingRowBtn");
const adminValidationMessage = document.getElementById("adminValidationMessage");
const adminResultBanner = document.getElementById("adminResultBanner");
const createInvestorBtn = document.getElementById("createInvestorBtn");

const investorsTableBody = document.querySelector("#investorsTable tbody");
const investorsEmptyState = document.getElementById("investorsEmptyState");
const refreshInvestorsBtn = document.getElementById("refreshInvestorsBtn");
const toast = document.getElementById("toast");

document.addEventListener("DOMContentLoaded", init);

async function init() {
  const session = requireRole("admin"); // redirects to /enviro365/login if not signed in as an admin
  if (!session) return;

  initUserMenu(session.name);

  addHoldingRowBtn.addEventListener("click", () => addHoldingRow());
  addInvestorForm.addEventListener("submit", onCreateInvestor);
  refreshInvestorsBtn.addEventListener("click", loadInvestors);

  try {
    await Promise.all([loadProducts(), loadInvestors()]);
  } catch (err) {
    console.error(err);
    showToast("Could not reach the backend. Is it running on " + API_BASE_URL + "?");
  }
}

// ---- products (for the holding-row product dropdown) -------------------------------
async function loadProducts() {
  const response = await fetch(`${API_BASE_URL}/products`);
  if (!response.ok) throw new Error("Failed to load products");
  availableProducts = await response.json();
}

function addHoldingRow() {
  const rowId = `holdingRow_${holdingRowCount++}`;
  const row = document.createElement("div");
  row.className = "holding-row";
  row.id = rowId;

  const productOptions = availableProducts
    .map((p) => `<option value="${p.id}">${escapeHtml(p.name)}</option>`)
    .join("");

  row.innerHTML = `
    <select class="holding-product-select">${productOptions}</select>
    <input type="number" class="holding-balance-input" min="0" step="0.01" placeholder="Opening balance (ZAR)">
    <button type="button" class="btn btn-secondary btn-small remove-holding-btn">✕</button>
  `;

  row.querySelector(".remove-holding-btn").addEventListener("click", () => row.remove());
  holdingRows.appendChild(row);
}

// ---- create investor ------------------------------------------------------------------
async function onCreateInvestor(evt) {
  evt.preventDefault();
  hideAdminValidation();
  clearAdminResult();

  const fullName = fullNameInput.value.trim();
  const email = emailInput.value.trim();
  const dateOfBirth = dobInput.value;

  const clientError = validateInvestorForm(fullName, email, dateOfBirth);
  if (clientError) {
    showAdminValidation(clientError);
    return;
  }

  createInvestorBtn.disabled = true;
  createInvestorBtn.textContent = "Creating...";

  try {
    const response = await fetch(`${API_BASE_URL}/investors`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ fullName, email, dateOfBirth }),
    });
    const body = await response.json();

    if (response.status !== 201) {
      const msg = body.validationErrors ? body.validationErrors.join("; ") : body.message;
      showAdminResult(false, msg || "Could not create investor.");
      return;
    }

    const newInvestor = body;
    const holdingResults = await submitHoldingRows(newInvestor.id);

    let message = `Created investor "${newInvestor.fullName}" (id ${newInvestor.id}).`;
    if (holdingResults.attempted > 0) {
      message += ` ${holdingResults.succeeded}/${holdingResults.attempted} opening holding(s) added.`;
      if (holdingResults.errors.length > 0) {
        message += ` Issues: ${holdingResults.errors.join("; ")}`;
      }
    }
    showAdminResult(true, message);

    resetForm();
    await loadInvestors();
  } catch (err) {
    console.error(err);
    showAdminResult(false, "Could not reach the backend. Please check your connection and try again.");
  } finally {
    createInvestorBtn.disabled = false;
    createInvestorBtn.textContent = "Create Investor";
  }
}

function validateInvestorForm(fullName, email, dateOfBirth) {
  if (!fullName) return "Please enter the investor's full name.";
  if (!email || !/^\S+@\S+\.\S+$/.test(email)) return "Please enter a valid email address.";
  if (!dateOfBirth) return "Please select a date of birth.";
  if (new Date(dateOfBirth) >= new Date()) return "Date of birth must be in the past.";
  return null;
}

async function submitHoldingRows(investorId) {
  const rows = Array.from(holdingRows.querySelectorAll(".holding-row"));
  const result = { attempted: 0, succeeded: 0, errors: [] };

  for (const row of rows) {
    const productId = Number(row.querySelector(".holding-product-select").value);
    const balanceRaw = row.querySelector(".holding-balance-input").value;
    if (!balanceRaw) continue; // skip rows the admin left blank

    result.attempted++;
    const balance = parseFloat(balanceRaw);

    try {
      const response = await fetch(`${API_BASE_URL}/investors/${investorId}/holdings`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ productId, balance }),
      });
      if (response.status === 201) {
        result.succeeded++;
      } else {
        const body = await response.json().catch(() => ({}));
        result.errors.push(body.message || `product ${productId} failed`);
      }
    } catch (err) {
      result.errors.push(`product ${productId}: network error`);
    }
  }
  return result;
}

function resetForm() {
  addInvestorForm.reset();
  holdingRows.innerHTML = "";
}

// ---- investors table ------------------------------------------------------------------
async function loadInvestors() {
  const response = await fetch(`${API_BASE_URL}/investors`);
  if (!response.ok) throw new Error("Failed to load investors");
  const investors = await response.json();

  investorsTableBody.innerHTML = "";
  investorsEmptyState.hidden = investors.length > 0;

  investors.forEach((inv) => {
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td>${inv.id}</td>
      <td>${escapeHtml(inv.fullName)}</td>
      <td>${escapeHtml(inv.email)}</td>
      <td>${inv.dateOfBirth}</td>
      <td>${inv.age}</td>
    `;
    investorsTableBody.appendChild(tr);
  });
}

// ---- small UI helpers -------------------------------------------------------------------
function showAdminValidation(message) {
  adminValidationMessage.textContent = message;
  adminValidationMessage.hidden = false;
}
function hideAdminValidation() {
  adminValidationMessage.hidden = true;
}

function showAdminResult(success, message) {
  adminResultBanner.textContent = message;
  adminResultBanner.className = "result-banner " + (success ? "success" : "rejected");
  adminResultBanner.hidden = false;
}
function clearAdminResult() {
  adminResultBanner.hidden = true;
  adminResultBanner.textContent = "";
}

let toastTimer = null;
function showToast(message) {
  toast.textContent = message;
  toast.hidden = false;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => (toast.hidden = true), 3500);
}

function escapeHtml(str) {
  const div = document.createElement("div");
  div.textContent = str ?? "";
  return div.innerHTML;
}
