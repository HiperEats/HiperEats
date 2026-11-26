# Contributing to HiperEats

Guidelines for every contributor, human or agent: how to run the project, how a story reaches `dev` and then `main`, and what we expect from the code. Keep this file consistent with `AGENTS.md` and with the QA Manual, which will explain the rationale (first version planned for Iteration 2, HE-42).

## Where things are

| What | Where |
|---|---|
| Product concept, team and project links | [README.md](README.md) |
| User stories, sprint and their state | [Jira backlog](https://hipereats.atlassian.net/jira/software/projects/HE/boards/2/backlog) |
| User stories, business rules (`BR<n>`), lifecycles, SLOs and API | [Product Specification](docs/HiperEats%20-%20Product%20Specification%20report.pdf) |
| Team decisions, by meeting | [Meeting minutes](docs/Minutes); a later decision replaces an earlier one |
| Package layout, domain, REST, telemetry and testing rules | [AGENTS.md](AGENTS.md); they apply to humans too |
| Gherkin scenarios | [backend/src/test/resources/features](backend/src/test/resources/features) |
| CI pipeline | [.github/workflows/ci.yml](.github/workflows/ci.yml) |

`backend/` is the Spring Boot platform, `frontend/` the React web UI, `simulator/` the fleet simulator (MQTT contract in `simulator/README.md`) and `infra/` the configuration of the other containers.

## Set up the project

You need Git, **Java 21**, **Node 22** (22.22.2 or newer, with npm) and **Docker** with Compose. Maven comes with the repository (`./mvnw`; on Windows, `mvnw.cmd`).

```bash
git clone https://github.com/HiperEats/HiperEats.git
cd HiperEats
cp .env.example .env      # once; then set your own POSTGRES_PASSWORD in .env
docker compose up --build
```

`.env` holds the database credentials and is never committed. Every `docker compose` command needs it.

Open <http://localhost:3000>: the page shows the status of the backend. The backend answers on <http://localhost:8080/api/status>, PostgreSQL listens on `localhost:5432` (this machine only) and the MQTT broker on port 1883. Stop everything with `docker compose down`; the database data stays in the `db-data` volume, and `docker compose down -v` erases it.

To work on one part without rebuilding the containers, run the backend and the frontend in separate terminals. Start only the database in Docker; the backend container must be stopped, because both use port 8080:

| Directory | Command | Result |
|---|---|---|
| repository root | `docker compose up -d db` | PostgreSQL on `localhost:5432` |
| `backend/` | `set -a; . ../.env; set +a; DB_URL=jdbc:postgresql://localhost:5432/$POSTGRES_DB DB_USER=$POSTGRES_USER DB_PASSWORD=$POSTGRES_PASSWORD ./mvnw spring-boot:run` | backend on `localhost:8080`, using the database of `.env` |
| `frontend/` | `npm ci`, then `npm run dev` | web UI on `localhost:5173`, which forwards `/api` to the backend |

### Common problems

| What you see | Cause and fix |
|---|---|
| `address already in use` (Docker) or `Port 8080 was already in use` (backend) | Another program uses the port. Run `docker compose down`, or stop the program that uses 8080, 3000, 5432 or 1883. |
| `Cannot connect to the Docker daemon` | Docker is not running. Start it and try again. |
| `required variable POSTGRES_PASSWORD is missing a value` | There is no `.env`. Run `cp .env.example .env` and set a password. |
| `password authentication failed for user` | The password in `.env` changed after the database was created. Run `docker compose down -v` (erases the local data) and start again. |
| Integration tests fail with `Could not find a valid Docker environment` | Testcontainers needs Docker running for `./mvnw verify`. |
| `release version 21 not supported` | Maven runs with an older Java. Check it with `./mvnw -version` and point `JAVA_HOME` to Java 21. |
| `EBADENGINE` warnings in `npm ci` | Node is too old or an unsupported version. Check it with `node --version`. |

## Run the tests

Run the tests of every part you changed before opening a pull request. CI runs `./mvnw verify` (backend and simulator) and all the frontend commands.

| Directory | Command | What it runs |
|---|---|---|
| `backend/` | `./mvnw test` | unit tests (`*Test`) |
| `backend/` | `./mvnw clean verify` | unit, integration (`*IT`) and Cucumber acceptance tests, and the coverage report of all of them |
| `backend/` | `./mvnw test -Dtest=StatusControllerTest` | one unit test class |
| `simulator/` | `./mvnw test` | unit tests (`*Test`) |
| `simulator/` | `./mvnw verify` | unit and integration (`*IT`) tests, with Mosquitto in Docker |
| `frontend/` | `npm ci` | installs the dependencies, needed before the commands below |
| `frontend/` | `npm run lint` | lint (oxlint) |
| `frontend/` | `npm test` | unit tests (Vitest) |
| `frontend/` | `npm run build` | production build |

If you changed only documentation (Markdown files or images), you do not need to set up the project or run the tests: check the links and how the page looks on GitHub. The branch, the pull request and the approval are the same, and CI still runs.

Reports: `backend/target/surefire-reports/` (unit), `backend/target/failsafe-reports/` (integration), `backend/target/cucumber/cucumber.html` (acceptance) and `backend/target/site/jacoco/index.html` (coverage). Without `clean`, the coverage of earlier runs is added to the report.

## Work process

The short version of how the team works; the QA Manual (HE-42) will have the full one, and the two must agree.

### Iterations

- Five one-week iterations, from Friday to Friday. Each one starts at the checkpoint in the practical class (P4, Fridays), where we show the outcomes of the one that ends.
- **Kick-off meeting**, on the checkpoint day: we record the professor's feedback, plan the iteration (sprint planning) and create its sprint in Jira with the stories and tasks we commit to. Each member takes stories and tasks of their own.
- **Grooming:** the Product Owner writes and refines the stories of the next iteration; the team reviews them, estimates their story points and checks the definition of ready before they enter a sprint.
- **Team meetings:** one or two a week. The minutes are in [docs/Minutes](docs/Minutes), one file per meeting (`minutes-YYYY-MM-DD[-<topic>].md`, the topic is optional), with the decisions, who does what, and the open questions.
- **End of the iteration:** everything for the checkpoint is merged into `dev`, `dev` goes to `main` through a pull request **(proposal)**, the reports are exported as PDF to `docs/`, and `main` is tagged `iter<n>` (`iter1`, `iter2`, ...). The final presentation (18 Dec) is tagged `v1.0`. The burndown and velocity charts of the sprint are in Jira (Reports).

### From the backlog to done

1. Grooming: the story meets the [definition of ready](#definition-of-ready) and enters the sprint at sprint planning. Jira: **To Do**.
2. A member takes it, creates its branch and writes the tests first, at least for the business rules (see [Workflow](#workflow) and [Tests](#tests)). Jira: **In Progress**.
3. Pull request to `dev`, with the template filled in. CI runs the build, the tests and the SonarCloud quality gate; Claude reviews it, and another member reviews it (see [Reviews](#reviews)). Jira: **In Review**.
4. Approval, every conversation resolved and the required checks green; the author merges (see [Merge rules](#merge-rules)). On `dev`, CI imports the test results into Xray, where the story shows its coverage (HE-41).
5. The story meets the [definition of done](#definition-of-done) and the Product Owner accepts it. Jira: **Done**.

### Roles

Everyone develops: implements and tests stories, with their own pull requests and commits. On top of that, each member holds one or more roles (who holds each one is in the [README](README.md#project-team)):

| Role | Does |
|---|---|
| Team Coordinator | Distributes the tasks fairly and checks that the team follows the plan; acts early on problems; makes sure the outcomes are delivered on time |
| Product Owner | Represents the client and knows the domain; writes and refines the stories and the Product Specification; clarifies features for the team; accepts the story increments |
| QA Engineer | Promotes the QA practices with the other roles; makes sure the tools that measure quality are in place (quality gate, Xray, dependency scanning, AI review); checks that the team follows the agreed practices, including the AI rules |
| DevOps Master | Makes sure the development and production infrastructure is in place: repository and its rules, CI/CD pipelines, containers, deployment environment, databases |

A role answers for an area; the tasks themselves are distributed among everyone at the kick-off meeting. For example, the quality gate (HE-40) is Daniel's (decision 5 of the [minutes of 20 Nov](docs/Minutes/minutes-2026-11-20-i2-kickoff.md)).

## Workflow

We follow GitHub flow with an integration branch, `dev`. Every change reaches `dev` through a pull request, and `main` receives changes only through a pull request from `dev`. Both branches are protected, and `main` is always deployable.

`dev` goes to `main` at the end of each iteration, with CI passing **(proposal)**.

1. Pick a story or task from the current sprint in Jira (see [Stories](#stories)) and move it to **In Progress**.
2. Create a branch from an up-to-date `dev`, named after the story key: `HE-<n>-short-description` (for example `HE-12-booking-endpoint`).

   ```bash
   git fetch origin
   git switch --no-track -c HE-12-booking-endpoint origin/dev
   ```

3. Write the tests first, see them fail and commit them; then write the code and commit it separately. This is mandatory at least for the business rules (see [Tests](#tests)).
4. Start every commit message with the story key: `HE-12 add booking endpoint`.
5. Run the tests and push: `git push -u origin HE-12-booking-endpoint`.
6. Open a pull request to `dev` whose title starts with the story key (`HE-12 Add booking endpoint`), fill in the template and move the story to **In Review**. One story per pull request.
7. Answer the review. CI runs the build and the tests, and Claude reviews the pull request. Record in the template which of its findings you accepted or rejected, and why.
8. At least one other member approves and CI passes. The author merges. The story moves to **Done** once it meets the [definition of done](#definition-of-done) and the Product Owner accepts it.

Agents never merge. A human approves every pull request.

### Stories

- Write stories in English as `As a <role>, I want to <action>, so that <benefit>`.
- Keep few acceptance criteria: only those that delimit the story.
- Each epic is one general theme of the scope; split an epic that grows too broad.
- Work of the *Platform and quality* epic is created as Tasks, not stories. Tasks follow the same branch and pull request workflow.

### Merge rules

- GitHub blocks direct pushes to `dev` and `main`, and blocks the merge until another member has approved and every review conversation is resolved. A new push cancels the approvals already given.
- GitHub also blocks the merge until these checks pass: `Backend (build and tests)`, `Simulator (build and tests)`, `Frontend (lint, tests and build)` and `Quality gate (SonarCloud)`. The ruleset of `dev` and `main` requires them from the merge of HE-40 on; until then, never merge with CI failing. The quality gate also fails when the backend job fails. The gate is SonarCloud with JaCoCo and *Sonar way* on the new code of the pull request: at least 80% coverage, at most 3% duplication, A ratings, security hotspots reviewed. SonarCloud ignores coverage and duplication below 20 new lines. The job `Quality gate (SonarCloud)` fails when the gate fails; the SonarCloud check of the pull request says why. It covers the backend only. Pull requests from Dependabot or from forks get no SonarCloud token, so the job passes without an analysis and the reviewer looks closer.
- A dependency check in CI on every build is also decided and not in place yet (HE-47).
- GitHub does not check yet that a pull request to `main` comes from `dev`: check the base and the source branch before you approve or merge.
- Choose **Create a merge commit**, also from `dev` to `main`. Squash would join the failing tests and the code in one commit, and the history would no longer show that the tests came first.
- GitHub deletes the story branch after the merge. The rules of the repository do not allow deleting `dev` or `main`.

## Code style

- **Java:** [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html). Four-space indentation is accepted to match IDE defaults.
- Everything in the repository is in English: code, comments, commits, pull requests and documents. Classes are nouns, methods are verbs, tests follow `whenX_thenY` (`whenBatteryBelowRequiredRange_thenBookingIsRefused`).
- Business rules live in the domain/service layer, never in controllers or adapters. The package layout is in [AGENTS.md, section 3](AGENTS.md#3-architecture-rules-backend).
- External systems (telemetry simulator, payment gateway) are reached only through a port interface and its adapter.
- No secrets in the repository. Configuration comes from environment variables or GitHub Secrets.
- **Frontend (React):** the code passes `npm run lint` (oxlint). _Style guide to define._

## Tests

- **Test first (TDD)**, mandatory at least for the business rules (BR): write the test, see it fail for the expected reason, then write the code. Commit the failing tests before the code, in a separate commit. Only the last commit of the pull request has to pass CI. [HE-45](https://github.com/HiperEats/HiperEats/pull/8/commits) is an example: first `HE-45 Add failing tests for the PostgreSQL persistence base`, then `HE-45 Add PostgreSQL, JPA and Flyway with the persistence base`.
- Name tests `whenX_thenY`: the situation and the observable result, not the method called. Tests of a business rule also have a `@DisplayName` that starts with the rule ID. Each test follows Arrange, Act, Assert and checks one behaviour.

  ```java
  @Test
  @DisplayName("BR1: booking refused below the required range")
  void whenBatteryBelowRequiredRange_thenBookingIsRefused() { ... }
  ```

- Unit tests for business logic, with the boundary values of each rule; integration tests (adapters, REST API, and Testcontainers with PostgreSQL 18 and Mosquitto) run separately from unit tests.
- Acceptance criteria of core stories are Gherkin scenarios automated with Cucumber, tagged with the story (`@HE-29`) and the business rule they cover (for example `@BR1`).
- Scenarios of stories not implemented yet are tagged `@wip` and left out of the build. When you implement a story, write its steps in `backend/src/test/java/pt/ua/hipereats/acceptance` and remove `@wip` from its feature file.
- Tests are linked to their Jira story so that Xray reports coverage: the story tag in Gherkin, and `@Requirement` in JUnit once `xray-junit-extensions` is in `backend/pom.xml`.
- Never weaken, skip or delete a test to make a build pass.

## AI use

AI tools are welcome; the responsibility stays with the person who opens the pull request.

- Read, run and test the code before you ask for a review. Only submit what you can explain: anyone on the team may be asked to explain any merged code.
- The same rules apply to all code, including the tests and the review.
- Give agents [AGENTS.md](AGENTS.md) before they change anything.
- In the **AI use** section of the pull request, say what was produced with AI, and which Claude review findings you accepted or rejected and why.

## Definition of done

A story is done when:

- its acceptance criteria are met and automated;
- at least for the business rules (BR), the tests were written first (TDD): the failing tests were committed before the code, in a separate commit;
- the required unit and integration tests are written and pass;
- CI is green and the SonarCloud quality gate (HE-40) passes on new code;
- the pull request was approved by another member;
- the Product Owner accepted the increment.

## Reviews

Start each comment with its weight, and say what goes wrong and how to fix it:

- **Important:** must be fixed, or agreed with the reviewer, before the merge. Use *Request changes* only when there is at least one.
- **Minor:** a suggestion; the author decides.

Every conversation must be resolved before the merge, so the author answers the minor ones too.

### Review checklist

- [ ] The pull request title and branch carry the story key.
- [ ] The change does what the story's acceptance criteria ask, and nothing unrelated.
- [ ] Business rules are tested, including boundary values.
- [ ] No business logic in controllers or adapters.
- [ ] No secrets, credentials or personal data.
- [ ] Names and structure are readable without extra explanation.
- [ ] AI review findings were considered, and the accepted or rejected ones are noted in the pull request.
