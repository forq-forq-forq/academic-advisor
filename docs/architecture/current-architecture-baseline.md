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
| Persistence | Spring Data JPA + Hibernate | Implemented |
| Default database | SQLite (`application.properties`) | Implemented |
| Docker database profile | PostgreSQL 16 (`application-docker.properties`) | Implemented |
| Schema management | Hibernate `ddl-auto` | Flyway not implemented yet |
| Domain model | Faculty, Major, CurriculumCourse, Course, Prerequisite, Student, StudentAccount, CartItem | Implemented & Seeded |
| Semester Cart (US-11) | `CartItem`, `CartItemRepository`, `CartService`, `PlannerController` (`/planner`) | Implemented |
| Prerequisite Validation (US-12) | `PrerequisiteRepository`, `PrerequisiteNotMetException`, `CartService.validatePrerequisites()`, `AvailableCourseDto` with UI alert & catalog status | Implemented |
| Copy Course Codes (US-13) | `CartDto.formattedCodes`, clipboard API with fallback in `planner.html`, dynamic copy button | Implemented |
| Credit Limit Warning (US-14) | `CartDto.exceedsLimit`, `CartDto.excessCredits`, `CartDto.limitWarning`, `#credit-limit-warning` banner, red counter styling | Implemented |
| Academic Standing & Progress (US-07) | `AcademicStandingDto`, `DashboardController` (`/dashboard`), SVG circular progress ring (`27 / 240 ECTS`), GPA badge & program summary | Implemented |
| Enrolled Courses List (US-08) | `EnrolledCourseDto`, `Student.enrolledCourses`, `DashboardController` (`/dashboard`), semester course list with ECTS credit weights | Implemented |
| Completed Course History (US-29) | `CourseCompletion` entity over `student_courses`, `CompletedCourseDto`, dashboard & profile tables | Implemented |
| Manual Transcript Management (US-30) | `TranscriptService`, `ProfileController` (`/profile`), add/edit/remove completed courses, letter grades, semesters, and academic profile | Implemented |
| Automatic Cumulative GPA (US-31) | `CumulativeGpaService` derives credit-weighted GPA from current completion records on `/dashboard` and `/profile` using SDU Handbook 2026–2027 scale | Implemented (`docs/US-31.md`) |
| FAQ & AI Handoff (US-34, US-35, US-36) | `FaqController` (`/faq`), categorized accordion, real-time filter, and `"Ask AI about my situation"` handoff to Copilot | Implemented |
| Floating AI Copilot Widget (US-40, US-42) | `fragments/copilot.html` included across `/dashboard`, `/planner`, `/profile`, `/faq` with `sessionStorage` state & quick prompt chips | Implemented |
| Automatic Academic Context (US-41) | `AcademicContextService` injects major, GPA, degree progress, completed/enrolled courses, cart, and eligible courses into AI prompts | Implemented |
| AI Semester Plan Builder (US-43) | `SemesterPlanService`, `CourseRecommendationDto` with `"Why this course?"` reasons, workload balancing (≤ 30 ECTS), and one-click `+ Add to Cart` | Implemented |
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
