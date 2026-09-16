# AI-Powered Academic Advisor

An intelligent academic planning and course recommendation system developed for SDU University students. This project is developed within the Project Management Information Systems course using an Agile/Scrum framework across 7 two-week sprints by a 5-member engineering team.

---

## Tech Stack

* **Language:** Java 17+
* **Framework:** Spring Boot 3.x (Spring Web, Spring Data JPA)
* **Frontend / Templating:** Thymeleaf, HTML5, CSS3
* **Database:** SQLite (Sprint 1 MVP), with planned migration to PostgreSQL
* **Build Tool:** Maven Wrapper (`mvnw`, `mvnw.cmd`)
* **Utilities:** Project Lombok, Hibernate Community Dialects

---

## Project Structure & Architecture

The repository adheres to standard Maven conventions and a classic **Layered Architecture** to keep concerns separated across team members:

```text
.
├── .mvn/wrapper/              # Maven wrapper binaries and configuration
├── src/
│   ├── main/
│   │   ├── java/kz/edu/sdu/advisor/
│   │   │   ├── config/        # Security, API client configurations (Gemini AI)
│   │   │   ├── controller/    # Web endpoints & Thymeleaf view controllers
│   │   │   ├── service/       # Business logic (advising rules, prerequisite engine)
│   │   │   ├── repository/    # Spring Data JPA interfaces
│   │   │   ├── model/         # JPA entities (@Entity) and DTOs
│   │   │   └── exception/     # Global exception handlers and error models
│   │   └── resources/
│   │       ├── static/        # Static assets (CSS, client-side JS, images)
│   │       ├── templates/     # Thymeleaf HTML views
│   │       └── application.properties # App configuration and datasources
│   └── test/                  # Unit and integration test suites
├── .editorconfig              # Uniform formatting rules across team IDEs
├── .gitattributes             # Line ending normalization (LF/CRLF)
├── .gitignore                 # Build artifacts and local cache exclusions
├── advisor.db                 # Local SQLite database file
├── mvnw / mvnw.cmd            # Platform-independent Maven execution scripts
└── pom.xml                    # Project dependencies and plugins
```

---

## Architectural Principles

* **Controller Layer:** Handles incoming HTTP requests, binds inputs, and returns Thymeleaf template views or API payloads. No direct business logic belongs here.

* **Service Layer:** Houses all business rules, GPA checks, AI prompt orchestration, and prerequisite validations.

* **Repository Layer:** Encapsulates database interactions through Spring Data JPA interfaces.

* **Model Layer:** Defines database entities (Student, Course, Prerequisite) and data transfer structures (DTOs).

---

## Getting Started
### Prerequisites

1. JDK 17 or higher installed.

2. Correctly configured JAVA_HOME environment variable:
```bash
echo $JAVA_HOME
# Expected output: /path/to/jdk-17 (e.g., /usr/lib/jvm/java-17-openjdk-amd64)
```

### Local Setup & Execution

1. Clone the repository:
```bash
git clone git@github.com:forq-forq-forq/academic-advisor.git
cd academic-advisor
```
2. Run the application:

    2.1. Linux / macOS / WSL 2:
    ```bash
    ./mvnw clean spring-boot:run
    ```
    2.2. Windows (PowerShell / Command Prompt):
    ```bash
    mvnw.cmd clean spring-boot:run
    ```
3. Access the application: Open http://localhost:8080 in your web browser.

---

## Database (Sprint 1 MVP)

    The system uses an embedded SQLite instance running locally via advisor.db.

    Schema definitions synchronize automatically on startup via spring.jpa.hibernate.ddl-auto=update.

    Important: Do not write database-specific native SQL queries (nativeQuery = true). Stick strictly to Spring Data repository query methods and JPQL so the planned migration to PostgreSQL will require zero code refactoring in business services.

---

## Team Workflow & Git Standards

**Direct commits to the main branch are restricted. All contributions must go through Pull Requests.**
### 1. Branch Naming Conventions

Always create branches from the latest state of main:
```bash
git checkout main
git pull origin main
git checkout -b feature/US<id>-short-description
# Example: git checkout -b feature/US3-student-login
```

### 2. Commit Message Guidelines (Conventional Commits)

Format every commit message with a clear prefix:

* **feat:** A new user-facing feature or domain capability

* **fix:** A bug fix

* **refactor:** Code modification without behavioral changes

* **docs:** Documentation updates

* **chore:** Build scripts, dependency adjustments, or tool configurations

### 3. Pull Request Requirements

* Every PR must link to its corresponding User Story / Issue.

* At least 1 peer review approval is strictly required before merging.

* Code style must follow the rules defined in .editorconfig.