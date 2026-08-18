Prompt 1 — Foundation
Inspect the existing project.

Implement the project foundation only.

Requirements:

- Verify pom.xml
- Configure Spring Boot
- Configure H2
- Configure Spring Data JPA
- Configure application.properties
- Verify Enviro365Application starts
- Do not implement controllers or frontend yet
- Do not modify existing tests

Run:

mvn test

Report:
1. What you changed
2. What already existed
3. Test result
4. Any assumptions made

Do not proceed to the next architectural layer until the application compiles.

Prompt 2 — Domain Model
Implement the domain model for the Enviro365 withdrawal system.

Use the existing entity package.

Implement/verify:

- Investor
- Product
- ProductType
- Holding
- WithdrawalNotice
- WithdrawalStatus

Requirements:

- Use JPA entities
- Establish appropriate relationships
- Add appropriate validation constraints
- Use suitable monetary types
- Define enums where appropriate
- Add constructors/getters/setters as appropriate
- Avoid business logic that belongs in services
- Keep entity responsibilities clear

Do not implement controllers or frontend.

After implementation:

1. Compile the project
2. Run the existing tests
3. Fix compilation issues without changing test intent
4. Explain the entity relationships.

Prompt 3 — Repositories
Implement the repository layer.

Use:

com.enviro.assessment.junior.sive.repository

Implement/verify:

- InvestorRepository
- ProductRepository
- HoldingRepository
- WithdrawalNoticeRepository

Requirements:

- Extend appropriate Spring Data repository interfaces
- Add only query methods that are required by the business logic
- Avoid unnecessary custom queries
- Ensure relationships can be queried efficiently enough for this assessment

Do not implement controllers.

Run:

mvn test

Do not change tests to make them pass.

Prompt 4 — Portfolio Service
Implement PortfolioService.

Study PortfolioServiceTest.java before implementing the service.

The tests are the authoritative behavioural specification for this service.

Requirements:

- Retrieve an investor's portfolio
- Retrieve holdings
- Calculate/return the portfolio information expected by the DTOs/tests
- Handle investor-not-found cases appropriately
- Use repositories rather than direct database access
- Keep business logic inside the service

Do not modify PortfolioServiceTest.java.

After implementation run:

mvn -Dtest=PortfolioServiceTest test

If a test fails:
1. Explain why
2. Fix production code
3. Re-run the test

Do not alter the test simply to accommodate the implementation.

Only continue when PortfolioServiceTest passes.

Prompt 5 — Withdrawal Service
Implement WithdrawalService.

First inspect:

- WithdrawalServiceTest.java
- WithdrawalNotice
- WithdrawalStatus
- Investor
- Holding
- WithdrawalRequestDto
- WithdrawalResponseDto

Treat WithdrawalServiceTest.java as the behavioural contract.

Implement:

- Withdrawal notice creation
- Validation
- Required business rules
- Withdrawal history
- Approved/rejected status handling
- Investor lookup
- Appropriate exception handling

Keep the business logic in WithdrawalService.

Do not implement frontend functionality yet.

Do not modify WithdrawalServiceTest.java.

Run:

mvn -Dtest=WithdrawalServiceTest test

Continue fixing production code until the existing test passes.

After the test passes, provide a short explanation of every business rule implemented.

Prompt 6 — CSV Export
Implement CsvExportService.

First inspect CsvExportServiceTest.java.

The existing test defines the expected CSV behaviour.

Implement:

- Withdrawal history export
- CSV headers
- Correct field ordering
- Correct formatting
- Correct handling of multiple records
- Correct handling of empty history where required

Do not modify CsvExportServiceTest.java.

Run:

mvn -Dtest=CsvExportServiceTest test

Fix production code until the test passes.

Then run:

mvn test

Prompt 7 — DTOs + Exceptions
Complete the DTO and exception layer.

Review:

dto/
exception/

Implement/verify:

- CreateHoldingRequestDto
- CreateInvestorRequestDto
- ErrorResponseDto
- HoldingDto
- InvestorDto
- PortfolioResponseDto
- ProductDto
- WithdrawalRequestDto
- WithdrawalResponseDto

Implement/verify:

- BusinessRuleException
- DuplicateResourceException
- ResourceNotFoundException
- GlobalExceptionHandler

Requirements:

- API should not unnecessarily expose JPA entities
- Validation errors must return useful responses
- Resource-not-found errors should use appropriate HTTP status
- Duplicate resources should use appropriate HTTP status
- Business-rule failures should use appropriate HTTP status
- Error responses should be consistent

Do not modify existing service tests.

Run:

mvn test

Prompt 8 — REST API
Implement the REST/API controller layer.

Review all service methods before implementing controllers.

Implement/verify:

- InvestorController
- WithdrawalController
- ProductController
- ExportController
- InvestorViewController

Requirements:

Investor functionality:
- Login-related API support
- Retrieve investor information
- Retrieve portfolio
- Retrieve withdrawal history
- Submit withdrawal notice
- Export withdrawal history

Administrator functionality:
- Add new investor
- Appropriate validation
- Duplicate investor handling

Withdrawal functionality:
- Create withdrawal notice
- Return appropriate response DTO
- Return meaningful HTTP status codes

Product functionality:
- Retrieve available products where required

CSV functionality:
- Return a downloadable CSV response
- Set appropriate content type
- Set appropriate Content-Disposition header

Do not place business logic in controllers.

Controllers should delegate to services.

After implementation run:

mvn test

Then inspect the endpoints and provide a table containing:

METHOD | ENDPOINT | PURPOSE | REQUEST | RESPONSE | STATUS CODES

Prompt 9 — Seed Data
Implement DataLoader.

The application must start with usable demonstration data.

Create realistic seed data for:

- Admin user
- Investors
- Products
- Holdings
- Withdrawal notices where useful for demonstrating history

Requirements:

- Do not create duplicate records every time the application starts
- Keep seed data deterministic
- Make the credentials clearly documented
- Ensure seeded data can be used to demonstrate the complete assessment workflow

After implementation:

mvn clean test

Then start the application and verify the seeded data can be accessed through the API.

Prompt 10 — Authentication
Implement the authentication mechanism required by the assessment.

There are two roles:

INVESTOR
ADMIN

Requirements:

Investor:
- Login
- Access own portfolio
- Access own withdrawal history
- Submit withdrawal notices
- Export own withdrawal history

Administrator:
- Separate login
- Add new investors
- Access administrator functionality only

Keep authentication appropriate for this assessment.

Do not introduce unnecessary enterprise security infrastructure unless required.

Ensure an investor cannot retrieve another investor's private portfolio or withdrawal history.

Ensure admin functionality is not accessible to ordinary investors.

Update the frontend authentication state accordingly.

Run:

mvn clean test

Prompt 11 — Frontend
Implement the vanilla HTML/CSS/JavaScript frontend.

Use the existing structure:

templates/
admin.html
index.html
login.html
settings.html

static/
css/style.css
js/admin.js
js/app.js
js/auth.js
js/login.js
js/settings.js

Do not introduce React, Angular, Vue or another frontend framework.

Implement the following user journeys.

## Investor

Login
↓
Dashboard
↓
View portfolio
↓
View withdrawal history
↓
Submit withdrawal notice
↓
See resulting withdrawal status/history
↓
Export withdrawal history to CSV

## Administrator

Admin login
↓
Admin dashboard
↓
Add investor
↓
See successful/failed result

Requirements:

- Responsive layout
- Clear navigation
- Form validation
- Loading states where useful
- Error messages
- Success messages
- Logout
- Authentication state handling
- API error handling
- CSV download
- No frontend framework

Keep JavaScript modular according to the existing files.

Do not change the backend architecture unnecessarily.

After implementation start the application and test both user journeys manually.

Prompt 12 — Full Integration Test
Perform a complete integration review of the Enviro365 withdrawal management system.

Do not immediately modify code.

First inspect the entire implementation.

Verify this investor workflow:

1. Investor opens login page
2. Investor logs in
3. Investor sees portfolio
4. Investor sees holdings
5. Investor submits withdrawal notice
6. Withdrawal is stored
7. Withdrawal appears in history
8. Approved/rejected records are displayed correctly
9. Investor exports withdrawal history to CSV
10. Investor logs out

Verify this administrator workflow:

1. Admin opens login page
2. Admin logs in
3. Admin opens administrator page
4. Admin creates an investor
5. Duplicate investor is rejected appropriately
6. Admin logs out

Verify security boundaries:

- Investor cannot access another investor's data
- Investor cannot access admin functionality
- Unauthenticated requests are handled correctly

Verify API behaviour:

- Correct HTTP methods
- Correct HTTP status codes
- Correct DTOs
- Correct error responses
- Correct validation

Verify persistence:

- Data is persisted in H2
- Relationships are correct
- Seed data loads correctly

Do not modify tests.

Run:

mvn clean test

Fix production issues found during the review.

