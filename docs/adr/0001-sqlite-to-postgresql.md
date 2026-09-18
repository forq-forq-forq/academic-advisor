# ADR-0001: Migrate from SQLite to PostgreSQL

**Status:** Accepted

**Date:** 2026-09-18

## Context

During Sprint 1, the project used **SQLite** as its database to enable rapid prototyping. SQLite was an attractive initial choice because it requires zero infrastructure — no server process, no installation, and the entire database lives in a single file. This allowed the team to focus on domain modeling and API design without worrying about database setup.

However, as the project matured, several limitations of SQLite became apparent:

- **Foreign key enforcement is off by default.** SQLite does not enforce foreign key constraints unless explicitly enabled per connection via `PRAGMA foreign_keys = ON`. This led to data integrity issues during development that would not have occurred with a traditional RDBMS.
- **Concurrent write access is limited.** SQLite uses file-level locking, meaning only one writer can operate at a time. With multiple team members running the application and tests simultaneously, this became a bottleneck.
- **Production viability.** SQLite is not a realistic choice for a deployed web application that needs to handle concurrent users, connection pooling, and transactional guarantees at scale.

The team needed a database that enforces referential integrity out of the box, supports concurrent access, and mirrors what would be used in a production deployment.

## Decision

Migrate to **PostgreSQL** as the primary relational database for all environments (development, testing, and production).

Key implementation details:

- PostgreSQL runs as a **Docker container** defined in the project's `docker-compose.yml`, eliminating the need for manual installation and ensuring every team member runs an identical database instance.
- **Flyway** is used for schema migration management, ensuring that all schema changes are versioned, repeatable, and tracked in source control.
- **SQLite is fully removed** from the project. No SQLite dependencies, configuration, or database files remain.

## Consequences

### Positive
- **Referential integrity by default.** PostgreSQL enforces foreign key constraints, `NOT NULL`, and unique constraints without additional configuration.
- **Concurrency support.** PostgreSQL's MVCC architecture handles concurrent reads and writes gracefully, removing the file-locking bottleneck.
- **Production parity.** The development environment now mirrors a realistic production setup, reducing "works on my machine" issues.
- **Reproducible environments.** Docker Compose ensures every team member and CI pipeline runs the exact same database version and configuration.
- **Versioned migrations.** Flyway tracks every schema change as a numbered migration script, providing a clear audit trail and safe rollback path.

### Negative
- **Docker dependency.** All developers must have Docker and Docker Compose installed on their machines to run the application locally.
- **Increased setup complexity.** Compared to a zero-config single-file database, running `docker compose up` is an additional step, albeit a minor one.

### Risks
- **Docker onboarding.** Team members who have not used Docker before may require initial guidance. This can be mitigated by clear setup instructions in the project README.
