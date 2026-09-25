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
* **Sprint Focus:** Backend prerequisite validation engine, balanced 30 ECTS semester plan generation, PostgreSQL + Flyway migration, and one-click course cart export.

### ⏩ Continue
* *...to be filled by the team during the Sprint 2 Retrospective...*

### ⛔ Stop
* *...to be filled by the team during the Sprint 2 Retrospective...*

### 🌸 Invent
* *...to be filled by the team during the Sprint 2 Retrospective...*

### 💪 Act
* *...to be filled by the team during the Sprint 2 Retrospective...*

---

## 🏃‍♂️ Sprint 3: MVP Stabilization, Testing & Demo Readiness

* **Timeline:** October 2026
* **Sprint Focus:** End-to-end integration testing of student user journey (Onboarding → Catalog → AI Recommendation → Cart Export), Gemini API resilience testing, and MVP UI polish.

### ⏩ Continue
* *...to be filled by the team during the Sprint 3 Retrospective...*

### ⛔ Stop
* *...to be filled by the team during the Sprint 3 Retrospective...*

### 🌸 Invent
* *...to be filled by the team during the Sprint 3 Retrospective...*

### 💪 Act
* *...to be filled by the team during the Sprint 3 Retrospective...*

---

## 🏃‍♂️ Sprint 4: Visual Course Planner & 4-Year Roadmap (v1.0 Part 1)

* **Timeline:** November 2026
* **Sprint Focus:** Interactive 8-semester timeline grid, collapsible Course Explorer drawer, drag-and-drop course allocation, and real-time prerequisite conflict highlighting.

### ⏩ Continue
* *...to be filled by the team during the Sprint 4 Retrospective...*

### ⛔ Stop
* *...to be filled by the team during the Sprint 4 Retrospective...*

### 🌸 Invent
* *...to be filled by the team during the Sprint 4 Retrospective...*

### 💪 Act
* *...to be filled by the team during the Sprint 4 Retrospective...*

---

## 🏃‍♂️ Sprint 5: Career Elective Matching, GPA Simulator & Polish (v1.0 Part 2)

* **Timeline:** November 2026
* **Sprint Focus:** Area Electives recommendation engine tailored to career specializations (Frontend/Backend/Data Science), standalone GPA Simulator & risk calculator, and PDF roadmap export.

### ⏩ Continue
* *...to be filled by the team during the Sprint 5 Retrospective...*

### ⛔ Stop
* *...to be filled by the team during the Sprint 5 Retrospective...*

### 🌸 Invent
* *...to be filled by the team during the Sprint 5 Retrospective...*

### 💪 Act
* *...to be filled by the team during the Sprint 5 Retrospective...*

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
