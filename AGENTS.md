# Agent instructions

Instructions for AI assistants and agents working in this repository. Read this file before changing anything. It must stay consistent with `CONTRIBUTING.md` and with the QA Manual in `docs/`; if they disagree, stop and ask a human.

Items marked **(proposal)** are defaults the team has not confirmed yet. Follow them unless a human or a newer decision says otherwise, and never turn a proposal into a hard-coded assumption.

## 1. Project context

HiperEats is the TQS 2026/27 group project (University of Aveiro): a light-mobility **fleet-as-a-service** platform, built to show that quality was engineered in from the start. Quality evidence (tests, CI, quality gates, traceability) matters as much as features.

- **HiperEats owns a fleet of electric scooters** and supplies it as a managed service to a fictional **hypermarket chain** with restaurants and take-away counters.
- **Couriers** are employees of the chain. They take a scooter at their store, deliver meals nearby and return it to the **same store**.
- **HiperEats (operator and technicians)** manages the scooter lifecycle and does maintenance within an SLA.
- **The chain (client)** sets the rules of use (the same for every store), follows usage reports and business KPIs, and pays HiperEats a monthly plan priced by number of scooters and included km. Every km above the allowance is charged per km.
- **Overtime charges are paid by the chain** through Stripe in test mode. There is no deposit per use; a scooter damaged by negligence or missing is charged to the chain at its contract value (BR11). Couriers never pay.
- **Out of scope:** food ordering (orders come from a simulated generator: delivery address and time only), end customers, issuing invoices to the chain, real vehicles and real payments, native mobile apps.

Roles in the system: `COURIER`, `TECHNICIAN`, `OPERATOR`, `CHAIN_MANAGER`, `ADMIN`.

`README.md` has the short version of the concept and the project links. The full specification (personas, user stories, business rules with their IDs, SLOs, architecture) is the Product Specification report in `docs/`. When the code and the specification disagree, raise it in the pull request; do not silently pick one.

## 2. Repository layout

- `backend/`: Spring Boot, Java 21, Maven
- `frontend/`: web UI in React
- `simulator/`: fleet telemetry simulator, own container; talks to the backend only through the telemetry integration interface (MQTT)
- `docs/`: reports in PDF (Product Specification, QA Manual)
- `.github/`: CI workflows, pull request template, Dependabot

## 3. Architecture rules (backend)

Ports and adapters (hexagonal). Package layout **(proposal)**:

```
pt.ua.hipereats/
  domain/            entities, value objects, business rules; no Spring, JPA or HTTP imports
  application/       use cases; ports as interfaces (port/in, port/out)
  adapters/in/web/       REST controllers and request/response DTOs
  adapters/out/persistence/  JPA entities and repositories
  adapters/out/telemetry/    MQTT subscriber, telemetry message mapping
  adapters/out/payments/     Stripe client
```

- Business rules live in `domain/` and `application/`, never in controllers, DTOs or adapters.
- Controllers are thin: validate input, call a use case, map the result. Never expose JPA entities in the API.
- The fleet simulator and Stripe are reached **only** through their port interfaces. A different simulator, or a fake Stripe in tests, must be pluggable with no change to business logic. Never call the Stripe SDK outside `adapters/out/payments`.
- Two couriers may book the same scooter at once: bookings must be transactional and safe under concurrency (one wins, the other gets a clear error).

## 4. Domain rules

- **Scooter states:** `INACTIVE`, `AVAILABLE`, `RESERVED`, `IN_USE`, `MAINTENANCE`, `RETIRED`. A new scooter starts `INACTIVE` when the operator registers it at a store (BR13). Allowed transitions (Product Specification, 3.3):
  - inactive to available: operator activates the scooter
  - available to reserved: courier reserves the scooter (BR1 to BR4)
  - available to in use: courier starts a use immediately and the platform unlocks the scooter (BR1 to BR4, BR12)
  - reserved to in use: use starts and the platform unlocks the scooter (BR5, BR12)
  - reserved to available: reservation cancelled or expired (BR5)
  - in use to available: scooter locked at its own store and use ended (BR6, BR7, BR12)
  - in use to maintenance: use ended with low battery, with a fault, or with a problem reported by the courier (BR8)
  - available to maintenance: fault reported by telemetry, or sent by the operator (BR8, BR9)
  - maintenance to available: ticket closed by the technician (BR11)
  - available or maintenance to inactive: operator deactivates the scooter
  - inactive to retired: operator retires the scooter; `RETIRED` is final

  Any other transition is an error. A fault reported while the scooter is in use is recorded and handled when the use ends.
- **Business rules** have IDs (`BR<n>`) in the Product Specification. Refer to them by ID in code comments only where it helps, and always in test names or tags.
- **Rule parameters are configuration, not constants.** The chain manager changes only the reservation validity (BR5) and the low-battery alert level (BR8), for all stores at once. Every other value is fixed and comes from application configuration: range margin, delivery radius, maximum use duration, grace period, overtime fee, peak windows, SLA times, plan km and price per extra km. Take the values from the Product Specification; never guess them.
- **Time:** inject a `java.time.Clock`; never call `now()` directly in domain or application code. Boundary tests depend on it. Time zone `Europe/Lisbon`.
- **Money:** `BigDecimal`, never `double`.
- **Do not invent business rules.** If a story needs a rule the specification does not define, ask, or state the assumption in the pull request.

## 5. REST API conventions

- Resources, not actions, all under `/api/v1`: `/stores`, `/scooters`, `/bookings`, `/maintenance-tickets`, `/charges`, `/rules`, `/reports/usage`, `/reports/performance`, `/users`. Ending a use is an update of the booking, not a separate action endpoint.
- Plural nouns, `camelCase` JSON fields, ISO-8601 timestamps with offset.
- Status codes **(proposal)**: `201` with `Location` on creation, `400` invalid input, `401` not authenticated, `403` wrong role, `404` not found, `409` state conflict (scooter already taken), `422` business rule violated. Error bodies use Spring's `ProblemDetail`, naming the rule when one applies (for example `BR1`).
- Role checks happen in the backend on every endpoint, not only in the UI.
- Document every endpoint for OpenAPI and Swagger UI.

## 6. Telemetry contract (proposal)

- MQTT topic per scooter: `hipereats/scooters/{scooterCode}/telemetry`.
- JSON message: `scooterCode`, `timestamp`, `batteryPercent`, `latitude`, `longitude`, `locked`, `fault` (null when none).
- The contract is documented and tested on both sides, so another simulator can replace ours.

## 7. Testing rules

- **Every change comes with tests.** Business rules get unit tests that cover their boundary values (for example, for BR1 and a delivery at 3 km: a range of 7.2 km accepted, 7.1 km refused).
- **Test first (TDD) is a team rule.** Write the test, see it fail for the expected reason, then write the code. The failing tests are committed before the code, in a separate commit.
- **Unit tests** are named `*Test` and run with Surefire; **integration tests** are named `*IT` and run with Failsafe, separately **(proposal)**.
- **Integration tests** cover persistence, the adapters (telemetry, payments) and the REST API. They use Testcontainers for the database and the MQTT broker. Tests never call the real Stripe API in CI; use a simulated Stripe.
- **Acceptance tests:** Gherkin features with Cucumber, tagged with the rule they cover (`@BR1`) and the story key (`@HE-12`).
- **Traceability:** link JUnit tests to their Jira story with `@Requirement("HE-12")` from `xray-junit-extensions`, so CI results reach Xray.
- **End-to-end tests** (Playwright) do not run on every build. Load tests (k6) check the SLO thresholds.
- Test names follow `whenX_thenY`: `whenBatteryBelowRequiredRange_thenBookingIsRefused`. Use AssertJ for assertions; mock only ports, not domain classes.
- **Never weaken, skip or delete existing tests, or loosen quality gate settings, to make a build pass.**

## 8. Frontend

- React with Vite (JavaScript, JSX). Lint with oxlint (`.oxlintrc.json`); unit tests with Vitest and React Testing Library, next to the component (`App.test.jsx`).
- In development, Vite forwards `/api` to the backend on `localhost:8080`; in Docker, nginx does the same. Always call the API with relative paths (`/api/v1/...`).
- One web UI with views per role; the backend is the source of truth for permissions.
- Use semantic HTML and labelled form fields (accessibility checks are a valued practice). Add `data-testid` attributes where Playwright needs stable selectors.

## 9. Security and data

- Never write secrets, credentials or real personal data in code, tests, fixtures or commits. Use synthetic data only.
- Stripe test keys and other secrets come from environment variables, set from GitHub Secrets in CI. `.env` files are not committed (see `.gitignore`).

## 10. Git and pull requests

- Follow `CONTRIBUTING.md` for code style, branch names and commit messages: branch `HE-<n>-short-description`, commits starting with the story key.
- Work on a feature branch and propose every change as a pull request: one story per pull request, small changes.
- Never push to `main` and never merge. A human reviews and approves every pull request.
- Fill in the pull request template, including the **AI use** section: what was produced by an agent, and which AI review findings were accepted or rejected.

## 11. Definition of done

See the definition of done in `CONTRIBUTING.md`.

## 12. Build and test commands

Backend (in `backend/`, Java 21):

- `./mvnw test`: unit tests (`*Test`, Surefire)
- `./mvnw verify`: unit and integration tests (`*IT`, Failsafe) and packaging
- `./mvnw spring-boot:run`: run on `localhost:8080`

Frontend (in `frontend/`, Node 22):

- `npm ci`: install dependencies
- `npm test`: unit tests (Vitest)
- `npm run lint`: lint (oxlint)
- `npm run dev`: development server on `localhost:5173`
- `npm run build`: production build into `dist/`

Whole stack (repository root):

- `docker compose up --build`: web UI on `localhost:3000`, backend on `localhost:8080`, MQTT broker on `localhost:1883`
- `docker compose down`: stop it

Run the tests of every part you changed before opening a pull request.
