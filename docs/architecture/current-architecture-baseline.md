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
| Authentication | Session attribute (`authenticatedStudentId`) via `HttpSession` | Spring Security not implemented yet |
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

1. Login is based on Student ID lookup in the `students` table.
2. Successful login stores `authenticatedStudentId` in `HttpSession`.
3. `/dashboard` and `/dashboard/chat` enforce session presence at controller level.
4. This is acceptable only as MVP-level mock authentication, not production-grade security.

---

## Next Technical Priorities (Agreed Direction)

1. Introduce production-grade authentication/authorization baseline.
2. Introduce Flyway migrations and migrate away from schema-by-`ddl-auto`.
3. Standardize database strategy (clear long-term decision for SQLite vs PostgreSQL by environment).
4. Keep all docs synchronized with implemented behavior after each architectural change.
