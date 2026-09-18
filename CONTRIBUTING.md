# 📜 Team Engineering Guidelines

## 1. Definition of Ready (DoR)
A task can only be moved into an active sprint if:
- [ ] It follows the User Story template ("As a / I want / So that").
- [ ] Acceptance Criteria are explicitly defined.
- [ ] A wireframe or UI reference (Miro/Figma) is linked (for frontend work).
- [ ] It has been estimated by the team in Story Points.

## 2. Definition of Done (DoD)
A task is marked as `Done` only when:
- [ ] Code passes all local builds and linter checks.
- [ ] Unit and Integration tests are written and passing.
- [ ] Flyway database migrations have been executed and validated locally.
- [ ] Documentation in `/docs` is updated (if DB schemas, APIs, or architecture changed).
- [ ] Received at least 1 approving Code Review.
- [ ] Merged into `main` and verified on the staging environment.

## 3. Git Branching & Commit Conventions
* **Branches:** `feat/#12-add-login`, `fix/#45-null-pointer-search`, `docs/#3-update-readme`
* **Commit Messages (Conventional Commits):**
  * `feat: add user registration form and validation`
  * `fix: handle empty query in dictionary search`
  * `docs: update db schema for user roles`
  * `refactor: extract authentication service`