# Database Schema

This document describes the relational schema used by the Academic Advisor application. The schema is generated from JPA entities using Hibernate `ddl-auto` (current state). Database engine depends on active profile: SQLite for `default`/`test`, PostgreSQL for `docker`.

## Entity-Relationship Diagram

```mermaid
erDiagram
    faculties ||--o{ majors : offers
    majors ||--o{ curriculum_courses : defines
    courses ||--o{ curriculum_courses : includes
    courses ||--o{ prerequisites : "is required by"
    courses ||--o{ prerequisites : "depends on"
    majors ||--o{ students : enrolls
    student_accounts ||--o| students : authenticates
    students ||--o{ student_courses : completed
    courses ||--o{ student_courses : completed_by
    students ||--o{ student_enrolled_courses : enrolled_in
    courses ||--o{ student_enrolled_courses : taken_by
    students ||--o{ cart_items : plans
    courses ||--o{ cart_items : planned_in
    students ||--o{ roadmap_items : schedules
    courses ||--o{ roadmap_items : scheduled_in

    cart_items {
        BIGINT id PK "Auto-generated"
        BIGINT student_id FK "References students.id"
        BIGINT course_id FK "References courses.id"
    }

    roadmap_items {
        BIGINT id PK "Auto-generated"
        BIGINT student_id FK "References students.id"
        BIGINT course_id FK "References courses.id"
        INTEGER semester "Target semester 1-8"
    }

    faculties {
        BIGINT id PK "Auto-generated"
        VARCHAR code UK "e.g. FE&NS"
        VARCHAR name "Faculty name"
    }

    majors {
        BIGINT id PK "Auto-generated"
        VARCHAR code UK "e.g. CS"
        VARCHAR name "Major title"
        BIGINT faculty_id FK "References faculties.id"
        INTEGER total_credits "Required ECTS (e.g. 240)"
    }

    curriculum_courses {
        BIGINT id PK "Auto-generated"
        BIGINT major_id FK "References majors.id"
        BIGINT course_id FK "References courses.id"
        INTEGER semester "Recommended semester (1-8)"
        INTEGER catalog_year "e.g. 2024"
        VARCHAR course_type "CORE, ELECTIVE, GENERAL"
    }

    student_accounts {
        BIGINT id PK "Auto-generated"
        VARCHAR student_id "Optional Student ID link"
        VARCHAR email UK "University email"
        VARCHAR password_hash "BCrypt hash"
        BIGINT major_id FK "Selected major"
        INTEGER catalog_year "Selected curriculum year"
    }

    students {
        BIGINT id PK "Auto-generated"
        VARCHAR student_id UK "Business identifier"
        VARCHAR name "NOT NULL"
        VARCHAR email UK "Nullable"
        DOUBLE gpa "Current GPA"
        BIGINT major_id FK "References majors.id"
        INTEGER catalog_year "e.g. 2024"
        INTEGER current_semester "e.g. 3"
        BIGINT account_id FK "References student_accounts.id"
    }

    courses {
        BIGINT id PK "Auto-generated"
        VARCHAR code UK "Course code (e.g. CS101)"
        VARCHAR name "NOT NULL"
        INTEGER credits "Credit hours (ECTS)"
        INTEGER difficulty "Difficulty 1-5 (default 3)"
        VARCHAR description "Course summary"
        VARCHAR season "FALL, SPRING, BOTH"
    }

    prerequisites {
        BIGINT id PK "Auto-generated"
        BIGINT course_id FK "References courses.id"
        BIGINT prerequisite_course_id FK "References courses.id"
    }

    student_courses {
        BIGINT student_id FK "References students.id"
        BIGINT course_id FK "References courses.id"
        VARCHAR letter_grade "Nullable recorded letter grade"
        VARCHAR semester_taken "Nullable actual term, e.g. Fall 2024"
    }

    student_enrolled_courses {
        BIGINT student_id FK "References students.id"
        BIGINT course_id FK "References courses.id"
    }
```

## Tables

### `faculties`

Stores university faculties.

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | BIGINT | PK, AUTO | Database primary key. |
| `code` | VARCHAR | NOT NULL, UNIQUE | Faculty code (e.g. `"FE&NS"`). |
| `name` | VARCHAR | NOT NULL | Faculty title. |

### `majors`

Stores degree programs and their total credit requirements.

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | BIGINT | PK, AUTO | Database primary key. |
| `code` | VARCHAR | NOT NULL, UNIQUE | Major code (e.g. `"CS"`). |
| `name` | VARCHAR | NOT NULL | Major title. |
| `faculty_id` | BIGINT | FK → `faculties.id`, NOT NULL | Faculty offering this program. |
| `total_credits`| INTEGER | NOT NULL (default 240) | Total degree credits needed. |

### `curriculum_courses`

Maps 4-year degree requirements by semester and catalog year for each major.

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | BIGINT | PK, AUTO | Database primary key. |
| `major_id` | BIGINT | FK → `majors.id`, NOT NULL | Major associated with this plan. |
| `course_id` | BIGINT | FK → `courses.id`, NOT NULL | Course required in this curriculum. |
| `semester` | INTEGER | NOT NULL | Recommended semester number (1–8). |
| `catalog_year` | INTEGER | NOT NULL | Curriculum version year (e.g. 2024). |
| `course_type` | VARCHAR | NOT NULL | Type: `CORE`, `ELECTIVE`, `GENERAL`. |

**Unique constraint:** `(major_id, course_id, catalog_year)`

### `students`

Stores student academic profiles.

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | BIGINT | PK, AUTO | Database primary key. |
| `student_id` | VARCHAR | NOT NULL, UNIQUE | Business identifier (e.g. `"240103000"`). |
| `name` | VARCHAR | NOT NULL | Full name of the student. |
| `email` | VARCHAR | UNIQUE | Email address. |
| `gpa` | DOUBLE | — | Grade point average. |
| `major_id` | BIGINT | FK → `majors.id` | Student's enrolled major. |
| `catalog_year` | INTEGER | — | Catalog year governing their curriculum. |
| `current_semester` | INTEGER | — | Current semester (e.g. 3). |
| `career_goal` | VARCHAR(255) | — | Optional student career aspiration. |
| `role` | VARCHAR(30) | DEFAULT `'STUDENT'` | Security role (`STUDENT`, `ADVISOR`, `ADMIN`). |
| `account_id` | BIGINT | FK → `student_accounts.id` | Linked authentication account. |

### `student_accounts`

Stores login credentials.

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | BIGINT | PK, AUTO | Database primary key. |
| `student_id` | VARCHAR | — | Associated student ID. |
| `email` | VARCHAR | NOT NULL, UNIQUE | Normalized university email address. |
| `password_hash` | VARCHAR | NOT NULL | BCrypt hash. |
| `major_id` | BIGINT | FK → `majors.id` | Major selected during registration. |
| `catalog_year` | INTEGER | — | Curriculum version selected during registration. |
| `career_goal` | VARCHAR(255) | — | Career aspiration captured during onboarding. |
| `role` | VARCHAR(30) | DEFAULT `'STUDENT'` | Security role (`STUDENT`, `ADVISOR`, `ADMIN`). |

### `courses`

Stores the course catalog.

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | BIGINT | PK, AUTO | Database primary key. |
| `code` | VARCHAR | NOT NULL, UNIQUE | Course code (e.g. `"CS101"`). |
| `name` | VARCHAR | NOT NULL | Course title. |
| `credits` | INTEGER | NOT NULL | Number of credit hours (ECTS). |
| `difficulty` | INTEGER | DEFAULT 3 | Course difficulty rating (1–5). |
| `description` | VARCHAR(500) | — | Course overview text. |
| `season` | VARCHAR(16) | DEFAULT `'BOTH'` | Offering term (`FALL`, `SPRING`, `BOTH`). |

### `prerequisites`

Represents directed prerequisite relationships between courses (`course_id` requires `prerequisite_course_id`).

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | BIGINT | PK, AUTO | Database primary key. |
| `course_id` | BIGINT | FK → `courses.id`, NOT NULL | Course with prerequisite. |
| `prerequisite_course_id` | BIGINT | FK → `courses.id`, NOT NULL | Prerequisite that must be completed. |

**Unique constraint:** `(course_id, prerequisite_course_id)`

### `cart_items`

Stores semester planning cart items per student (US-11).

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | BIGINT | PK, AUTO | Database primary key. |
| `student_id` | BIGINT | FK → `students.id`, NOT NULL | Student who owns the cart. |
| `course_id` | BIGINT | FK → `courses.id`, NOT NULL | Planned course added to cart. |

**Unique constraint:** `(student_id, course_id)`

### `roadmap_items`

Stores multi-year 8-semester roadmap placements per student (US-18).

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | BIGINT | PK, AUTO | Database primary key. |
| `student_id` | BIGINT | FK → `students.id`, NOT NULL | Student owning the roadmap. |
| `course_id` | BIGINT | FK → `courses.id`, NOT NULL | Course placed on roadmap. |
| `semester` | INTEGER | NOT NULL | Target semester number (`1`–`8`). |

**Unique constraint:** `(student_id, course_id)`

### `password_reset_tokens`

Single-use expiring password reset tokens (`US-05`).

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | BIGINT | PK, AUTO | Database primary key. |
| `token` | VARCHAR(64) | NOT NULL, UNIQUE | Random UUID reset token. |
| `student_id` | VARCHAR(32) | NOT NULL | Target student business identifier. |
| `email` | VARCHAR(254) | NOT NULL | Target student university email. |
| `expires_at` | TIMESTAMP | NOT NULL | Token expiration timestamp (30 min TTL). |
| `used` | BOOLEAN | NOT NULL, DEFAULT FALSE | Prevents replay after completion/rotation. |
| `created_at` | TIMESTAMP | NOT NULL | Creation timestamp. |

### `registration_periods`

Academic registration schedule windows powering dashboard alerts (`US-09`).

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | BIGINT | PK, AUTO | Database primary key. |
| `term_name` | VARCHAR(120) | NOT NULL | Registration window title (e.g. `Fall 2026 Course Registration`). |
| `start_date` | DATE | NOT NULL | Registration opening date. |
| `end_date` | DATE | NOT NULL | Registration closing deadline. |
| `active` | BOOLEAN | NOT NULL, DEFAULT TRUE | Whether alert evaluation considers this period. |
| `description` | VARCHAR(500) | — | Guidance text displayed inside the dashboard alert. |

### Join Tables

- **`student_courses`**: Many-to-Many join table representing courses completed by the student.
- **`student_enrolled_courses`**: Many-to-Many join table representing courses currently taken by the student in their active semester.

## Seed / Demo Data

| Entity | Key Fields | Description |
|---|---|---|
| Faculty | `code = "FE&NS"` | Faculty of Engineering and Natural Sciences |
| Major | `code = "CS"`, `total_credits = 240` | Computer Science under FE&NS |
| Courses | 22 demo catalog courses | CS, MATH, ENG, PHYS, and general courses for the seeded Computer Science plan |
| Curriculum | 22 demo mappings | CS 2024 test curriculum across Semesters 1 through 8; not an official study plan |
| Student | `student_id = "240103000"`, `name = "John Doe"`, `email = "240103000@sdu.edu.kz"`, `GPA = 3.8`, `current_semester = 3` | Linked to account, 27 completed ECTS (Sem 1-2), 14 enrolled ECTS (Sem 3) |
| Account | `student_id = "240103000"`, password = `Student123!@#` | Hashed password for demo student |
