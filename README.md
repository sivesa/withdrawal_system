# Enviro365 Investments — Withdrawal Management System

A full-stack withdrawal management system built for Enviro365 Investments: a Spring Boot REST API backed by H2, paired with a vanilla HTML/CSS/JavaScript frontend. Investors log in, view their portfolio, submit withdrawal notices, see their full withdrawal history (approved and rejected), and export data to CSV. Administrators log in separately to add new investors.

```
enviro365-withdrawal-system/
├── withdrawal_system/
│   └── src/main/
│       ├── java/...                    Spring Boot API (Java 21, Maven)
│       └── resources/
│           ├── templates/*.html        Thymeleaf views (served by InvestorViewController)
│           ├── static/css/style.css    Stylesheet (served directly by Spring Boot)
│           └── static/js/*.js          Frontend logic (served directly by Spring Boot)
└── screenshots/ UI screenshots (see note below)
```

**The Spring Boot serves everything** — API, HTML pages, CSS, and JS
all come from `http://localhost:8080`. There's no separate frontend server
to run. See section 1a below for why, and section 1b for the URL map.

---

## 1. Setup instructions

### Prerequisites
- Java 21+
- Maven 3.6+
- Any modern browser
- Internet access the first time you build, so Maven can download dependencies
  from Maven Central (spring-boot-starter-web, spring-boot-starter-data-jpa, H2,
  spring-boot-starter-validation, spring-boot-starter-test)

# Note on Deliverable: Project Creation & Development

The system was created incrementally following a layered **Spring Boot** architecture. The implementation was developed and reviewed locally using the project structure defined for the assessment.

The development process followed these main stages:

---

### 1. Project Setup
- Created the Spring Boot Maven project.
- Configured the required Spring Boot dependencies.
- Configured **H2** as the database.
- Configured the application properties.

### 2. Domain Model
Created the core domain entities required for the withdrawal management system:
- `Investor`
- `Product`
- `ProductType`
- `Holding`
- `WithdrawalNotice`
- `WithdrawalStatus`

### 3. Repository Layer
- Created **Spring Data JPA** repositories for investors, products, holdings, and withdrawal notices.
- Added custom repository query methods required for application services.

### 4. Service Layer
- **`PortfolioService`**: Implemented logic for retrieving investor portfolio information.
- **`WithdrawalService`**: Implemented business rules for creating and retrieving withdrawal notices.
- **`CsvExportService`**: Implemented logic for exporting withdrawal history to CSV format.

### 5. Testing
Used the provided service tests as implementation checkpoints:
- `PortfolioServiceTest`
- `WithdrawalServiceTest`
- `CsvExportServiceTest`

> *The implementation was developed with tests in mind so that core business logic could be verified independently of the frontend.*

### 6. DTO & Exception Handling
- Introduced request and response **DTOs** to separate API models from JPA entities.
- Added application-specific exceptions and a global exception handler for consistent API error responses.

### 7. REST API Layer
- Implemented REST controllers for **Investor**, **Product**, **Withdrawal**, and **CSV Export** functionality.
- Controllers delegate business logic directly to the service layer.

### 8. Initial Data Seeding
- Added a `DataLoader` to populate initial investors, products, holdings, and demonstration data required for running and testing the system.

### 9. Frontend Development
- Built the UI using plain **HTML**, **CSS**, and **JavaScript**.
- Created separate pages and JS modules for:
    - Login / Authentication
    - Investor Portal
    - Administration
    - Settings
- Connected the frontend components to the Spring Boot REST API.

### 10. Final Integration & Verification
- Integrated end-to-end workflows for authentication, portfolio viewing, withdrawal creation, transaction history, and CSV export.
- Reviewed complete application structure and verified full consistency between the backend API and frontend JavaScript.

### Run the backend
```bash
cd withdrawa_system
mvn spring-boot:run
```
The API starts on **http://localhost:8080**. On every startup, `DataLoader`
seeds three demo investors, three products, and their holdings, so there's
data to log in with immediately.

Seeded investors (used on the login page's Investor tab):
| Investor | Age | Holdings |
|---|---|---|
| Thandiwe Nkosi | 70 | Retirement Annuity (R450,000), Savings Plan (R85,000) |
| Sipho Dlamini | 42 | Retirement Annuity (R210,000), Discretionary Investment (R60,000) |
| Lindiwe Khumalo | 66 | Retirement Annuity (R320,000), Savings Plan (R15,000), Discretionary Investment (R40,000) |

This spread deliberately exercises the age rule: Thandiwe (70) and Lindiwe (66)
can withdraw from their retirement annuities, Sipho (42) cannot.

You can inspect the database directly via the H2 console at
`http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:enviro365db`, user
`sa`, blank password).

### Run the tests
```bash
cd withdrawal_system
mvn test
```
Runs `WithdrawalServiceTest`, `PortfolioServiceTest` and `CsvExportServiceTest`, covering all three business rules plus the
structural error paths, using Mockito so no database is required.

### 1a. Why the frontend moved into the backend (Thymeleaf)

Earlier versions of this project served the frontend as flat static files
opened directly in a browser or from a separate `python3 -m http.server`, `php -S localhost:8080`.
That's gone now — the HTML pages moved to
`withdrawal_system/src/main/resources/templates/`, and the CSS/JS moved to
`withdrawal_system/src/main/resources/static/{css,js}/`, with the Spring Boot
`spring-boot-starter-thymeleaf` dependency added to `pom.xml`.

**Why:** a `@Controller` returning a bare view name (e.g. `return "login";`)
needs an actual `ViewResolver` that knows how to find a template called
`login`. Without Thymeleaf (or another template engine) on the classpath,
Spring falls back to `InternalResourceViewResolver`, which just forwards the
request to a relative path — for a request already at `/enviro365/login`,
that forwards back to `/enviro365/login` again, an infinite loop Spring
detects and rejects with a "Circular view path" error. Adding Thymeleaf and
moving the HTML into `templates/` gives Spring a real place to resolve
`"login"` to (`templates/login.html`), fixing that at the root cause.

The HTML/CSS/JS content itself didn't need to change for Thymeleaf — none of
it uses `th:*` attributes, and Thymeleaf renders plain HTML unchanged. What
*did* need to change is every hardcoded link between pages: when pages were
flat files sitting next to each other, `<a href="settings.html">` and
`window.location.href = "login.html"` worked by relative-path convention.
Now that pages are views rendered at controller-mapped URLs (see the table
below) and `templates/*.html` is not directly web-accessible, those had to
become absolute paths matching the controller's `@GetMapping`s — e.g.
`/enviro365/settings` — everywhere they occurred (in `admin.html`,
`index.html`, `settings.html`, and in `auth.js`, `login.js`, `settings.js`).
Similarly, `<link href="style.css">` and `<script src="app.js">` became
`/css/style.css` and `/js/app.js`, since those now come from Spring Boot's
static resource handler rather than sitting next to the HTML file.

### 1b. URL map

| URL | Serves |
|---|---|
| `GET /` | Redirects to `/enviro365/login` |
| `GET /enviro365/login` | `templates/login.html` |
| `GET /enviro365/investor/dashboard` | `templates/index.html` (investor dashboard) |
| `GET /enviro365/admin` | `templates/admin.html` |
| `GET /enviro365/settings` | `templates/settings.html` |
| `GET /css/style.css`, `GET /js/*.js` | Static assets, served automatically by Spring Boot |
| `GET/POST /api/**` | The REST API (unchanged, see section 3) |

### Run the frontend
There's nothing separate to run — once the backend is up
(`mvn spring-boot:run`), open **http://localhost:8080/** in a browser; it
redirects straight to the login page. `API_BASE_URL` in the JS files is now
the relative path `/api` (previously a hardcoded
`http://localhost:8080/api`), since the frontend and API are served from the
same origin — no CORS round trip needed, though `config/WebConfig.java`
still leaves CORS open on `/api/**` in case you ever split the frontend back
out to a separate host during development.

### Logging in
There is no backend authentication (see section 5 below) — login is a
frontend-only demo gate over data that already exists via the API:

- **Investor tab**: pick any seeded investor from the dropdown and enter any
  non-empty password → lands on the portfolio dashboard
  (`/enviro365/investor/dashboard`) for that investor.
- **Administrator tab**: username `admin`, password `Enviro365Admin!` → lands
  on the admin panel (`/enviro365/admin`).

---

## 2. Pages

| Page | URL | Template file | Purpose |
|---|---|---|---|
| Login | `/enviro365/login` | `templates/login.html` | Entry point. Investor or Administrator sign-in. |
| Dashboard | `/enviro365/investor/dashboard` | `templates/index.html` | Portfolio, withdrawal form, portfolio summary, withdrawal history. Investor-only. |
| Settings | `/enviro365/settings` | `templates/settings.html` | Read-only profile view, reached from the header user menu. |
| Admin panel | `/enviro365/admin` | `templates/admin.html` | Add new investors (with optional opening holdings) and browse all investors. Admin-only. |

Every protected page calls `requireRole(...)` (in `auth.js`) on load and
redirects to `/enviro365/login` if there's no matching session, so
navigating directly to `/enviro365/investor/dashboard` or `/enviro365/admin`
without logging in bounces you back.

The dashboard header no longer has an investor-picker dropdown — the investor
is fixed by who's logged in. In its place is a **user menu** (top-right) with
the investor's name and a dropdown for **Settings** and **Logout**.

The dashboard's card include a:
**Portfolio Summary** card: total portfolio value, number of products held,
total withdrawal attempts, and the most recent attempt's outcome/date — all
computed client-side from data the dashboard already loads.

---

## 3. API documentation

Base URL: `http://localhost:8080/api`

All error responses share this shape (produced centrally by
`GlobalExceptionHandler`):
```json
{
  "timestamp": "2026-08-12T10:15:30",
  "status": 400,
  "error": "Validation Failed",
  "message": "One or more fields are invalid.",
  "path": "/api/withdrawals",
  "validationErrors": ["amount: amount must be greater than zero"]
}
```

### GET `/investors`
List all investors (used by the login page and the admin investor table).
```
200 OK
[
  { "id": 1, "fullName": "Thandiwe Nkosi", "email": "thandiwe.nkosi@example.com",
    "dateOfBirth": "1956-07-22", "age": 70 }
]
```

### POST `/investors`  — *new: powers the admin "Add Investor" page*
Creates a new investor.
```json
// Request
{ "fullName": "Nomvula Zulu", "email": "nomvula.zulu@example.com", "dateOfBirth": "1958-03-14" }
```
- `201 Created` — investor created, response is an `InvestorDto`.
- `400 Bad Request` — missing/blank fields, invalid email format, or a date
  of birth that isn't in the past.
- `409 Conflict` — the email is already registered to another investor.

### GET `/investors/{id}/portfolio`
Investor details plus current holdings, including a pre-computed 90%-of-balance
figure so the frontend can pre-validate amounts without duplicating the maths.
```
200 OK
{
  "investor": { "id": 1, "fullName": "Thandiwe Nkosi", "email": "...", "dateOfBirth": "1956-07-22", "age": 70 },
  "holdings": [
    { "holdingId": 1, "productId": 1, "productName": "Enviro365 Retirement Annuity",
      "productType": "RETIREMENT_ANNUITY", "balance": 450000.00, "maxWithdrawable": 405000.00 }
  ]
}
```
`404` if the investor id doesn't exist.

### POST `/investors/{id}/holdings`  — *new: powers the admin "opening holdings" step*
Gives an investor a holding in a product.
```json
// Request
{ "productId": 1, "balance": 100000.00 }
```
- `201 Created` — holding created, response is a `HoldingDto`.
- `400 Bad Request` — missing/negative balance.
- `404 Not Found` — investor or product id doesn't exist.

### GET `/products`  — *new: powers the admin holding-row product dropdown*
```
200 OK
[ { "id": 1, "name": "Enviro365 Retirement Annuity", "type": "RETIREMENT_ANNUITY" } ]
```

### POST `/withdrawals`
Creates a withdrawal notice and applies the three business rules.
```json
// Request
{ "investorId": 1, "holdingId": 1, "amount": 5000.00 }
```
- **`201 Created`** — approved. Balance was deducted; response includes `balanceAfter`.
- **`422 Unprocessable Entity`** — a valid request that failed a business rule.
  The attempt is still recorded (so it appears in history) and `reason`
  explains why.
- **`400 Bad Request`** — structurally invalid request (missing/negative
  amount, unknown investor/holding, or a holding that doesn't belong to the
  given investor). Nothing is persisted.
- **`404 Not Found`** — investor or holding id doesn't exist.

```json
// 201 response
{
  "id": 12, "investorId": 1, "investorName": "Thandiwe Nkosi", "holdingId": 1,
  "productName": "Enviro365 Retirement Annuity", "requestedAmount": 5000.00,
  "status": "SUCCESS", "reason": null, "balanceAfter": 445000.00,
  "createdAt": "2026-08-12T10:15:30"
}
```
```json
// 422 response (e.g. investor is 42, product is retirement)
{
  "id": 13, "investorId": 2, "investorName": "Sipho Dlamini", "holdingId": 3,
  "productName": "Enviro365 Retirement Annuity", "requestedAmount": 5000.00,
  "status": "REJECTED",
  "reason": "Retirement withdrawals are only allowed for investors older than 65 (investor is 42).",
  "balanceAfter": null, "createdAt": "2026-08-12T10:16:02"
}
```

### GET `/withdrawals?investorId=&status=`
Withdrawal history. Both query params are optional and combinable.
`status` is `SUCCESS` or `REJECTED`. Returns a list of the same shape as the
POST response above, newest first.

### GET `/export/portfolio?investorId=`
Downloads a CSV of holdings (`investorId` optional — omit for all investors).
Columns: `InvestorId,InvestorName,Email,Age,ProductId,ProductName,ProductType,Balance,MaxWithdrawable90pct`

### GET `/export/withdrawals?investorId=&status=`
Downloads a CSV of the withdrawal history, with the same optional filters as
the history endpoint.
Columns: `WithdrawalId,InvestorId,InvestorName,ProductName,RequestedAmount,Status,Reason,BalanceAfter,CreatedAt`

### Example curl calls
```bash
curl http://localhost:8080/api/investors
curl http://localhost:8080/api/investors/1/portfolio
curl -X POST http://localhost:8080/api/investors \
  -H "Content-Type: application/json" \
  -d '{"fullName":"Nomvula Zulu","email":"nomvula.zulu@example.com","dateOfBirth":"1958-03-14"}'
curl -X POST http://localhost:8080/api/investors/4/holdings \
  -H "Content-Type: application/json" \
  -d '{"productId":2,"balance":25000.00}'
curl -X POST http://localhost:8080/api/withdrawals \
  -H "Content-Type: application/json" \
  -d '{"investorId":1,"holdingId":1,"amount":5000.00}'
curl "http://localhost:8080/api/withdrawals?investorId=1&status=REJECTED"
curl -OJ "http://localhost:8080/api/export/portfolio?investorId=1"
```

---

## 4. Implementation quality checklist

The brief asked for at least three of five. This project implements **all five**:

1. **Global exception handling** — `exception/GlobalExceptionHandler.java`, a
   single `@RestControllerAdvice` mapping `ResourceNotFoundException` (404),
   `DuplicateResourceException` (409), `BusinessRuleException` (400), Bean
   Validation failures (400, with a `validationErrors` list), type-mismatch
   errors (400), and a catch-all (500).
2. **DTO layer** — `dto/` package. Entities never leave the service layer;
   controllers only see/return DTOs, including the new
   `CreateInvestorRequestDto`, `CreateHoldingRequestDto`, and `ProductDto`
   added for the admin page.
3. **Input validation** — Jakarta Bean Validation (`@NotBlank`, `@Email`,
   `@Past`, `@DecimalMin`) enforced via `@Valid` on every write endpoint,
   backstopped by explicit service-layer checks (email uniqueness, holding
   must belong to the given investor).
4. **Unit tests** — `WithdrawalServiceTest.java` covers all three business
   rules (both sides of each boundary, including the exact 90% edge case),
   the persistence/rollback behaviour on success vs rejection, and the
   not-found path — all via Mockito, no DB needed.
5. **UI-side validation** — mirrors backend rules for instant feedback:
   `app.js`'s `validateOnClient()` for withdrawals, `admin.js`'s
   `validateInvestorForm()` for new investors. The backend remains the
   source of truth and re-validates everything server-side regardless.

---

## 5. Business rules — where they live

All three withdrawal rules are enforced in one place server-side:
`backend/.../service/WithdrawalService.java#evaluateBusinessRules()`.

1. **Retirement age rule** — if the holding's product is `RETIREMENT_ANNUITY`
   and `investor.getAge() <= 65`, reject. Age is derived from `dateOfBirth`
   via `Period.between(...)` on every request rather than stored.
2. **Balance cap** — `amount > holding.getBalance()` → reject.
3. **90% cap** — `amount > holding.getBalance() * 0.90` (rounded down to
   cents) → reject.

A rejection is **not** thrown as a Java exception — it's a normal business
outcome that gets persisted as a `WithdrawalNotice` with `status=REJECTED`
and a `reason`, specifically so it shows up in the withdrawal history table.
Only genuinely malformed requests throw and are rejected before hitting the
database.

---

## 6. Login / admin — security caveats (read this)

The login page and admin panel are **frontend-only conveniences**, not a real
authentication/authorization layer:

- There is no `/api/auth` endpoint, no password hashing, no server-side
  session or token. "Logging in" just writes a small JSON object
  (`{ role, investorId/name }`) to the browser's `localStorage` (see
  `auth.js`) and every protected page checks it client-side before rendering.
- The investor login accepts **any password** for a real, existing investor
  — the password field exists to demonstrate the login UX, not to actually
  authenticate.
- The admin credentials (`admin` / `Enviro365Admin!`) are hardcoded in
  `login.js` in plain text. Anyone who reads the frontend source has them.
- The REST API itself has **no authorization checks** — `POST /api/investors`
  works for anyone who can reach port 8080, logged in or not, via curl or
  otherwise. The frontend gate does not protect the API.

This is appropriate for the scope of this assessment (a working demo of the
required UI flows) but would need to be replaced before any real deployment
— e.g. Spring Security with a real user store, hashed passwords, and
server-issued JWTs/session cookies, with `@PreAuthorize` (or equivalent) on
the investor-creation and admin endpoints.

---

## 7. AI Usage Disclosure

This entire project (backend Java REST API, H2 persistence layer, vanilla HTML/CSS/JS frontend, and project documentation) was developed with AI assistance using **Claude Code**.

To maintain architectural integrity and ensure rigorous quality control, development was executed through a structured **Prompting Graph**. Rather than generating the project in a single unmonitored prompt, implementation proceeded incrementally through strict architectural checkpoints gated by existing unit tests (`PortfolioServiceTest`, `WithdrawalServiceTest`, and `CsvExportServiceTest`).

---

### Architectural Implementation Breakdown

* **Layered Spring Boot Architecture (`Entities → Repositories → Services → Controllers`)**  
  The backend follows standard Spring Boot conventions. For example, adding the administrator *"Create Investor"* workflow involved introducing a DTO (`CreateInvestorRequestDto`), adding a service method (`PortfolioService.createInvestor`) to validate business rules before calling `InvestorRepository.save`, and exposing a controller endpoint returning `201 Created`.

* **Service-Level Validation vs. Database Constraints**  
  Although the `Investor` entity enforces `@Column(unique = true)` on the email field, relying solely on database constraints throws a low-level `DataIntegrityViolationException` (resulting in a generic 500 error). To provide a clean API contract, `PortfolioService` explicitly checks `existsByEmailIgnoreCase` first and throws a `DuplicateResourceException` mapped to HTTP `409 Conflict`.

* **Frontend Authentication & State Management**  
  Because no OAuth2 or JWT provider was specified in the scope, client-side session management uses `localStorage` via a centralized `auth.js` module (`getSession`, `setSession`, `clearSession`, `requireRole`). This provides a seamless client-side experience for switching between **Investor** and **Administrator** views. *(Note: As disclosed in Section 6, this serves presentation logic; production deployments require real backend security filters).*

* **Sequential REST Operations for Complex Submissions**  
  The administrator form's *"Add Holding"* feature utilizes sequential POST calls (`submitHoldingRows` in `admin.js`) targeting `POST /api/investors/{id}/holdings` rather than introducing bulk-create backend endpoints. This keeps backend surface area minimal while providing partial-failure tolerance on the frontend (i.e., an issue with an individual holding entry does not roll back an otherwise valid investor creation).

* **Financial Precision**  
  All monetary calculations across entities, services, and DTOs (e.g., `CreateHoldingRequestDto.balance`) strictly utilize `BigDecimal` rather than floating-point primitives (`double`/`float`) to prevent precision and rounding errors.

* **Framework-Free Vanilla Frontend & Dynamic View Resolution**  
  The frontend relies exclusively on native DOM manipulation (`document.createElement`), `fetch()` operations, and modular JS scripts without external frameworks.

  To resolve template view handling, the application utilizes `spring-boot-starter-thymeleaf` to serve views from `src/main/resources/templates` while hosting assets in `src/main/resources/static`. View links rely on mapped relative paths (`/enviro365/login`, `/enviro365/investor/dashboard`, `/enviro365/admin`, `/enviro365/settings`), and `API_BASE_URL` resolves relatively to `/api` to avoid hardcoded host or port dependencies.

---

### Prompting Graph Execution Strategy

Development followed this sequential, test-gated execution plan:
Here is your revised Section 7 markdown disclosure, structured cleanly for inclusion in your README:

```markdown
## 7. AI Usage Disclosure

This entire project (backend Java REST API, H2 persistence layer, vanilla HTML/CSS/JS frontend, and project documentation) was developed with AI assistance using **Claude Code**. 

To maintain architectural integrity and ensure rigorous quality control, development was executed through a structured **Prompting Graph**. Rather than generating the project in a single unmonitored prompt, implementation proceeded incrementally through strict architectural checkpoints gated by existing unit tests (`PortfolioServiceTest`, `WithdrawalServiceTest`, and `CsvExportServiceTest`).

---

### Architectural Implementation Breakdown

* **Layered Spring Boot Architecture (`Entities → Repositories → Services → Controllers`)**  
  The backend follows standard Spring Boot conventions. For example, adding the administrator *"Create Investor"* workflow involved introducing a DTO (`CreateInvestorRequestDto`), adding a service method (`PortfolioService.createInvestor`) to validate business rules before calling `InvestorRepository.save`, and exposing a controller endpoint returning `201 Created`.

* **Service-Level Validation vs. Database Constraints**  
  Although the `Investor` entity enforces `@Column(unique = true)` on the email field, relying solely on database constraints throws a low-level `DataIntegrityViolationException` (resulting in a generic 500 error). To provide a clean API contract, `PortfolioService` explicitly checks `existsByEmailIgnoreCase` first and throws a `DuplicateResourceException` mapped to HTTP `409 Conflict`.

* **Frontend Authentication & State Management**  
  Because no OAuth2 or JWT provider was specified in the scope, client-side session management uses `localStorage` via a centralized `auth.js` module (`getSession`, `setSession`, `clearSession`, `requireRole`). This provides a seamless client-side experience for switching between **Investor** and **Administrator** views. *(Note: As disclosed in Section 6, this serves presentation logic; production deployments require real backend security filters).*

* **Sequential REST Operations for Complex Submissions**  
  The administrator form's *"Add Holding"* feature utilizes sequential POST calls (`submitHoldingRows` in `admin.js`) targeting `POST /api/investors/{id}/holdings` rather than introducing bulk-create backend endpoints. This keeps backend surface area minimal while providing partial-failure tolerance on the frontend (i.e., an issue with an individual holding entry does not roll back an otherwise valid investor creation).

* **Financial Precision**  
  All monetary calculations across entities, services, and DTOs (e.g., `CreateHoldingRequestDto.balance`) strictly utilize `BigDecimal` rather than floating-point primitives (`double`/`float`) to prevent precision and rounding errors.

* **Framework-Free Vanilla Frontend & Dynamic View Resolution**  
  The frontend relies exclusively on native DOM manipulation (`document.createElement`), `fetch()` operations, and modular JS scripts without external frameworks. 
  
  To resolve template view handling, the application utilizes `spring-boot-starter-thymeleaf` to serve views from `src/main/resources/templates` while hosting assets in `src/main/resources/static`. View links rely on mapped relative paths (`/enviro365/login`, `/enviro365/investor/dashboard`, `/enviro365/admin`, `/enviro365/settings`), and `API_BASE_URL` resolves relatively to `/api` to avoid hardcoded host or port dependencies.

---

### Prompting Graph Execution Strategy

Development followed this sequential, test-gated execution plan:


```
              ┌──────────────────────────┐
              │  1. Project Foundation   │
              └────────────┬─────────────┘
                           │
                           ▼
              ┌──────────────────────────┐
              │     2. Domain Model      │
              └────────────┬─────────────┘
                           │
                           ▼
              ┌──────────────────────────┐
              │   3. Repository Layer    │
              └────────────┬─────────────┘
                           │
        ┌──────────────────┴──────────────────┐
        ▼                                     ▼
┌───────────────────────┐             ┌───────────────────────┐
│ 4. Portfolio Service  │             │ 5. Withdrawal Service │
└───────────┬───────────┘             └───────────┬───────────┘
            │                                     │
            ▼                                     ▼
┌─────────────────────┐               ┌─────────────────────┐
│ PortfolioServiceTest│               │WithdrawalServiceTest│
│  (CHECKPOINT #1)    │               │  (CHECKPOINT #2)    │
└──────────┬──────────┘               └──────────┬──────────┘
           │                                     │
           └──────────────────┬──────────────────┘
                              ▼
                  ┌──────────────────────────┐
                  │  6. CSV Export Service   │
                  └────────────┬─────────────┘
                               │
                               ▼
                  ┌──────────────────────────┐
                  │   CsvExportServiceTest   │
                  │     (CHECKPOINT #3)      │
                  └────────────┬─────────────┘
                               │
                               ▼
                  ┌──────────────────────────┐
                  │ 7. DTOs & Exception Layer│
                  └────────────┬─────────────┘
                               │
                               ▼
                  ┌──────────────────────────┐
                  │    8. REST Controller    │
                  └────────────┬─────────────┘
                               │
                               ▼
                  ┌──────────────────────────┐
                  │  9. Bootstrap Data Loader│
                  └────────────┬─────────────┘
                               │
                               ▼
                  ┌──────────────────────────┐
                  │ 10. Authentication State │
                  └────────────┬─────────────┘
                               │
                               ▼
                  ┌──────────────────────────┐
                  │  11. Vanilla JS Frontend │
                  └────────────┬─────────────┘
                               │
                               ▼
                  ┌──────────────────────────┐
                  │12. End-to-End Integration│
                  └────────────┬─────────────┘
                               │
                               ▼
                  ┌──────────────────────────┐
                  │13. Final Verification    │
                  │    (`mvn clean test`)    │
                  └──────────────────────────┘

```

Every stage was verified against the codebase using `mvn clean test` before progressing down the graph, ensuring all implementation details fully satisfy both the business rules and technical constraints of the assessment.

```

---