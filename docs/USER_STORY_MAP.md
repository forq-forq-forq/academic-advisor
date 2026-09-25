# User Story Map — Academic Advisor

## Milestones

### 🚀 MVP (v0.1) — *Your First Smart Semester*

The student opens the app, logs in, and within 2 minutes has a **balanced, AI-generated course selection** for their next semester — no more sleepless nights wondering if they accidentally signed up for 6 impossible courses at once.

The MVP solves **one specific, painful problem**: the moment before registration when a student stares at a list of 30+ available courses and has to figure out which 5–6 to take without burning out, violating prerequisites, or exceeding credit limits. Today this takes hours of cross-referencing PDFs and asking upperclassmen. With the MVP, it takes one click.

**What the student gets:** Login → see completed courses & GPA → ask AI "build my next semester" → get a workload-balanced plan with explanations → copy course codes for registration.

**What the student does NOT get yet:** No 4-year roadmap, no visual drag-and-drop, no GPA simulation, no document uploads. Those come later.

---

### 🌟 v1.0 — *Own Your Entire Degree*

The student graduates from thinking semester-by-semester to **seeing and controlling all 4 years** of their degree in one interactive view.

v1.0 answers the strategic questions: *"Can I take 4th-year courses early to free up time for an internship?"*, *"Which area electives actually match my career goals?"*, *"What happens to my GPA if I get a B in Algorithms?"*.

**What changes for the student:**
- A visual **Course Planner** with a semester grid, drag-and-drop, real-time prerequisite validation, and workload indicators.
- A **GPA Planner** that lets them simulate "what-if" scenarios before committing.
- **Career-driven elective matching** — describe your dream job once, and AI tailors every elective recommendation to that goal.
- **Export & share** — PDF plans, shareable links, copy-paste course codes.

The app stops being a "chatbot" and becomes a **full planning workspace**.

---

### 🔮 v2.0 — *AI That Reads Your Documents*

The system gains the ability to **understand real university documents**: 200-page curriculum handbooks, PDF syllabi, official transcripts, and even job descriptions from LinkedIn.

v2.0 closes the loop between student, AI, and institution. A student uploads their transcript PDF → the system auto-populates their history. An admin uploads a curriculum handbook → the system auto-generates the 4-year course grid for that major. A student pastes a job listing → AI maps the required skills to specific electives.

**What changes for the student:**
- **Zero manual data entry** — transcript parsing and curriculum auto-import.
- **Syllabus intelligence** — AI reads any course syllabus and tells you the real workload, tech stack, and career relevance.
- **Advisor approval workflow** — send your plan to your faculty advisor for sign-off, right inside the app.
- **Notifications** — Telegram alerts for registration deadlines and plan approvals.

---

## Impact / Effort Legend

| Label | Meaning | Emoji |
|---|---|---|
| **Quick Win** | High impact, low effort. Do first. | ⚡ |
| **Big Bet** | High impact, high effort. Core features worth the investment. | 🎯 |
| **Fill-In** | Low impact, low effort. Nice-to-have, slot into spare capacity. | 📌 |
| **Deprioritize** | Low impact, high effort. Do last or cut. | ⏳ |

---

## Activity 0: Technical Infrastructure

> *Cross-cutting engineering work that enables all business features. Not visible to users, but critical for team velocity, code quality, and deployment reliability.*

### Task 0.1: Containerization & DevOps

| ID | User Story | Release | Impact/Effort |
|---|---|---|---|
| **T-01** | As a developer, I want a Docker Compose setup (PostgreSQL + app) so that any team member can run the full stack with one command. | MVP | ⚡ Quick Win |
| **T-02** | As a developer, I want a GitHub Actions CI pipeline that runs tests and validates Docker builds on every PR. | MVP | ⚡ Quick Win |
| **T-03** | As a developer, I want a `.dockerignore` and multi-stage Dockerfile so that builds are fast and images are small. | MVP | 📌 Fill-In |

### Task 0.2: Database & Schema Management

| ID | User Story | Release | Impact/Effort |
|---|---|---|---|
| **T-04** | As a developer, I want to migrate from SQLite to PostgreSQL so that the app supports concurrent users and enforces referential integrity. | MVP | 🎯 Big Bet |
| **T-05** | As a developer, I want Flyway migration scripts so that schema changes are versioned, repeatable, and auditable. | MVP | ⚡ Quick Win |

### Task 0.3: Security & Auth Framework

| ID | User Story | Release | Impact/Effort |
|---|---|---|---|
| **T-06** | As a developer, I want Spring Security with session-based auth, CSRF protection, and role definitions (STUDENT, ADVISOR) so that the app is secure by default. | v1.0 | 🎯 Big Bet |

### Task 0.4: Frontend Architecture

| ID | User Story | Release | Impact/Effort |
|---|---|---|---|
| **T-07** | As a developer, I want to extract all inline CSS/JS from Thymeleaf templates into static asset files (`/css/`, `/js/`) so that code is maintainable and cacheable. | v1.0 | ⚡ Quick Win |
| **T-08** | As a developer, I want to adopt Tailwind CSS (or a similar utility framework) so that UI development is fast and consistent. | v1.0 | 🎯 Big Bet |

### Task 0.5: Testing & Quality

| ID | User Story | Release | Impact/Effort |
|---|---|---|---|
| **T-09** | As a developer, I want integration tests for all controllers (MockMvc) and services (mocked Gemini) so that regressions are caught automatically. | MVP | ⚡ Quick Win |
| **T-10** | As a developer, I want a separate `application-test.properties` using H2 in-memory DB so that tests run without Docker. | MVP | ⚡ Quick Win |

---

## Activity 1: Auth & Onboarding (`/login`, `/register`)

> *The student's first contact with the system. Registration is where the "magic import" happens — the student picks their major, and the system pre-loads their entire 4-year curriculum automatically. No manual course entry needed.*

### Task 1.1: Student Registration

| ID | User Story | Release | Impact/Effort |
|---|---|---|---|
| **US-01** | As a new student, I want to register with my university email (`@sdu.edu.kz`), Student ID, and password so that I have a secure personal account. | MVP | 🎯 Big Bet |
| **US-02** | As a new student, I want to select my faculty, major (e.g., Computer Science), and catalog year during registration so that the system auto-loads my full 4-year curriculum. | MVP | 🎯 Big Bet |
| **US-03** | As a new student, I want to briefly describe my career aspirations in a text field during onboarding (e.g., "I want to become a backend developer") so that AI personalizes elective recommendations from day one. | v1.0 | ⚡ Quick Win |

### Task 1.2: Authentication

| ID | User Story | Release | Impact/Effort |
|---|---|---|---|
| **US-04** | As a returning student, I want to log in with my Student ID and password so that I can access my saved plans and history. | MVP | ⚡ Quick Win |
| **US-05** | As a student who forgot my password, I want to reset it via a link sent to my university email. | v1.0 | 📌 Fill-In |

### Task 1.3: Session Management

| ID | User Story | Release | Impact/Effort |
|---|---|---|---|
| **US-06** | As a logged-in student, I want to log out and have my session fully invalidated so that my data is protected on shared computers. | MVP | 📌 Fill-In |

---

## Activity 2: Dashboard — Home (`/dashboard`)

> *The student's personal command center. At a glance: where am I in my degree, what do I need to do next, and how am I doing. NOT the main chat page — the AI lives as a floating widget everywhere (see Activity 8).*

### Task 2.1: Academic Status Overview

| ID | User Story | Release | Impact/Effort |
|---|---|---|---|
| **US-07** | As a student, I want to see my current GPA and a credit progress ring (e.g., "96 / 240 ECTS completed") on my dashboard so that I instantly understand my academic standing. | MVP | ⚡ Quick Win |
| **US-08** | As a student, I want to see a list of courses I'm currently enrolled in this semester with their credit weights. | MVP | 📌 Fill-In |

### Task 2.2: Smart Alerts & Next Actions

| ID | User Story | Release | Impact/Effort |
|---|---|---|---|
| **US-09** | As a student, I want to see an alert card when course registration is approaching (e.g., "Registration opens in 3 days — your cart has 0 courses") so that I don't miss deadlines. | v1.0 | ⚡ Quick Win |
| **US-10** | As a student, I want a "Recommended Next Action" card (e.g., "Build your Fall 2026 plan → Open Planner") that guides me to the right page. | v1.0 | 📌 Fill-In |

---

## Activity 3: Course Planner (`/planner`)

> *The core workspace of the entire application. A merged view: the left/center area is the interactive semester planner (grid or cart), and a slide-out drawer on the right is the course catalog (explorer). The student searches, filters, and drags courses from the catalog drawer directly into their semester slots.*

### Design: How the merged Planner + Explorer works

```
┌─────────────────────────────────────────────────────────────────────┐
│  Course Planner                                          [≡ Courses]│
│                                                                     │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐       ┌──────────┐│
│  │ Semester 3   │ │ Semester 4   │ │ Semester 5   │      │ 🔍 Search││
│  │ Fall 2025    │ │ Spring 2026  │ │ Fall 2026    │  ◀─  │──────────││
│  │              │ │              │ │              │ drag │ CS201  3cr││
│  │ ┌──────────┐│ │ ┌──────────┐│ │              │      │ CS205  3cr││
│  │ │ CS201  3cr││ │ │ MATH301 4││ │  Drop here   │      │ ENG202 2cr││
│  │ │ ██████ 4/5││ │ │ ██░░░ 2/5││ │              │      │ PHY101 3cr││
│  │ └──────────┘│ │ └──────────┘│ │              │      │──────────││
│  │ ┌──────────┐│ │              │ │              │      │ Filter:  ││
│  │ │ ENG201 2cr││ │              │ │              │      │ ☑ Core   ││
│  │ │ █░░░░ 1/5││ │              │ │              │      │ ☑ Elective││
│  │ └──────────┘│ │              │ │              │      │ ☐ General ││
│  │              │ │              │ │              │      │──────────││
│  │ 14/30 ECTS  │ │  4/30 ECTS   │ │  0/30 ECTS   │      │ Season:  ││
│  │ Load: ████░ │ │ Load: ██░░░  │ │ Load: ░░░░░  │      │ ○ Fall   ││
│  └─────────────┘ └─────────────┘ └─────────────┘       │ ○ Spring ││
│                                                          └──────────┘│
│  ┌─────────────────────────────────────────────────┐                 │
│  │ 📋 Registration Cart: CS201, MATH301, ENG201    │  [📋 Copy]     │
│  │    Total: 18/30 ECTS  |  Workload: ████░ OK     │                 │
│  └─────────────────────────────────────────────────┘                 │
└─────────────────────────────────────────────────────────────────────┘
```

### Task 3.1: Semester Cart Builder

| ID | User Story | Release | Impact/Effort |
|---|---|---|---|
| **US-11** | As a student, I want to see my upcoming semester as a cart with a running credit total (e.g., "18 / 30 ECTS") and a workload balance indicator so that I know if I'm overloading. | MVP | 🎯 Big Bet |
| **US-12** | As a student, I want the system to block me from adding a course whose prerequisites I haven't completed, with a clear explanation of what's missing. | MVP | 🎯 Big Bet |
| **US-13** | As a student, I want to copy all course codes in my cart to clipboard in one click (e.g., `CS201, MATH301, ENG201`) so that I can paste them into the university registration portal. | MVP | ⚡ Quick Win |
| **US-14** | As a student, I want a warning when my cart exceeds the university's maximum credit limit. | MVP | ⚡ Quick Win |

### Task 3.2: Course Catalog Drawer (Explorer)

| ID | User Story | Release | Impact/Effort |
|---|---|---|---|
| **US-15** | As a student, I want to open a slide-out course catalog panel and search courses by code or name so that I can find what I need without leaving the planner. | v1.0 | 🎯 Big Bet |
| **US-16** | As a student, I want to filter courses by type (Core / Area Elective / General), season (Fall / Spring), and difficulty so that I can narrow down options quickly. | v1.0 | ⚡ Quick Win |
| **US-17** | As a student, I want to see a course detail card showing description, credits, difficulty rating, prerequisite chain, and season availability. | v1.0 | 🎯 Big Bet |

### Task 3.3: Multi-Year Roadmap View

| ID | User Story | Release | Impact/Effort |
|---|---|---|---|
| **US-18** | As a student, I want to see all 8 semesters of my degree on one interactive board with courses placed as cards. | v1.0 | 🎯 Big Bet |
| **US-19** | As a student, I want to drag-and-drop courses between semesters and see instant validation (prerequisite violations turn red, overloaded semesters turn yellow). | v1.0 | 🎯 Big Bet |
| **US-20** | As a student, I want to choose a pacing strategy ("Balanced 4 years" vs "Fast-track: light 4th year for internship") and have AI rearrange my roadmap accordingly. | v1.0 | 🎯 Big Bet |
| **US-21** | As a student, I want the system to flag courses available only in Fall or Spring so that I don't accidentally plan a spring-only course for a fall semester. | v1.0 | ⚡ Quick Win |

### Task 3.4: Export & Share

| ID | User Story | Release | Impact/Effort |
|---|---|---|---|
| **US-22** | As a student, I want to save multiple plan drafts (e.g., "Safe plan" vs "Ambitious plan") in my profile. | v1.0 | ⚡ Quick Win |
| **US-23** | As a student, I want to export my semester plan or 4-year roadmap as a PDF with course codes, credits, and workload summary. | v1.0 | 📌 Fill-In |
| **US-24** | As a student, I want to share my plan via a direct link so that a friend or mentor can review it. | v2.0 | ⏳ Deprioritize |

---

## Activity 4: GPA Planner (`/gpa`)

> *A dedicated analytical tool for grade simulation and academic risk assessment. The student enters expected grades for upcoming courses and sees how their cumulative GPA changes — before committing to a course load.*

### Task 4.1: GPA Simulation

| ID | User Story | Release | Impact/Effort |
|---|---|---|---|
| **US-25** | As a student, I want to enter expected letter grades (A, B+, C, etc.) for each course in my cart and see a projected cumulative GPA so that I can assess risk before registration. | v1.0 | 🎯 Big Bet |
| **US-26** | As a student with low GPA, I want the system to warn me if my projected GPA will drop below the scholarship threshold or trigger academic probation. | v1.0 | ⚡ Quick Win |

### Task 4.2: Strategic Grade Planning

| ID | User Story | Release | Impact/Effort |
|---|---|---|---|
| **US-27** | As a student, I want AI to suggest which courses are "safe" (historically high average grades) and which are "risky" (high failure rates) so that I can plan strategically. | v2.0 | 🎯 Big Bet |
| **US-28** | As a student, I want to see the minimum grade I need in each remaining course to reach a target GPA (e.g., "You need at least B+ in all 5 courses to reach 3.0"). | v2.0 | ⚡ Quick Win |

---

## Activity 5: Profile & Academic Record (`/profile`)

> *The student's transcript and academic identity. This is where they manage what the system knows about them — completed courses, grades, and retakes. In the MVP, students mark courses manually. In v2.0, they upload a PDF transcript and the system auto-populates.*

### Task 5.1: Transcript Management

| ID | User Story | Release | Impact/Effort |
|---|---|---|---|
| **US-29** | As a student, I want to see a table of my completed courses with letter grades, credits, and semester taken. | MVP | ⚡ Quick Win |
| **US-30** | As a student, I want to manually mark courses as completed and enter grades so that the system accurately reflects my history. | MVP | 🎯 Big Bet |
| **US-31** | As a student, I want my cumulative GPA to auto-recalculate whenever I update my transcript. | MVP | ⚡ Quick Win |

### Task 5.2: Document Uploads

| ID | User Story | Release | Impact/Effort |
|---|---|---|---|
| **US-32** | As a student, I want to upload my official transcript PDF and have the system auto-extract courses, grades, and credits using Gemini AI. | v2.0 | 🎯 Big Bet |
| **US-33** | As a student, I want to upload a language certificate (IELTS/Duolingo) so the system marks language courses as exempt and frees up those credits. | v2.0 | 📌 Fill-In |

---

## Activity 6: FAQ & Academic Regulations (`/faq`)

> *A searchable knowledge base of university rules (retakes, probation, credit limits, summer school) with a bridge to the AI assistant. Each FAQ answer has a "Ask AI about my situation" button that opens the Copilot with a pre-filled context-aware prompt.*

### Task 6.1: Static Knowledge Base

| ID | User Story | Release | Impact/Effort |
|---|---|---|---|
| **US-34** | As a student, I want to browse FAQ articles organized by category (GPA & Grading, Registration, Retakes, Summer School, Scholarships) in an accordion layout. | v1.0 | ⚡ Quick Win |
| **US-35** | As a student, I want a search bar in FAQ that filters articles in real time as I type. | v1.0 | 📌 Fill-In |

### Task 6.2: AI-Enhanced FAQ

| ID | User Story | Release | Impact/Effort |
|---|---|---|---|
| **US-36** | As a student reading a FAQ answer, I want to click "Ask AI about my situation" which opens the Copilot chat pre-filled with context (e.g., "My GPA is 2.3. Based on the retake policy, can I...?"). | v1.0 | ⚡ Quick Win |

---

## Activity 7: Settings (`/settings`)

> *Personalization and account management. Kept minimal — only settings that genuinely improve the student's experience.*

### Task 7.1: Preferences

| ID | User Story | Release | Impact/Effort |
|---|---|---|---|
| **US-37** | As a student, I want to switch between light and dark theme so the app is comfortable at night. | v1.0 | 📌 Fill-In |
| **US-38** | As a student, I want to update my career interest / specialization tag at any time so that AI adjusts its recommendations. | v1.0 | 📌 Fill-In |

### Task 7.2: Notifications

| ID | User Story | Release | Impact/Effort |
|---|---|---|---|
| **US-39** | As a student, I want to connect my Telegram account and receive alerts when registration opens or my advisor approves/rejects my plan. | v2.0 | ⏳ Deprioritize |

---

## Activity 8: AI Copilot (Floating Widget — cross-cutting)

> *The AI assistant is NOT a separate page. It's a floating button (bottom-right corner, like GitHub Copilot) available on every page. When opened, a slide-out panel appears. The AI automatically knows which page the student is on and adjusts its context.*
>
> **How context injection works by page:**
> | Page the student is on | AI automatically knows... |
> |---|---|
> | Dashboard | Student profile, GPA, current semester |
> | Course Planner | Courses currently in the cart, workload balance, available slots |
> | GPA Planner | Projected grades, GPA simulation results |
> | Profile | Full transcript, completed courses |
> | FAQ | The specific FAQ topic being read |
> | Course detail card | That specific course's syllabus, prereqs, difficulty |

### Task 8.1: Core AI Chat

| ID | User Story | Release | Impact/Effort |
|---|---|---|---|
| **US-40** | As a student, I want to click a floating AI button on any page and have a chat panel slide out so that I can ask questions without navigating away. | MVP | 🎯 Big Bet |
| **US-41** | As a student, I want the AI to automatically include my academic context (GPA, completed courses, current page) in its responses without me having to explain my situation every time. | MVP | 🎯 Big Bet |
| **US-42** | As a student, I want quick-prompt chips above the chat input ("Build my next semester", "What electives fit my career?", "Explain this course") so I don't have to type from scratch. | MVP | ⚡ Quick Win |

### Task 8.2: AI-Powered Actions

| ID | User Story | Release | Impact/Effort |
|---|---|---|---|
| **US-43** | As a student, I want AI to generate a balanced semester plan and display each recommended course with a "Why this course?" explanation and an "Add to Cart" button. | MVP | 🎯 Big Bet |
| **US-44** | As a student on the Planner page, I want to ask AI "Is my current cart balanced?" and get feedback on workload distribution with specific swap suggestions. | v1.0 | ⚡ Quick Win |

### Task 8.3: Document Intelligence

| ID | User Story | Release | Impact/Effort |
|---|---|---|---|
| **US-45** | As a student, I want to upload a PDF syllabus of an unfamiliar elective into the chat and get AI analysis: real workload estimate, tech stack covered, and career relevance score. | v2.0 | 🎯 Big Bet |
| **US-46** | As a student, I want to paste or describe a job listing in chat and have AI map the required skills to specific electives I should take. | v2.0 | 🎯 Big Bet |

---

## Activity 9: Advisor Approval Portal (`/advisor`) — v2.0 only

> *A separate interface for faculty advisors (куратор). They see a list of their assigned students' draft plans and can approve, reject with comments, or suggest modifications. This closes the institutional loop: student → AI → advisor → approved plan.*

### Task 9.1: Advisor Workflow

| ID | User Story | Release | Impact/Effort |
|---|---|---|---|
| **US-47** | As a faculty advisor, I want to log in under an ADVISOR role and see a list of students assigned to me with their submitted plan drafts. | v2.0 | 🎯 Big Bet |
| **US-48** | As a faculty advisor, I want to approve or reject a student's semester plan with an optional comment so that the student gets actionable feedback. | v2.0 | 🎯 Big Bet |

---

## Summary Matrix

### Stories per Release

| Release | Business Stories | Technical Stories | Total |
|---|---|---|---|
| **MVP** | 17 | 5 | **22** |
| **v1.0** | 19 | 3 | **22** |
| **v2.0** | 12 | 0 | **12** |
| **Total** | **48** | **8** | **56** |

### Stories by Impact/Effort

| Category | Count | Strategy |
|---|---|---|
| ⚡ Quick Win | 22 | Do first within each sprint |
| 🎯 Big Bet | 24 | Core features, allocate full sprints |
| 📌 Fill-In | 7 | Slot into remaining sprint capacity |
| ⏳ Deprioritize | 3 | Cut if timeline is tight |

### Stories by Activity

| # | Activity | MVP | v1.0 | v2.0 | Total |
|---|---|---|---|---|---|
| 0 | Technical Infrastructure | 5 | 3 | — | 8 |
| 1 | Auth & Onboarding | 3 | 2 | — | 5 |
| 2 | Dashboard | 2 | 2 | — | 4 |
| 3 | Course Planner | 4 | 10 | 1 | 15 |
| 4 | GPA Planner | — | 2 | 2 | 4 |
| 5 | Profile & Academic Record | 3 | — | 2 | 5 |
| 6 | FAQ & Academic Regulations | — | 3 | — | 3 |
| 7 | Settings | — | 2 | 1 | 3 |
| 8 | AI Copilot | 4 | 1 | 2 | 7 |
| 9 | Advisor Portal | — | — | 2 | 2 |
