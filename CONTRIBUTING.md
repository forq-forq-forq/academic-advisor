# Contributing

Engineering guidelines for the Academic Advisor team. Read this before starting any work.

---

## 1. Definition of Ready (DoR)

A task can be moved into an active sprint only when **all** of the following are met:

- [ ] Follows the User Story format: _"As a [user], I want [action], so that [benefit]."_
- [ ] Acceptance Criteria are explicitly defined.
- [ ] Estimated by the team in Story Points (Fibonacci: 1, 2, 3, 5, 8, 10).

---

## 2. Definition of Done (DoD)

A task is marked as **Done** only when all of the following are true:

- [ ] Code compiles locally (`./mvnw clean compile`).
- [ ] Unit and integration tests are written and passing (`./mvnw clean test`).
- [ ] Flyway migration added if the database schema changed.
- [ ] Documentation in `/docs` is updated (if DB schemas, APIs, or architecture changed).
- [ ] At least 1 approving code review received.
- [ ] Merged into `main` and CI pipeline passes.

---

## 3. Git Branching Conventions

Always branch from the latest `main`:

```bash
git checkout main
git pull origin main
git checkout -b <type>/<issue>-<short-description>
```

**Branch naming examples:**

| Type | Example |
|---|---|
| Feature | `feat/US12-add-login` |
| Bug fix | `fix/45-null-pointer-search` |
| Documentation | `docs/3-update-readme` |
| Refactor | `refactor/extract-auth-service` |

> **Direct commits to `main` are not allowed.** All changes go through Pull Requests.

---

## 4. Commit Messages

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>: <short description>
```

| Prefix | Usage |
|---|---|
| `feat:` | New user-facing feature or capability |
| `fix:` | Bug fix |
| `refactor:` | Code change with no behavior modification |
| `docs:` | Documentation updates |
| `test:` | Adding or updating tests |
| `chore:` | Build scripts, CI config, dependency updates |

**Examples:**
```
feat: add user registration form and validation
fix: handle empty query in dictionary search
docs: update db schema for user roles
refactor: extract authentication service
```

---

## 5. Pull Request Process

1. Create a PR using the [PR template](/.github/PULL_REQUEST_TEMPLATE.md).
2. Link the corresponding Issue (`Closes #12`).
3. Assign at least 1 reviewer from the team.
4. All CI checks must pass before merging.
5. Use **Squash and Merge** to keep `main` history clean.

---

## 6. Code Style

The project uses [`.editorconfig`](/.editorconfig) for consistent formatting:

- **Charset:** UTF-8
- **Line endings:** LF
- **Indentation:** 4 spaces (Java), 2 spaces (HTML, XML, YAML, properties)
- **Trailing whitespace:** trimmed
- **Final newline:** always

Ensure your IDE respects EditorConfig settings. Most modern IDEs (IntelliJ, VS Code) support it natively or via a plugin.

---

## 7. Local Development Setup

See the [README](README.md) for full setup instructions. The quick version:

```bash
cp .env.example .env         # Configure your environment
docker compose up --build    # Start full stack
./mvnw clean test            # Run tests
```