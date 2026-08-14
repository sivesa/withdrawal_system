const API_BASE_URL = "/api"; // same-origin now that the frontend is served by this Spring Boot app

document.addEventListener("DOMContentLoaded", init);

async function init() {
  const session = getSession();
  if (!session) {
    window.location.href = "/enviro365/login";
    return;
  }

  initUserMenu(session.name);
  const backLink = document.getElementById("backLink");
  if (backLink) backLink.href = session.role === "admin" ? "/enviro365/admin" : "/enviro365/investor/dashboard";

  const profileDetails = document.getElementById("profileDetails");

  if (session.role === "admin") {
    profileDetails.innerHTML = `
      <div><span class="field-label">Name</span><span class="field-value">${escapeHtml(session.name)}</span></div>
      <div><span class="field-label">Role</span><span class="field-value">Administrator</span></div>
      <div><span class="field-label">Signed in since</span><span class="field-value">${formatDateTime(session.loginAt)}</span></div>
    `;
    return;
  }

  try {
    const response = await fetch(`${API_BASE_URL}/investors/${session.investorId}/portfolio`);
    if (!response.ok) throw new Error("Failed to load profile");
    const data = await response.json();
    const investor = data.investor;

    profileDetails.innerHTML = `
      <div><span class="field-label">Full name</span><span class="field-value">${escapeHtml(investor.fullName)}</span></div>
      <div><span class="field-label">Email</span><span class="field-value">${escapeHtml(investor.email)}</span></div>
      <div><span class="field-label">Date of birth</span><span class="field-value">${investor.dateOfBirth}</span></div>
      <div><span class="field-label">Age</span><span class="field-value">${investor.age}</span></div>
      <div><span class="field-label">Role</span><span class="field-value">Investor</span></div>
    `;
  } catch (err) {
    console.error(err);
    profileDetails.innerHTML = `<p class="empty-state">Could not load profile details.</p>`;
  }
}

function formatDateTime(iso) {
  if (!iso) return "-";
  const d = new Date(iso);
  return d.toLocaleString("en-ZA", { dateStyle: "medium", timeStyle: "short" });
}

function escapeHtml(str) {
  const div = document.createElement("div");
  div.textContent = str ?? "";
  return div.innerHTML;
}
