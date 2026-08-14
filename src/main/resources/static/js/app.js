/* ---------------------------------------------------------------------------
 * Enviro365 Withdrawal Management - investor dashboard logic
 *
 * Talks to the Spring Boot backend (default: http://localhost:8080).
 * No build tools / frameworks - vanilla JS, fetch API only.
 *
 * The logged-in investor is fixed by the login page's session (see auth.js),
 * not chosen from a dropdown here anymore.
 * ------------------------------------------------------------------------ */

const API_BASE_URL = "/api"; // same-origin now that the frontend is served by this Spring Boot app

// ---- state -----------------------------------------------------------------
let currentInvestorId = null;
let currentInvestorAge = null;
let currentHoldings = [];   // holdings for the logged-in investor, used for client-side pre-validation

// ---- element refs ------------------------------------------------------------
const investorDetails = document.getElementById("investorDetails");
const holdingsTableBody = document.querySelector("#holdingsTable tbody");
const holdingsEmptyState = document.getElementById("holdingsEmptyState");

const holdingSelect = document.getElementById("holdingSelect");
const holdingHint = document.getElementById("holdingHint");
const amountInput = document.getElementById("amountInput");
const withdrawalForm = document.getElementById("withdrawalForm");
const clientValidationMessage = document.getElementById("clientValidationMessage");
const submitResultBanner = document.getElementById("submitResultBanner");

const historyTableBody = document.querySelector("#historyTable tbody");
const historyEmptyState = document.getElementById("historyEmptyState");
const historyStatusFilter = document.getElementById("historyStatusFilter");

const downloadPortfolioBtn = document.getElementById("downloadPortfolioBtn");
const downloadHistoryBtn = document.getElementById("downloadHistoryBtn");
const toast = document.getElementById("toast");

const summaryTotalValue = document.getElementById("summaryTotalValue");
const summaryProductCount = document.getElementById("summaryProductCount");
const summaryWithdrawalCount = document.getElementById("summaryWithdrawalCount");
const summaryLastWithdrawal = document.getElementById("summaryLastWithdrawal");

// ---- init --------------------------------------------------------------------
document.addEventListener("DOMContentLoaded", init);

async function init() {
  const session = requireRole("investor"); // redirects to /enviro365/login if not signed in as an investor
  if (!session) return;

  currentInvestorId = session.investorId;
  initUserMenu(session.name);

  try {
    holdingSelect.addEventListener("change", updateHoldingHint);
    historyStatusFilter.addEventListener("change", () => loadHistory(currentInvestorId));
    withdrawalForm.addEventListener("submit", onSubmitWithdrawal);
    downloadPortfolioBtn.addEventListener("click", downloadPortfolioCsv);
    downloadHistoryBtn.addEventListener("click", downloadHistoryCsv);

    await Promise.all([loadPortfolio(currentInvestorId), loadHistory(currentInvestorId)]);
  } catch (err) {
    showToast("Could not reach the backend. Is it running on " + API_BASE_URL + "?");
    console.error(err);
  }
}

// ---- portfolio -----------------------------------------------------
async function loadPortfolio(investorId) {
  const data = await apiGet(`/investors/${investorId}/portfolio`);
  currentHoldings = data.holdings;
  currentInvestorAge = data.investor.age;
  renderInvestorDetails(data.investor);
  renderHoldingsTable(data.holdings);
  renderHoldingSelect(data.holdings);
  renderPortfolioSummary(data.holdings);
}

function renderInvestorDetails(investor) {
  investorDetails.innerHTML = `
    <div><span class="field-label">Full name</span><span class="field-value">${escapeHtml(investor.fullName)}</span></div>
    <div><span class="field-label">Email</span><span class="field-value">${escapeHtml(investor.email)}</span></div>
    <div><span class="field-label">Date of birth</span><span class="field-value">${investor.dateOfBirth}</span></div>
    <div><span class="field-label">Age</span><span class="field-value">${investor.age}</span></div>
  `;
}

function renderHoldingsTable(holdings) {
  holdingsTableBody.innerHTML = "";
  holdingsEmptyState.hidden = holdings.length > 0;

  holdings.forEach((h) => {
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td>${escapeHtml(h.productName)}</td>
      <td>${formatProductType(h.productType)}</td>
      <td>${formatCurrency(h.balance)}</td>
      <td>${formatCurrency(h.maxWithdrawable)}</td>
    `;
    holdingsTableBody.appendChild(tr);
  });
}

function renderHoldingSelect(holdings) {
  holdingSelect.innerHTML = "";
  holdings.forEach((h) => {
    const opt = document.createElement("option");
    opt.value = h.holdingId;
    opt.textContent = `${h.productName} - Balance ${formatCurrency(h.balance)}`;
    holdingSelect.appendChild(opt);
  });
  updateHoldingHint();
}

function updateHoldingHint() {
  const holding = getSelectedHolding();
  if (!holding) {
    holdingHint.textContent = "";
    return;
  }
  holdingHint.textContent =
    `Available balance: ${formatCurrency(holding.balance)} | Max withdrawable (90%): ${formatCurrency(holding.maxWithdrawable)}` +
    (holding.productType === "RETIREMENT_ANNUITY" ? " | Retirement product: investor must be older than 65" : "");
}

function getSelectedHolding() {
  const id = Number(holdingSelect.value);
  return currentHoldings.find((h) => h.holdingId === id);
}

// ---- portfolio summary card (replaces the old static "Withdrawal Rules" card) ----
function renderPortfolioSummary(holdings) {
  const total = holdings.reduce((sum, h) => sum + Number(h.balance), 0);
  summaryTotalValue.textContent = formatCurrency(total);
  summaryProductCount.textContent = holdings.length;
}

function renderWithdrawalSummary(history) {
  summaryWithdrawalCount.textContent = history.length;
  if (history.length === 0) {
    summaryLastWithdrawal.textContent = "No attempts yet";
    return;
  }
  const latest = history[0]; // history is returned newest-first by the backend
  const badge = latest.status === "SUCCESS" ? "Approved" : "Rejected";
  summaryLastWithdrawal.textContent = `${badge} - ${formatDateTime(latest.createdAt)}`;
}

// ---- withdrawal submission (with client-side pre-validation) -------------------
async function onSubmitWithdrawal(evt) {
  evt.preventDefault();
  clearResultBanner();
  hideClientValidation();

  const holding = getSelectedHolding();
  const amountRaw = amountInput.value;
  const amount = parseFloat(amountRaw);

  // ---- UI-side validation: catch obvious problems before hitting the network ----
  const clientError = validateOnClient(holding, amount);
  if (clientError) {
    showClientValidation(clientError);
    return;
  }

  const submitBtn = document.getElementById("submitWithdrawalBtn");
  submitBtn.disabled = true;
  submitBtn.textContent = "Submitting...";

  try {
    const response = await fetch(`${API_BASE_URL}/withdrawals`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        investorId: currentInvestorId,
        holdingId: holding.holdingId,
        amount: amount,
      }),
    });

    const body = await response.json();

    if (response.status === 201) {
      showResultBanner(true, `Withdrawal approved. New balance: ${formatCurrency(body.balanceAfter)}.`);
    } else if (response.status === 422) {
      showResultBanner(false, body.reason || "Withdrawal rejected by business rules.");
    } else {
      // 400 / 404 / 500 - structural error from GlobalExceptionHandler
      const msg = body.validationErrors ? body.validationErrors.join("; ") : body.message;
      showResultBanner(false, msg || "Request could not be processed.");
    }

    amountInput.value = "";
    // Refresh portfolio (balance may have changed) and history regardless of outcome
    await Promise.all([loadPortfolio(currentInvestorId), loadHistory(currentInvestorId)]);
  } catch (err) {
    console.error(err);
    showResultBanner(false, "Could not reach the backend. Please check your connection and try again.");
  } finally {
    submitBtn.disabled = false;
    submitBtn.textContent = "Submit Withdrawal";
  }
}

/**
 * Client-side mirror of the backend's business rules, so the investor gets
 * instant feedback without a round trip. The backend is still the source of
 * truth and re-checks everything server-side - this is purely UX.
 */
function validateOnClient(holding, amount) {
  if (!holding) return "Please select a product/holding.";
  if (isNaN(amount) || amount <= 0) return "Please enter a withdrawal amount greater than zero.";

  if (holding.productType === "RETIREMENT_ANNUITY" && currentInvestorAge !== null && currentInvestorAge <= 65) {
    return `Retirement withdrawals are only allowed for investors older than 65 (this investor is ${currentInvestorAge}).`;
  }

  if (amount > holding.balance) {
    return `Amount exceeds the available balance of ${formatCurrency(holding.balance)}.`;
  }

  if (amount > holding.maxWithdrawable) {
    return `Amount exceeds 90% of available balance. Maximum allowed is ${formatCurrency(holding.maxWithdrawable)}.`;
  }

  return null;
}

// ---- withdrawal history ----------------------------------------------------------
async function loadHistory(investorId) {
  if (!investorId) return;
  const status = historyStatusFilter.value;
  const params = new URLSearchParams({ investorId });
  if (status) params.append("status", status);

  const history = await apiGet(`/withdrawals?${params.toString()}`);
  renderHistoryTable(history);
  renderWithdrawalSummary(history);
}

function renderHistoryTable(history) {
  historyTableBody.innerHTML = "";
  historyEmptyState.hidden = history.length > 0;

  history.forEach((w) => {
    const tr = document.createElement("tr");
    const statusBadge =
      w.status === "SUCCESS"
        ? `<span class="badge badge-success">Success</span>`
        : `<span class="badge badge-rejected">Rejected</span>`;

    const detail =
      w.status === "SUCCESS"
        ? formatCurrency(w.balanceAfter)
        : escapeHtml(w.reason || "-");

    tr.innerHTML = `
      <td>${formatDateTime(w.createdAt)}</td>
      <td>${escapeHtml(w.productName)}</td>
      <td>${formatCurrency(w.requestedAmount)}</td>
      <td>${statusBadge}</td>
      <td>${detail}</td>
    `;
    historyTableBody.appendChild(tr);
  });
}

// ---- CSV downloads -----------------------------------------------------------------
async function downloadPortfolioCsv() {
  if (!currentInvestorId) return;
  await downloadCsv(`/export/portfolio?investorId=${currentInvestorId}`, "portfolio.csv");
}

async function downloadHistoryCsv() {
  if (!currentInvestorId) return;
  const status = historyStatusFilter.value;
  const params = new URLSearchParams({ investorId: currentInvestorId });
  if (status) params.append("status", status);
  await downloadCsv(`/export/withdrawals?${params.toString()}`, "withdrawal_history.csv");
}

async function downloadCsv(path, fallbackFilename) {
  try {
    const response = await fetch(`${API_BASE_URL}${path}`);
    if (!response.ok) throw new Error("Export failed");

    const disposition = response.headers.get("Content-Disposition") || "";
    const match = disposition.match(/filename="([^"]+)"/);
    const filename = match ? match[1] : fallbackFilename;

    const blob = await response.blob();
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    a.remove();
    window.URL.revokeObjectURL(url);
    showToast(`Downloaded ${filename}`);
  } catch (err) {
    console.error(err);
    showToast("Could not download the CSV file.");
  }
}

// ---- small UI helpers ---------------------------------------------------------------
function showClientValidation(message) {
  clientValidationMessage.textContent = message;
  clientValidationMessage.hidden = false;
}
function hideClientValidation() {
  clientValidationMessage.hidden = true;
}

function showResultBanner(success, message) {
  submitResultBanner.textContent = message;
  submitResultBanner.className = "result-banner " + (success ? "success" : "rejected");
  submitResultBanner.hidden = false;
}
function clearResultBanner() {
  submitResultBanner.hidden = true;
  submitResultBanner.textContent = "";
}

let toastTimer = null;
function showToast(message) {
  toast.textContent = message;
  toast.hidden = false;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => (toast.hidden = true), 3500);
}

function formatCurrency(value) {
  if (value === null || value === undefined) return "-";
  return "R " + Number(value).toLocaleString("en-ZA", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function formatDateTime(iso) {
  if (!iso) return "-";
  const d = new Date(iso);
  return d.toLocaleString("en-ZA", { dateStyle: "medium", timeStyle: "short" });
}

function formatProductType(type) {
  return String(type).replace(/_/g, " ").replace(/\w\S*/g, (t) => t.charAt(0) + t.substr(1).toLowerCase());
}

function escapeHtml(str) {
  const div = document.createElement("div");
  div.textContent = str ?? "";
  return div.innerHTML;
}

// ---- tiny fetch wrapper --------------------------------------------------------------
async function apiGet(path) {
  const response = await fetch(`${API_BASE_URL}${path}`);
  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    throw new Error(body.message || `GET ${path} failed with ${response.status}`);
  }
  return response.json();
}
