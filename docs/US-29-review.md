# US-29 / PR #38 code review

Reviewed the actual PR head `f27523e` against [issue #37](https://github.com/forq-forq-forq/academic-advisor/issues/37),
`CONTRIBUTING.md`, the current architecture baseline, and the Sprint 3 section of
`QUICK_RETROSPECTIVE.md`. This report includes the focused corrections made on
the existing PR branch. No replacement implementation, new issue, or new PR was created.

## Acceptance criteria

| Issue #37 criterion | Original PR | After review fixes | Evidence |
|---|---|---|---|
| Code, name, letter grade, ECTS credits and semester for every completed course, ordered by code | PASS | PASS | Database-backed controller tests; actual browser rendering of recorded values |
| Only authenticated student's completions; exclude enrolled-only courses and other students | PASS | PASS | Query filters database student ID; added test with two students completing CS101 with different grades and semesters; browser student-ID parameters cannot override session |
| Empty history shows "No completed courses yet." | PASS | PASS | MockMvc and actual browser checks |
| Missing grades/semesters show "Not recorded", without inference | PASS | PASS | Null and blank metadata tests; original seeded data has neither value |
| Caption, column headers, escaped stored text and horizontal scrolling on narrow screens | FAIL | PASS | Original mobile table widened the panel; corrected sizing and verified keyboard scrolling at 375px, plus five viewport sizes and accessibility tree |

## Confirmed defects corrected

1. **Medium: mobile table does not scroll within the panel.** The grid item's
   default intrinsic minimum width let the new table widen the academic panel.
   At 375px, the panel was 485px wide, the table wrapper was 403px wide, and its
   scroll range was zero. Adding `min-width: 0` to `.standing-panel` lets the
   wrapper constrain the table: panel 343px, wrapper 261px, table 403px, with a
   142px scroll range. ArrowRight moved the focused wrapper by 40px. At 320px the
   wrapper is 212px wide and remains inside the viewport. Desktop layouts also pass.
2. **Low: empty histories expose an empty keyboard focus stop and named region.**
   The condition hid only the table, leaving its `tabindex="0"` region present.
   The condition now hides the wrapper as well. The empty-state message remains.
3. **Documentation gap corrected:** the architecture baseline did not list the
   new transcript view. Added the US-29 entry without changing the architecture.

## Database retrieval and population

- `DashboardController` resolves the business student ID from trusted session
  attributes, reloads that student from the repository, and passes its **database
  primary key** to `CourseCompletionRepository.findForStudent`.
- The JPQL query filters `CourseCompletion.id.studentId`, fetch-joins the shared
  `Course`, and orders by its unique, non-null code. The DTO reads the recorded
  `letter_grade` and `semester_taken` from `student_courses`, and code, name and
  credits from `courses`. The fetch join avoids additional per-row course queries.
- No production writer for grades/terms exists. The seeder writes completion
  membership only; registration creates an empty history. The SQL migration adds
  nullable columns without backfilling them. Tests populate their own data using
  JDBC; the browser checks temporarily populate an isolated review database.
- This is acceptable for **issue #37 as written**: missing metadata explicitly
  has a fallback, and transcript import/editing is explicitly excluded. The
  feature displays supplied grades and semesters; it does not obtain real
  university transcripts. It would be incomplete if the product expectation
  were automatic population of an actual transcript.
- Credits are current catalog credits, not historical snapshots. The composite
  student/course key supports one completion per course, not repeated attempts.
  Both limits are already documented in the issue and remain outside this story.

## Migration and architecture

`V1__completed_course_history.sql` adds two nullable VARCHAR columns to the
existing join table. It leaves completion membership, primary keys and foreign
keys unchanged. The statements are compatible with SQLite and PostgreSQL SQL;
the SQLite migration test passes and preserves the pre-existing row.

An additional startup experiment used a copy of the seeded database with the
original two-column `student_courses` schema and eight completions. The current
Hibernate `ddl-auto=update` added both metadata columns and retained all eight
rows. This also confirms that the two JPA mappings can share this table at startup.

Important execution limits:

- There is **no Flyway dependency/runtime**. A file in `db/migration` is not
  automatically executed. Current schema updates are Hibernate's responsibility,
  consistently with the authoritative architecture baseline.
- For the documented manual upgrade path, run the script once before upgraded
  startup. It assumes the existing table exists and both added columns are absent.
  It is not idempotent: rerunning after Hibernate adds the columns fails. If a
  manual client commits each statement separately, an interrupted run can leave
  only one new column; inspect schema state before retrying. Full Flyway
  adoption/baselining is a separate infrastructure task, not added by this review.
- PostgreSQL runtime migration was **not executed locally**: no PostgreSQL or
  Docker executable was available. The CI test suite uses the SQLite test profile
  even though its job provisions a PostgreSQL service. A passing Docker build
  does not establish PostgreSQL migration coverage.

The read-only entity/DTO/repository adds a second view of completion membership,
but does not introduce a competing completion table or computation. This is a
reasonable small change to the existing architecture. No extra service layer,
transcript editor, enum conversion, or unrelated refactor is justified here.
Future writers must preserve metadata when changing completion membership;
replacing/deleting join rows would also replace/delete the grades stored there.

## Authentication and regressions

- Logged-out requests redirect to login; stale student IDs invalidate the session
  and redirect. Browser request parameters do not choose the student.
- Two students can share a course while retaining different grades and terms.
  The added isolation test checks the exact DTO for the second student, not just
  an empty result. Stored semester HTML is escaped by `th:text`.
- Existing session authentication and its legacy student-object fallback are
  unchanged. This review does not claim that the application implements Spring
  Security, CSRF protection, or a production authentication redesign.
- Academic standing still uses the stored GPA and existing completed/enrolled
  course sets. With recorded transcript metadata, tests retain GPA 3.80, Honor
  Standing, 27 completed ECTS and 14 enrolled ECTS. CS204's completed prerequisite
  remains satisfied; CS202's missing prerequisite is still rejected.

## UI and accessibility

Headless Edge checks passed at widths 1280, 900, 768, 375 and 320px. The corrected
table no longer changes the panel/page width compared with hiding the US-29 card.
Long unbroken cell text also scrolls without widening the panel. The accessibility
tree exposes the named table, caption, five column headers and cells; the wrapper
is keyboard-focusable and arrow-scrollable. Browser checks also verified actual
database grades/terms and the empty state without an empty focusable region.

The existing dashboard header still causes a 426px document width on narrow
screens, including when the US-29 card is hidden. That pre-existing page-level
responsive issue was left unchanged. This was a semantic/keyboard browser check,
not a complete screen-reader or cross-browser accessibility audit.

## Test results

| Run | Result |
|---|---|
| Original PR: `mvnw.cmd clean test` | 71 tests, 0 failures/errors/skips |
| Reviewed changes: `mvnw.cmd test` | 75 tests, 0 failures/errors/skips |
| Browser checks described above | PASS |
| Existing SQLite database startup upgrade | PASS; 8 completion rows preserved |
| Original-head GitHub CI | Java 17 tests and Docker build PASS |

Local Java was 23, compiling for release 17. The four additional automated tests
cover same-course student isolation/request-ID overrides, unauthenticated access,
standing/credit/prerequisite regression with metadata, and blank metadata. The
existing empty-history test now also rejects the empty focusable region. The
fresh PR-head CI results should be checked after pushing these fixes.

## CONTRIBUTING and Sprint 3 workflow

- PASS: user story, explicit acceptance criteria and QA cases; feature branch
  from the then-current main; Conventional Commit; existing PR with `Closes #37`;
  tests, SQL migration file, schema and architecture documentation.
- PENDING: the story's suggested 3 points are explicitly pending **team**
  estimation. No confirmed team estimate is recorded in the issue.
- PENDING: GitHub returned no requested reviewers and no approving reviews.
  A teammate must be requested and provide approval before merge under
  `CONTRIBUTING.md`. This report is not an independent GitHub approval of the
  author's own PR.
- UNVERIFIED: issue/PR have no milestone or project items returned by the API,
  and the token lacks `read:project` to inspect the linked sprint board. The
  repository's Sprint 3 documentation describes October MVP stabilization,
  integration testing and UI polish, but provides no US-29 board assignment.
  Team estimation and board/iteration placement must be confirmed by the team.
- NOT DONE: PR remains unmerged and the issue remains open. Use the repository's
  squash-merge workflow after approval and green checks. No automatic merge was
  requested or performed by this review.

**Verdict:** all issue acceptance criteria pass after the focused fixes. The
implementation is ready for teammate review, but is not yet merge-ready under
the repository workflow until approval is received and current-head CI is green;
Sprint 3 planning metadata remains unverified.
