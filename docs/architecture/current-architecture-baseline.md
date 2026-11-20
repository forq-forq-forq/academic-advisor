# Current Architecture Baseline (Single Source of Truth)

## Purpose

This document is the source of truth for the **actual implemented architecture** of the repository.  
If any other document conflicts with this file, this file takes precedence.

---

## Current Status Summary

| Area | Current Implementation | Notes |
|---|---|---|
| Runtime language/platform | Java 17 + Spring Boot 3.4.7 | Implemented |
| Web layer | Spring MVC + Thymeleaf | Implemented |
| Static Assets (T-07) | Extracted CSS (`static/css/*`) and deferred JS modules (`static/js/*`, CSRF meta headers) with zero inline `<style>`/`<script>` blocks | Implemented (`docs/T-07.md`) |
| Tailwind CSS Utilities (T-08) | Local offline-safe `tailwind-utilities.css` & `tailwind-config.js` token layer loaded across all Thymeleaf views | Implemented (`docs/T-08.md`, ADR-0002) |
| Persistence | Spring Data JPA + Hibernate | Implemented |
| Default database | SQLite (`application.properties`) | Implemented |
| Docker database profile | PostgreSQL 16 (`application-docker.properties`) | Implemented |
| Schema management | Hibernate `ddl-auto` | Flyway not implemented yet |
| Domain model | Faculty, Major, CurriculumCourse, Course, Prerequisite, Student, StudentAccount, CartItem | Implemented & Seeded |
| Semester Cart (US-11) | `CartItem`, `CartItemRepository`, `CartService`, `PlannerController` (`/planner`) | Implemented |
| Prerequisite Validation (US-12) | `PrerequisiteRepository`, `PrerequisiteNotMetException`, `CartService.validatePrerequisites()`, `AvailableCourseDto` with UI alert & catalog status | Implemented |
| Copy Course Codes (US-13) | `CartDto.formattedCodes`, clipboard API with fallback in `planner.html`, dynamic copy button | Implemented |
| Credit Limit Warning (US-14) | `CartDto.exceedsLimit`, `CartDto.excessCredits`, `CartDto.limitWarning`, `#credit-limit-warning` banner, red counter styling | Implemented |
| Slide-Out Course Catalog Drawer (US-15) | `#catalog-drawer`, `#btn-toggle-catalog-drawer`, `GET /planner/catalog`, live code/name search | Implemented (`docs/US-15.md`) |
| Catalog Multi-Facet Filters (US-16) | `Course.season` (`V8`), `AvailableCourseDto.category`/`season`, `type`/`season`/`difficulty` filters in UI and `GET /planner/catalog` | Implemented (`docs/US-16.md`) |
| Course Detail Card (US-17) | `CourseDetailDto`, `GET /planner/courses/{courseId}`, `#course-detail-modal` with description, difficulty bar, prerequisite chain, and unlocked courses | Implemented (`docs/US-17.md`) |
| 8-Semester Roadmap View (US-18) | `RoadmapItem` (`V9`), `RoadmapService`, `GET /planner/roadmap`, `#roadmap-board-section` with `#roadmap-semester-1..8` | Implemented (`docs/US-18.md`) |
| Roadmap Drag & Drop + Validation (US-19) | `POST /planner/roadmap/move`, `POST /planner/roadmap/remove`, HTML5 drag-and-drop, `.card-prereq-violation` (red), `.semester-overloaded` (yellow) | Implemented (`docs/US-19.md`) |
| Pacing Strategy Rearrangement (US-20) | `RoadmapService.applyPacingStrategy`, `POST /planner/roadmap/pacing`, `#pacing-strategy-select` (`BALANCED` / `FAST_TRACK`) | Implemented (`docs/US-20.md`) |
| Season Availability Flagging (US-21) | `seasonMismatch`, `seasonWarning`, `seasonConflictCount`, `#roadmap-season-conflicts`, `.card-season-mismatch` & `.badge-season-conflict` | Implemented (`docs/US-21.md`) |
| Academic Standing & Progress (US-07) | `AcademicStandingDto`, `DashboardController` (`/dashboard`), SVG circular progress ring (`27 / 240 ECTS`), GPA badge & program summary | Implemented |
| Enrolled Courses List (US-08) | `EnrolledCourseDto`, `Student.enrolledCourses`, `DashboardController` (`/dashboard`), semester course list with ECTS credit weights | Implemented |
| Completed Course History (US-29) | `CourseCompletion` entity over `student_courses`, `CompletedCourseDto`, dashboard & profile tables | Implemented |
| Manual Transcript Management (US-30) | `TranscriptService`, `ProfileController` (`/profile`), add/edit/remove completed courses, letter grades, semesters, and academic profile | Implemented |
| Automatic Cumulative GPA (US-31) | `CumulativeGpaService` derives credit-weighted GPA from current completion records on `/dashboard` and `/profile` using SDU Handbook 2026–2027 scale | Implemented (`docs/US-31.md`) |
| FAQ & AI Handoff (US-34, US-35, US-36) | `FaqController` (`/faq`), categorized accordion, real-time filter, and `"Ask AI about my situation"` handoff to Copilot | Implemented |
| Floating AI Copilot Widget (US-40, US-42) | `fragments/copilot.html` included across `/dashboard`, `/planner`, `/profile`, `/faq` with `sessionStorage` state & quick prompt chips | Implemented |
| Automatic Academic Context (US-41) | `AcademicContextService` injects major, GPA, degree progress, completed/enrolled courses, cart, and eligible courses into AI prompts | Implemented |
| AI Semester Plan Builder (US-43) | `SemesterPlanService`, `CourseRecommendationDto` with `"Why this course?"` reasons, workload balancing (≤ 30 ECTS), and one-click `+ Add to Cart` | Implemented |
| Onboarding & Career Aspirations (US-03) | Quick-select aspiration chips on `/register`, dual persistence on `StudentAccount` & `Student`, onboarding goal banner, expanded career matching in `SemesterPlanService` | Implemented (`docs/US-03.md`) |
| Self-Service Password Reset (US-05) | `PasswordResetToken`, `PasswordResetService`, `PasswordResetController` (`/forgot-password`, `/reset-password`), anti-enumeration, 30m single-use UUID tokens, dual hash updates | Implemented (`docs/US-05.md`) |
| Registration Alert Banner (US-09) | `RegistrationPeriod`, `RegistrationAlertService`, seeded Fall 2026 registration period, `#registration-alert-card` urgency banner on `/dashboard` | Implemented (`docs/US-09.md`) |
| Recommended Next Action (US-10) | `RecommendedActionDto`, `RecommendedActionService`, `#recommended-next-action-card` adaptive dashboard action card linking directly to `/planner` or `/profile` | Implemented (`docs/US-10.md`) |
| Authentication & RBAC (T-06) | `spring-boot-starter-security` (`SecurityConfig`), `CustomUserDetailsService`, `SessionSecurityAuthenticationFilter`, `Role` (`STUDENT`, `ADVISOR`, `ADMIN`), `/advisor/**` role guard, CSRF protection | Implemented (`docs/T-06.md`) |
| AI integration | Google Gemini API via `RestClient` | Implemented |
| CI | GitHub Actions (`./mvnw clean test`, Docker build checks) | Implemented |

---

## Profile Policy (Current)

| Profile | Purpose | Data source | Schema mode |
|---|---|---|---|
| `default` | Local development without Docker | SQLite (`advisor.db`) | `spring.jpa.hibernate.ddl-auto=update` |
| `docker` | Containerized app in Docker Compose | PostgreSQL 16 container | `spring.jpa.hibernate.ddl-auto=update` |
| `test` | Automated tests | SQLite (`target/test-advisor.db`) | `spring.jpa.hibernate.ddl-auto=create-drop` |

---

## Security Baseline (Spring Security + RBAC, T-06)

1. Login form collects Student ID or SDU email and password; credentials are checked against BCrypt hashes via `PasswordEncoder` and `CustomUserDetailsService`.
2. Successful authentication stores `authenticatedStudent`, `authenticatedStudentId`, and `SPRING_SECURITY_CONTEXT` in `HttpSession`, and `SessionSecurityAuthenticationFilter` restores the principal on each request.
3. Invalid credentials display `"Invalid Student ID or password"` while preserving the entered identifier.
4. `SecurityConfig` enforces authenticated access on protected routes (`/dashboard/**`, `/planner/**`, `/profile/**`, `/faq/**`, `/api/**`), redirecting HTML requests to `/login` and returning `401` JSON errors for unauthenticated API calls.
5. Role-Based Access Control (RBAC) assigns `Role.STUDENT` by default and protects `/advisor/**` (`AdvisorController`) with `hasAnyRole('ADVISOR', 'ADMIN')` (`403 Forbidden` for `STUDENT`).
6. CSRF protection is enabled (`advisor.security.csrf.strict=true` in production; controllable via `advisor.security.csrf.strict=false` and `X-Enforce-Csrf: true` in automated test profile) along with standard security headers (`X-Frame-Options: SAMEORIGIN`, `X-Content-Type-Options: nosniff`).

---

## Next Technical Priorities (Agreed Direction)

1. Introduce production-grade authentication/authorization baseline.
2. Introduce Flyway migrations and migrate away from schema-by-`ddl-auto`.
3. Standardize database strategy (clear long-term decision for SQLite vs PostgreSQL by environment).
4. Keep all docs synchronized with implemented behavior after each architectural change.
