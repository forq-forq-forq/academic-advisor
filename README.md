# AI-Powered Academic Advisor

An intelligent academic planning and course recommendation system for SDU University students. Students authenticate with their university ID and interact with an AI-powered advisor that helps with course selection, prerequisite verification, and academic planning.

Built with Spring Boot, Thymeleaf, PostgreSQL, and Google Gemini AI.

---

## Tech Stack

| | Technology |
|---|---|
| **Backend** | Java 17, Spring Boot 3.x, Spring Data JPA |
| **Frontend** | Thymeleaf, HTML5, CSS3 |
| **Database** | SQLite (default/test), PostgreSQL 16 (Docker profile) |
| **Security** | MVP session-based auth via HttpSession (Student ID) |
| **AI** | Google Gemini API |
| **Infrastructure** | Docker, Docker Compose, GitHub Actions CI |
| **Build** | Maven Wrapper |

---

## Prerequisites

- [Docker](https://docs.docker.com/get-docker/) and Docker Compose (included with Docker Desktop)
- A [Google Gemini API key](https://aistudio.google.com/app/apikey)
- Git

> **Without Docker:** Java 17+ (default profile uses local SQLite file `advisor.db`).

---

## Quick Start

### 1. Clone the repository

```bash
git clone git@github.com:forq-forq-forq/academic-advisor.git
cd academic-advisor
```

### 2. Configure environment variables

```bash
cp .env.example .env
```

Open `.env` and set your Gemini API key:

```env
GEMINI_API_KEY=your_actual_api_key_here
```

### 3. Start the application

**With Docker (recommended):**

```bash
docker compose up --build
```

This starts both PostgreSQL and the application. Open [http://localhost:8080](http://localhost:8080) in your browser.

**Without Docker (SQLite default profile):**

```bash
# Linux / macOS / WSL
./mvnw spring-boot:run

# Windows (PowerShell)
mvnw.cmd spring-boot:run
```

Optional PostgreSQL run (local process or container) is available via `docker` profile settings.

### 4. Log in

Use the pre-seeded demo account:

| Field | Value |
|---|---|
| **Student ID** | `240103000` |
| **Name** | John Doe |
| **Email** | 240103000@sdu.edu.kz |
| **GPA** | 3.8 |

Pre-loaded courses: `CS101` (Introduction to Computer Science), `MATH101` (Calculus I).
The demo catalog also includes a non-official CS 2024 curriculum across eight semesters for registration testing.

---

## Running Tests

```bash
./mvnw clean test
```

Tests use an isolated SQLite test database under `target/` and mocked external services — no Docker or API keys required.

---

## Project Structure

```
advisor/
├── src/main/java/kz/edu/sdu/advisor/
│   ├── config/          # Spring configuration beans
│   ├── controller/      # Web endpoints and view controllers
│   ├── exception/       # Custom exception hierarchy
│   ├── model/           # JPA entities and Gemini DTOs
│   ├── repository/      # Spring Data JPA interfaces
│   └── service/         # Business logic layer
├── src/main/resources/
│   ├── templates/       # Thymeleaf HTML views
│   └── application.properties
├── docs/                # Project documentation
├── Dockerfile           # Multi-stage application build
└── docker-compose.yml   # PostgreSQL + app services
```

---

## Documentation

| Document | Description |
|---|---|
| [Current Architecture Baseline](docs/architecture/current-architecture-baseline.md) | Actual implemented architecture and policy baseline |
| [System Overview](docs/architecture/system-overview.md) | Architecture, tech stack, data model, deployment |
| [Database Schema](docs/db/schema.md) | ER diagram and table definitions |
| [Contributing](CONTRIBUTING.md) | Team guidelines: DoR, DoD, Git conventions |
| [ADR Log](docs/adr/) | Architecture Decision Records |

---

## Team Links

| Resource | Link |
|---|---|
| 🗺️ Roadmap & Story Map (Miro) | [Open board](https://miro.com/app/dashboard/space/6CR8mnM7hM7RcI71EuYrdU) |
| 📋 Sprint Board (GitHub Projects) | [Open board](https://github.com/users/forq-forq-forq/projects/4/views/1) |
| 🎨 Screen Mockups (Figma / Miro) | [Open sketches](https://...) |
| 📞 Meeting Room | [Google Meet](https://meet.google.com/...) |
