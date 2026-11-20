# Quick Retrospective Log (Sprints 1–7)

> **Framework:** 4-Box Retrospective (**Continue**, **Stop**, **Invent**, **Act**)  
> **Course:** PMIS — SDU University  
> **Team:** forq-forq-forq  
> **Project:** AI-Powered Academic Advisor  

---

## 🧭 Retrospective Framework Overview

Each sprint concludes with a Quick Retrospective ceremony following the 4-quadrant format:

| Quadrant | Guiding Question | Focus |
| :--- | :--- | :--- |
| **Continue ⏩** | *What helped us move forward?* | Practices, tools, and team habits that delivered positive outcomes and should be sustained. |
| **Stop ⛔** | *What held us back?* | Bottlenecks, anti-patterns, time-wasters, or errors that need to be eliminated immediately. |
| **Invent 🌸** | *How could we do things differently?* | Creative experiments, fresh ideas, and new workflows to overcome recurring issues. |
| **Act 💪** | *What should we do next?* | Concrete, measurable action items with clear owners assigned for the upcoming sprint. |

---

## 🏃‍♂️ Sprint 1: Project Setup, Initial Architecture & MVP Foundation

* **Timeline:** September 2026
* **Sprint Focus:** Spring Boot 3 initialization, core domain entity modeling (`Student`, `Course`, `Prerequisite`), Gemini API client configuration (`RestClient`), baseline login & dashboard scaffolding, Docker & CI/CD pipeline setup, User Story Map & Lean Canvas delivery.
* **Completed Stories:** `T-01` (Docker Compose), `T-02` (CI Workflow), `US-01` (Student Auth/Profile MVP), `US-07` (Gemini API Integration).

### ⏩ Continue (What helped us move forward?)
* **Pre-testing prompts in Google AI Studio:** Validating JSON schemas and token response latencies in AI Studio prior to writing Java integration code saved hours of backend debugging.
* **Granular User Story Mapping:** Having a structured 3-milestone breakdown with predefined acceptance criteria in `user-story.yml` aligned the entire team and eliminated scope ambiguity.
* **Automated CI via GitHub Actions:** Enforcing test suites and multi-stage Docker builds on every pull request immediately surfaced environment incompatibilities.
* **Pair programming on complex infrastructure:** Teaming up on the Gemini `RestClient` and custom exception hierarchy (`GeminiApiException`) prevented integration bugs early on.

### ⛔ Stop (What held us back?)
* **Database indecision (SQLite vs. PostgreSQL):** Maintaining SQLite for quick prototyping led to Docker driver mismatches and profile configuration confusion (`application.properties`).
* **Delayed Pull Request reviews:** PRs sat without peer review for 2–3 days, causing feature branches to diverge significantly from `main`.
* **Manual "in-browser" testing:** Manually creating students, courses, and enrollments through forms instead of relying on programmatic database seeders wasted testing cycles.
* **Overly prolonged sync calls:** Voice syncs frequently drifted into theoretical architecture debates instead of addressing immediate daily blockers.

### 🌸 Invent (How could we do things differently?)
* **Automated demo seeding (`DataSeeder`):** Package pre-populated curricula and transcript fixtures so that developers and frontend contributors immediately see populated dashboards.
* **Asynchronous daily standups in Telegram:** Replace 40-minute voice meetings with a quick 3-bullet text update before 11:00 AM (1: What was done? 2: What is planned today? 3: Any blockers?).
* **Floating Copilot widget instead of a standalone chat tab:** Transition from a dedicated chat page to a cross-cutting, context-aware floating drawer available on every page.
* **Strict environment variable verification:** Add startup diagnostics for `.env` variables so containers fail fast with informative messages rather than cryptic exit codes.

### 💪 Act (What should we do next?)
1. [ ] **Backend Team:** Finalize the migration to PostgreSQL and Flyway versioned migrations during Sprint 2.
2. [ ] **Scrum Master:** Enforce a strict 24-hour SLA on Pull Request reviews (at least 1 peer approval required before merging).
3. [ ] **AI Engineer:** Implement student context injection (GPA, completed courses, credit caps) into the Gemini system prompt for safe semester recommendations (US-08).
4. [ ] **Frontend Contributor:** Implement the visual layout for the `Course Planner` workbench with course cards and ECTS workload counters.

---

## 🏃‍♂️ Sprint 2: Core Academic Engine & Context Injection (MVP Part 2)

* **Timeline:** October 2026
* **Sprint Focus:** Prerequisite validation engine (US-12), Semester cart with persistent storage (US-11), 1-click clipboard export (US-13), Credit limit warning system (US-14), Academic standing & progress ring (US-07), Enrolled courses list (US-08), and Docker multi-profile integration.
* **Completed Stories & Tasks:** `US-04`, `US-07`, `US-08`, `US-11`, `US-12`, `US-13`, `US-14`, Docker PostgreSQL configuration, Comprehensive unit/integration test suite (66 tests passed).

### ⏩ Continue (What helped us move forward?)
* **200% Velocity Increase (+2x completed stories):** The team closed twice as many User Stories compared to Sprint 1 (`US-04`, `US-07`, `US-08`, `US-11`, `US-12`, `US-13`, `US-14`). The refined sprint cadence and granular story decomposition proved highly effective.
* **Traceable Documentation via GitHub Issues:** Enforcing detailed GitHub Issue specifications (Acceptance Criteria, Given/When/Then QA scenarios, Story Points) and maintaining `current-architecture-baseline.md` significantly reduced regression bugs and architectural discrepancies.
* **Unified Docker Environment:** Containerizing the local development stack (PostgreSQL + Spring Boot) eliminated "works on my machine" issues and streamlined feature verification across the entire team.
* **100% Green CI & Automated Test Suite:** Maintaining a comprehensive test suite (66 unit and integration tests passing) caught regressions instantly when refactoring complex prerequisite validation and credit cap logic.

### ⛔ Stop (What held us back?)
* **Manual Time Management & PM Micromanagement:** The PM/Scrum Master had to continuously ping and tag developers manually in chat to ensure tasks were picked up on time, wasting valuable hours.
* **Relying on Generic Web-Based AI Chats for Code Generation:** Developers occasionally used context-blind web chats, resulting in low-quality snippets, improper package structures, and duplicate logic that broke architecture boundaries.
* **Infrequent Branch Synchronization:** Delayed `git pull --rebase` against `main` created avoidable merge conflicts in core files (`Student.java`, `pom.xml`, and configuration profiles).

### 🌸 Invent (How could we do things differently?)
* **Automated Scrum Master Telegram Bot:** Implement a bot powered by GitHub Webhooks/Actions to automatically track the GitHub Project board, monitor upcoming milestones, and tag assignees who have idle tasks or pending reviews.
* **Standardized IDE-Integrated AI Tooling:** Prohibit context-isolated web chats; standardize developer workflows on specialized IDE AI tools equipped with full repository context (Google Antigravity, Cursor, GitHub Copilot).
* **Mid-Sprint Review Checkpoints:** Establish a quick 15-minute sync midway through the sprint to demonstrate draft deliverables early and unblock dependencies before the final sprint review.

### 💪 Act (What should we do next?)
1. [ ] **Scrum Master / PM:** Design and deploy the Telegram notification bot integrated with GitHub Webhooks to automate reminders and eliminate manual tagging.
2. [ ] **Lead Developer:** Conduct a team onboarding session on effective IDE AI usage (Antigravity/Cursor) aligned with repository architecture guidelines.
3. [ ] **DevOps / Team:** Configure a pre-push hook to run local automated test verification (`./mvnw test`) before pushing commits to remote branches.
4. [ ] **Backend Team:** Design the recommendation engine pipeline for Area Electives and career specializations (US-09, US-10).

---

## 🏃‍♂️ Sprint 3: MVP Stabilization, Testing & Demo Readiness

* **Timeline:** October–November 2026
* **Sprint Focus:** Spring Security RBAC & CSRF hardening (`T-06`), static CSS/JS extraction & offline Tailwind utility layer (`T-07`, `T-08`), Career Aspirations onboarding & Self-Service Password Reset (`US-03`, `US-05`), Proactive Registration Alerts & Recommended Next Action hero card (`US-09`, `US-10`), Manual Transcript & GPA Auto-Recalculation (`US-29`, `US-30`, `US-31`), FAQ Knowledge Base (`US-34`, `US-35`), Floating Context-Aware AI Copilot & AI Semester Plan Builder (`US-40`, `US-41`, `US-42`, `US-43`), and SDU SITAM Information Systems handbook curriculum seeding.
* **Completed Stories & Tasks:** `T-06`, `T-07`, `T-08`, `US-03`, `US-05`, `US-09`, `US-10`, `US-29`, `US-30`, `US-31`, `US-34`, `US-35`, `US-40`, `US-41`, `US-42`, `US-43`, SITAM IS curriculum seeding, and the expanded automated test suite (`155` unit and integration tests passing).

### ⏩ Continue (What helped us move forward?)
* **Stacked Feature Branches & Atomic Pull Requests:** Breaking down technical foundation tasks (`T-06`–`T-08`) and student-facing stories (`US-03`–`US-10`) into focused, sequential branches with dedicated docs and tests kept reviews fast and conflict-free.
* **Context-Aware Floating AI Copilot & Prompt Grounding:** Injecting real-time student academic data (GPA, completed courses, career goals, active cart) into the floating Copilot widget (`US-40`–`US-43`) made AI advice immediately actionable across all pages.
* **Architecture & UI Regression Guards (`155` Tests Passing):** Adding structural tests (`StaticAssetsArchitectureTest`, `TailwindUiConsistencyTest`, `SecurityConfigTest`) alongside controller and service tests prevented template regressions and CSRF breakage during the frontend refactor.
* **Authentic SDU SITAM Curriculum Dataset:** Replacing synthetic demo courses with the real SDU Information Systems BSc handbook curriculum made prerequisite chains and career-track matching realistic during live demos.

### ⛔ Stop (What held us back?)
* **Embedding Inline `<style>` and `<script>` inside Thymeleaf Templates:** Scattering CSS rules and JavaScript logic directly inside HTML templates caused duplication and made CSRF header injection painful until `T-07` extracted them into `static/css/*` and `static/js/*`.
* **Postponing Spring Security & CSRF Integration:** Adding `spring-boot-starter-security` (`T-06`) after multiple `POST` forms and `fetch()` endpoints were already built required retrofitting CSRF tokens and `SessionSecurityAuthenticationFilter` across existing flows.
* **Stacked Branch Rebase Overhead:** Chaining seven dependent PRs (`#44`–`#50`) meant that any small fix on an earlier branch required rebasing downstream branches carefully to avoid drift.
* **Last-Minute Documentation Batching:** Waiting until the end of a feature burst to sync ADRs and schema docs (`V4`–`V7`) increased context-switching overhead for developers.

### 🌸 Invent (How could we do things differently?)
* **Offline-First Tailwind Utility Layer + Custom Design Tokens:** Combining a local `tailwind-utilities.css` stylesheet with domain-specific stylesheets (`ADR-0002`) gave us utility-class productivity without external CDN or Node.js build fragile dependencies.
* **DOM `data-*` State Hydration Pattern:** Passing server-side Thymeleaf state via `#planner-state` `data-*` attributes and a shared `window.advisorCsrfHeaders()` helper cleanly decoupled external JS modules from server template rendering.
* **Automated Merge-Queue / Stacked PR Workflow:** Introduce a strict merge order checklist (or GitHub merge queue) so stacked PRs merge into `main` continuously rather than accumulating in a 7-PR chain.
* **Interactive Seed Personas for QA:** Add pre-configured student personas (e.g., Freshman onboarding, Probation GPA warning, Senior capstone planning) in `DataSeeder` to test dashboard alerts (`US-09`, `US-10`) in one click.

### 💪 Act (What should we do next?)
1. [ ] **Tech Lead / Team:** Merge stacked Pull Requests (`#44`–`#50`) sequentially into `main` and verify the production Docker image build.
2. [ ] **Frontend Team:** Begin prototyping the interactive 8-semester timeline grid and drag-and-drop course allocation for Sprint 4 (`v1.0 Course Planner`).
3. [ ] **Backend Team:** Expand `RegistrationAlertService` and `RecommendedActionService` with multi-semester graduation milestone tracking and advisor hold indicators.
4. [ ] **AI Engineer:** Benchmark Gemini API fallback latency under rate-limiting scenarios and prepare mock response fixtures for offline classroom demos.

---

## 🏃‍♂️ Sprint 4: Visual Course Planner & 4-Year Roadmap (v1.0 Part 1)

* **Timeline:** November 2026
* **Sprint Focus:** Slide-out Course Catalog Drawer (`US-15`), Multi-facet Course Filters (`US-16`), Interactive Course Detail Modal (`US-17`), 8-Semester Degree Roadmap (`US-18`), Drag-and-Drop Reordering & Instant Prerequisite/Overload Validation (`US-19`), Pacing Strategy Rearrangement (`US-20`), and Season Availability Flagging (`US-21`).
* **Completed Stories:** `US-15`, `US-16`, `US-17`, `US-18`, `US-19`, `US-20`, `US-21`.

### ⏩ Continue
* **Sequential Atomic PRs:** Merging each user story cleanly into `main` before branching the next kept `PlannerController`, `RoadmapService`, and UI assets free of merge conflicts.
* **Pure-Server Validation + Instant UI Sync:** Computing prerequisite conflicts, overload thresholds (`> 30 ECTS`), and Fall/Spring mismatches server-side inside `RoadmapService` guaranteed consistent behavior across page renders and AJAX updates.

### ⛔ Stop
* **Blindly Seeding Unchecked Electives:** Initializing electives without verifying upstream prerequisites caused spurious roadmap conflicts until `arePrereqsPlacedBefore` and topological downstream depth bounds were enforced.

### 🌸 Invent
* **Combined Drag-and-Drop + Accessible Dropdown Controls:** Pairing HTML5 drag-and-drop with semester `<select>` controls on roadmap cards preserved both visual interactivity and keyboard/testability ergonomics.

### 💪 Act
1. [x] **Backend & Frontend:** Deliver saved plan drafts (`US-22`), PDF export (`US-23`), and the standalone GPA Simulator (`US-25`, `US-26`) in Sprint 5.

---

## 🏃‍♂️ Sprint 5: Plan Drafts, PDF Export & GPA Simulator (v1.0 Part 2)

* **Timeline:** November 2026
* **Sprint Focus:** Multi-scenario Plan Drafts in Planner & Profile (`US-22`), Pure-Java PDF 1.4 Export (`US-23`), GPA Planner & Cumulative GPA Simulator (`US-25`), and Scholarship / Academic Probation Risk Warnings (`US-26`).
* **Completed Stories:** `US-22`, `US-23`, `US-25`, `US-26`.

### ⏩ Continue
* **Zero-External-Dependency PDF Generation & Static Asset Discipline:** Writing a pure-Java PDF 1.4 writer (`PlanPdfExportService`) and strictly separating CSS/JS assets (`StaticAssetsArchitectureTest`) kept Docker builds and CI fast and deterministic.

### ⛔ Stop
* **Bulk-Deleting Managed Entities Before Re-Insertion:** Calling `deleteAllInBatch` inside an active Hibernate session risked L1 cache key collisions on SQLite until switched to managed `deleteAll` + `flush`.

### 🌸 Invent
* **Cart/Enrolled Fallback in GPA Simulation:** Automatically sourcing upcoming courses from the Semester Cart (or Currently Enrolled courses when the cart is empty) makes `/gpa` immediately useful without manual course re-entry.

### 💪 Act
1. [ ] **Team:** Proceed to v2.0 Document Intelligence (PDF transcript/syllabus parsing) and Advisor Approval Portal in Sprints 6–7.

---

## 🏃‍♂️ Sprint 6: Document Intelligence (PDF Syllabi & Transcripts) (v2.0 Part 1)

* **Timeline:** December 2026
* **Sprint Focus:** Multimodal PDF processing via Gemini File API: official transcript ingestion, external university syllabus equivalence checking for academic mobility programs.

### ⏩ Continue
* *...to be filled by the team during the Sprint 6 Retrospective...*

### ⛔ Stop
* *...to be filled by the team during the Sprint 6 Retrospective...*

### 🌸 Invent
* *...to be filled by the team during the Sprint 6 Retrospective...*

### 💪 Act
* *...to be filled by the team during the Sprint 6 Retrospective...*

---

## 🏃‍♂️ Sprint 7: Advisor Portal, Telegram Notifications & Final Defense (v2.0 Part 2)

* **Timeline:** December 2026
* **Sprint Focus:** Faculty Advisor review & approval portal, automated Telegram registration deadline alerts, final system load testing, and PMIS capstone presentation delivery.

### ⏩ Continue
* *...to be filled by the team during the Sprint 7 Retrospective...*

### ⛔ Stop
* *...to be filled by the team during the Sprint 7 Retrospective...*

### 🌸 Invent
* *...to be filled by the team during the Sprint 7 Retrospective...*

### 💪 Act
* *...to be filled by the team during the Sprint 7 Retrospective...*
