# Documentation

Project documentation for the Academic Advisor system.

---

## Contents

| Document | Description |
|---|---|
| [Current Architecture Baseline](architecture/current-architecture-baseline.md) | Actual implemented architecture and policy baseline (source of truth) |
| [System Overview](architecture/system-overview.md) | Architecture, tech stack, layered design, data model, deployment topology |
| [Database Schema](db/schema.md) | ER diagram, table definitions, column constraints, seed data |
| [Architecture Decision Records](adr/) | Log of key technical decisions with context and rationale |

## ADR Index

| ID | Title | Status |
|---|---|---|
| [ADR-0001](adr/0001-sqlite-to-postgresql.md) | SQLite to PostgreSQL migration | Accepted |
| [ADR-0002](adr/0002-adopt-tailwind-css-utility-architecture.md) | Adopt local Tailwind CSS utility architecture | Accepted |

## Feature & Technical Task Docs

| Document | Scope |
|---|---|
| [T-06](T-06.md) | Spring Security authentication, RBAC (`/advisor/**`), `CustomUserDetailsService`, and CSRF protection |
| [T-07](T-07.md) | Static CSS/JS asset extraction and CSRF-aware fetch helpers |
| [T-08](T-08.md) | Offline Tailwind CSS utility layer and theme configuration |
| [US-03](US-03.md) | Onboarding career aspiration quick-select chips and recommendation alignment |
| [US-05](US-05.md) | Self-service `/forgot-password` & `/reset-password` flow with 30m single-use tokens |
| [US-09](US-09.md) | Dashboard course registration alert banners (`Fall 2026 Course Registration`) |
| [US-10](US-10.md) | Dashboard adaptive Recommended Next Action hero card |
| [US-29](US-29.md) | Completed course history view |
| [US-31](US-31.md) | Automatic credit-weighted cumulative GPA |

---

## Related

- [README](../README.md) — Project overview and quick start guide
- [Contributing](../CONTRIBUTING.md) — Team engineering guidelines (DoR, DoD, Git conventions)
