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
| Authentication | Session attributes (`authenticatedStudent`, `authenticatedStudentId`) with BCrypt password verification via `PasswordEncoder` | US-04 implemented |
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

## Security Baseline for MVP (Current)

1. Login form collects Student ID and password.
2. Student ID is verified in the `students` table and password is verified against the stored BCrypt hash via `PasswordEncoder` (never plaintext).
3. Successful authentication creates an active session (`authenticatedStudent` and `authenticatedStudentId` in `HttpSession`) and redirects to `/planner`.
4. Invalid credentials display `"Invalid Student ID or password"` while preserving the entered Student ID.
5. Client-side and server-side validation reject empty submissions and highlight required fields.
6. Protected pages (`/dashboard`, `/dashboard/chat`, `/planner`) enforce session presence and redirect unauthenticated users back to `/login`.

---

## Next Technical Priorities (Agreed Direction)

1. Introduce production-grade authentication/authorization baseline.
2. Introduce Flyway migrations and migrate away from schema-by-`ddl-auto`.
3. Standardize database strategy (clear long-term decision for SQLite vs PostgreSQL by environment).
4. Keep all docs synchronized with implemented behavior after each architectural change.
