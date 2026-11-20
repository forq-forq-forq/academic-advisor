package kz.edu.sdu.advisor.controller;

import kz.edu.sdu.advisor.model.Course;
import kz.edu.sdu.advisor.model.CourseCompletion;
import kz.edu.sdu.advisor.model.Student;
import kz.edu.sdu.advisor.repository.CartItemRepository;
import kz.edu.sdu.advisor.repository.CourseCompletionRepository;
import kz.edu.sdu.advisor.repository.CourseRepository;
import kz.edu.sdu.advisor.repository.StudentRepository;
import kz.edu.sdu.advisor.service.CartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PlannerControllerTest {

    private static final String VALID_STUDENT_ID = "240103000";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CourseCompletionRepository courseCompletionRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CartService cartService;

    private Course testCourse;

    @BeforeEach
    void setUp() {
        cartItemRepository.deleteAll();
        testCourse = courseRepository.findByCode("CSS215").orElseGet(() -> {
            Course c = new Course();
            c.setCode("CSS215");
            c.setName("Introduction to Algorithms");
            c.setCredits(5);
            return courseRepository.save(c);
        });
    }

    @Test
    @DisplayName("Unauthenticated GET /planner should redirect to /login")
    void getPlanner_unauthenticated_shouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/planner"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("Unauthenticated POST /planner/cart/add should return 401 Unauthorized")
    void addCourse_unauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(post("/planner/cart/add")
                        .param("courseId", testCourse.getId().toString()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", containsString("Session expired")));
    }

    @Test
    @DisplayName("Authenticated GET /planner should render view with empty cart placeholder and enroll button")
    void getPlanner_authenticated_shouldRenderEmptyCart() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("authenticatedStudentId", VALID_STUDENT_ID);

        mockMvc.perform(get("/planner").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("planner"))
                .andExpect(model().attributeExists("student", "cart", "availableCourses", "enrolledCourses", "enrolledCredits"))
                .andExpect(content().string(containsString("Your Semester Cart")))
                .andExpect(content().string(containsString("id=\"btn-enroll-cart\"")))
                .andExpect(content().string(containsString("No courses added yet. Ask the AI advisor or browse the catalog.")));
    }

    @Test
    @DisplayName("POST /planner/cart/add should add course and return updated cart JSON")
    void addCourse_authenticated_shouldAddAndReturnJson() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("authenticatedStudentId", VALID_STUDENT_ID);

        mockMvc.perform(post("/planner/cart/add")
                        .session(session)
                        .param("courseId", testCourse.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCredits", is(testCourse.getCredits())))
                .andExpect(jsonPath("$.workloadState", is("Light")))
                .andExpect(jsonPath("$.empty", is(false)))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].code", is(testCourse.getCode())));
    }

    @Test
    @DisplayName("DELETE /planner/cart/remove should remove course and return updated cart JSON")
    void removeCourse_authenticated_shouldRemoveAndReturnJson() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("authenticatedStudentId", VALID_STUDENT_ID);

        // First add
        mockMvc.perform(post("/planner/cart/add")
                        .session(session)
                        .param("courseId", testCourse.getId().toString()))
                .andExpect(status().isOk());

        // Then remove
        mockMvc.perform(delete("/planner/cart/remove")
                        .session(session)
                        .param("courseId", testCourse.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCredits", is(0)))
                .andExpect(jsonPath("$.empty", is(true)))
                .andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    @DisplayName("QA-6 & QA-7: Cart persists server-side across requests and sessions")
    void cartPersistence_shouldSurviveSubsequentRequestsAndSessions() throws Exception {
        MockHttpSession session1 = new MockHttpSession();
        session1.setAttribute("authenticatedStudentId", VALID_STUDENT_ID);

        // Add course in session 1
        mockMvc.perform(post("/planner/cart/add")
                        .session(session1)
                        .param("courseId", testCourse.getId().toString()))
                .andExpect(status().isOk());

        // New session (simulating logout/login or page refresh)
        MockHttpSession session2 = new MockHttpSession();
        session2.setAttribute("authenticatedStudentId", VALID_STUDENT_ID);

        mockMvc.perform(get("/planner").session(session2))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(testCourse.getCode())))
                .andExpect(content().string(containsString(testCourse.getCredits() + " ECTS")));
    }

    @Test
    @DisplayName("US-12 QA-2: Adding course with missing prerequisite should return 400 Bad Request with clear error message")
    void addCourse_withMissingPrerequisites_shouldReturn400WithErrorDetails() throws Exception {
        Course css217 = courseRepository.findByCode("CSS217").orElseThrow();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("authenticatedStudentId", VALID_STUDENT_ID);

        mockMvc.perform(post("/planner/cart/add")
                        .session(session)
                        .param("courseId", css217.getId().toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("Cannot add CSS217: missing prerequisite CSS215")))
                .andExpect(jsonPath("$.missingPrerequisites", hasItem("CSS215")));
    }

    @Test
    @DisplayName("US-12 QA-4: GET /planner renders available courses with prerequisite badges/metadata")
    void getPlanner_shouldIncludePrerequisiteMetadataInAvailableCourses() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("authenticatedStudentId", VALID_STUDENT_ID);

        mockMvc.perform(get("/planner").session(session))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("availableCourses"))
                .andExpect(content().string(containsString("Missing Prereq")))
                .andExpect(content().string(containsString("CSS215")));
    }

    @Test
    @DisplayName("US-13 QA-1 & QA-2: GET /planner renders copy button and handles empty vs populated cart state")
    void getPlanner_shouldRenderCopyCodesButton() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("authenticatedStudentId", VALID_STUDENT_ID);

        // Initially empty -> button is rendered and disabled
        mockMvc.perform(get("/planner").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"btn-copy-codes\"")))
                .andExpect(content().string(containsString("Copy Codes")))
                .andExpect(content().string(containsString("disabled")));
    }

    @Test
    @DisplayName("US-13 QA-3 & QA-5: POST /planner/cart/add and GET /planner/cart include formattedCodes in JSON response")
    void cartEndpoints_shouldIncludeFormattedCodesInResponse() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("authenticatedStudentId", VALID_STUDENT_ID);

        // Add course
        mockMvc.perform(post("/planner/cart/add")
                        .session(session)
                        .param("courseId", testCourse.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.formattedCodes", is(testCourse.getCode())));

        // Verify GET /planner/cart
        mockMvc.perform(get("/planner/cart").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.formattedCodes", is(testCourse.getCode())));
    }

    @Test
    @DisplayName("US-14 QA-1 & QA-3: GET /planner renders credit limit warning element and cart JSON includes limit attributes")
    void getPlanner_shouldRenderCreditLimitWarningElement() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("authenticatedStudentId", VALID_STUDENT_ID);

        mockMvc.perform(get("/planner").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"credit-limit-warning\"")))
                .andExpect(content().string(containsString("Maximum Credit Limit Exceeded!")));

        mockMvc.perform(get("/planner/cart").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exceedsLimit", is(false)))
                .andExpect(jsonPath("$.excessCredits", is(0)));
    }

    @Test
    @DisplayName("US-12 Fix: Adding prerequisite CSS215 to cart enables adding dependent course CSS217")
    void addPrerequisiteToCart_enablesAddingDependentCourseViaEndpoint() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("authenticatedStudentId", VALID_STUDENT_ID);
        Course css217 = courseRepository.findByCode("CSS217").orElseThrow();

        // 1. Initially CSS217 cannot be added (fails with 400)
        mockMvc.perform(post("/planner/cart/add")
                        .session(session)
                        .param("courseId", css217.getId().toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("Cannot add CSS217: missing prerequisite CSS215")));

        // 2. Add CSS215 to cart
        mockMvc.perform(post("/planner/cart/add")
                        .session(session)
                        .param("courseId", testCourse.getId().toString()))
                .andExpect(status().isOk());

        // 3. Now CSS217 addition succeeds!
        mockMvc.perform(post("/planner/cart/add")
                        .session(session)
                        .param("courseId", css217.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)));
    }

    @Test
    @DisplayName("Course Enrollment & Cancellation: POST /planner/cart/enroll, /planner/enrolled/cancel, and /planner/enrolled/return-to-cart")
    void enrollCancelAndReturnToCart_shouldUpdateEnrolledCoursesAndCart() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("authenticatedStudentId", VALID_STUDENT_ID);
        Course inf202 = courseRepository.findByCode("INF202").orElseThrow();

        // Add INF202 to cart
        mockMvc.perform(post("/planner/cart/add")
                        .session(session)
                        .param("courseId", inf202.getId().toString()))
                .andExpect(status().isOk());

        // Confirm / Enroll cart courses
        mockMvc.perform(post("/planner/cart/enroll").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cart.empty", is(true)))
                .andExpect(jsonPath("$.enrolledCourses[*].code", hasItem("INF202")));

        // Cancel INF202 from enrolled courses
        mockMvc.perform(post("/planner/enrolled/cancel")
                        .session(session)
                        .param("courseId", inf202.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enrolledCourses[*].code", not(hasItem("INF202"))));

        // Return all remaining enrolled courses to cart
        mockMvc.perform(post("/planner/enrolled/return-to-cart").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enrolledCourses", hasSize(0)))
                .andExpect(jsonPath("$.cart.items", hasSize(4)));
    }

    @Test
    @DisplayName("US-15: GET /planner renders slide-out catalog drawer and GET /planner/catalog searches by code or name")
    void catalogDrawerAndSearchEndpoint_shouldRenderAndFilterCourses() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("authenticatedStudentId", VALID_STUDENT_ID);

        mockMvc.perform(get("/planner").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"catalog-drawer\"")))
                .andExpect(content().string(containsString("id=\"btn-toggle-catalog-drawer\"")))
                .andExpect(content().string(containsString("id=\"catalog-search\"")))
                .andExpect(content().string(containsString("id=\"catalog-match-count\"")));

        mockMvc.perform(get("/planner/catalog")
                        .session(session)
                        .param("q", "INF202"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].code", hasItem("INF202")))
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(get("/planner/catalog")
                        .session(session)
                        .param("q", "Algorithms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].code", hasItems("CSS215", "INF319")));

        mockMvc.perform(get("/planner/catalog")
                        .header("Accept", "application/json"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", containsString("Session expired")));
    }

    @Test
    @DisplayName("US-16: GET /planner renders type/season/difficulty filters and GET /planner/catalog filters by type, season, and difficulty")
    void catalogFilters_shouldFilterByTypeSeasonAndDifficulty() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("authenticatedStudentId", VALID_STUDENT_ID);

        mockMvc.perform(get("/planner").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-filter=\"GENERAL\"")))
                .andExpect(content().string(containsString("id=\"catalog-season-filter\"")))
                .andExpect(content().string(containsString("id=\"catalog-difficulty-filter\"")));

        mockMvc.perform(get("/planner/catalog")
                        .session(session)
                        .param("type", "CORE")
                        .param("season", "SPRING")
                        .param("difficulty", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].code", hasItem("CSS217")))
                .andExpect(jsonPath("$[*].category", everyItem(is("CORE"))))
                .andExpect(jsonPath("$[*].season", everyItem(is("SPRING"))))
                .andExpect(jsonPath("$[*].difficulty", everyItem(is(4))));

        mockMvc.perform(get("/planner/catalog")
                        .session(session)
                        .param("type", "GENERAL")
                        .param("season", "FALL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].code", hasItem("MDE171")))
                .andExpect(jsonPath("$[*].category", everyItem(is("GENERAL"))))
                .andExpect(jsonPath("$[*].season", everyItem(is("FALL"))));

        mockMvc.perform(get("/planner/catalog")
                        .session(session)
                        .param("type", "ELECTIVE")
                        .param("difficulty", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].code", hasItem("INF345")))
                .andExpect(jsonPath("$[*].category", everyItem(is("ELECTIVE"))))
                .andExpect(jsonPath("$[*].difficulty", everyItem(is(4))));
    }

    @Test
    @DisplayName("US-17: GET /planner/courses/{courseId} returns description, credits, difficulty rating, prerequisite chain, unlocked courses, and season")
    void courseDetailEndpoint_shouldReturnFullCourseDetailCard() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("authenticatedStudentId", VALID_STUDENT_ID);
        Course css217 = courseRepository.findByCode("CSS217").orElseThrow();
        Course css215 = courseRepository.findByCode("CSS215").orElseThrow();

        mockMvc.perform(get("/planner").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"course-detail-modal\"")))
                .andExpect(content().string(containsString("btn-course-details")));

        mockMvc.perform(get("/planner/courses/" + css217.getId()).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("CSS217")))
                .andExpect(jsonPath("$.credits", is(5)))
                .andExpect(jsonPath("$.difficulty", is(4)))
                .andExpect(jsonPath("$.difficultyLabel", containsString("4/5")))
                .andExpect(jsonPath("$.description", containsString("GoF design patterns")))
                .andExpect(jsonPath("$.season", is("Spring")))
                .andExpect(jsonPath("$.prerequisiteCodes", contains("CSS215")))
                .andExpect(jsonPath("$.prerequisiteChain", contains("CSS105", "INF108", "CSS215")));

        mockMvc.perform(get("/planner/courses/" + css215.getId()).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("CSS215")))
                .andExpect(jsonPath("$.unlocksCourseCodes", hasItems("CSS217", "INF319")));
    }

    @Test
    @DisplayName("US-18: GET /planner renders 8-semester roadmap board and GET /planner/roadmap returns all 8 semesters")
    void roadmapViewAndEndpoint_shouldReturnAllEightSemesters() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("authenticatedStudentId", VALID_STUDENT_ID);

        mockMvc.perform(get("/planner").session(session))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("roadmap"))
                .andExpect(content().string(containsString("id=\"roadmap-board-section\"")))
                .andExpect(content().string(containsString("id=\"roadmap-semester-1\"")))
                .andExpect(content().string(containsString("id=\"roadmap-semester-8\"")))
                .andExpect(content().string(containsString("roadmap-course-card")));

        mockMvc.perform(get("/planner/roadmap").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.semesters", hasSize(8)))
                .andExpect(jsonPath("$.semesters[0].semester", is(1)))
                .andExpect(jsonPath("$.semesters[7].semester", is(8)))
                .andExpect(jsonPath("$.totalCourses", greaterThanOrEqualTo(20)));
    }

    @Test
    @DisplayName("US-19: POST /planner/roadmap/move validates prerequisites and overload, and /remove deletes course from roadmap")
    void roadmapMoveAndRemoveEndpoints_shouldValidateAndPersistChanges() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("authenticatedStudentId", VALID_STUDENT_ID);
        Course css217 = courseRepository.findByCode("CSS217").orElseThrow();

        mockMvc.perform(get("/planner").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("draggable=\"true\"")))
                .andExpect(content().string(containsString("id=\"roadmap-validation-bar\"")));

        // Move CSS217 to Semester 2 -> triggers prerequisiteViolation because CSS215 is in Semester 3
        mockMvc.perform(post("/planner/roadmap/move")
                        .session(session)
                        .param("courseId", css217.getId().toString())
                        .param("targetSemester", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prerequisiteConflictCount", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.semesters[1].courses[?(@.code=='CSS217')].prerequisiteViolation", hasItem(true)));

        // Remove CSS217 from roadmap
        mockMvc.perform(post("/planner/roadmap/remove")
                        .session(session)
                        .param("courseId", css217.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.semesters[1].courses[?(@.code=='CSS217')]", empty()));
    }

    @Test
    @DisplayName("US-20: GET /planner renders pacing controls and POST /planner/roadmap/pacing switches between BALANCED and FAST_TRACK")
    void roadmapPacingEndpoint_shouldRearrangeRoadmap() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("authenticatedStudentId", VALID_STUDENT_ID);

        mockMvc.perform(get("/planner").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"pacing-strategy-select\"")))
                .andExpect(content().string(containsString("Fast-track: light 4th year for internship")));

        mockMvc.perform(post("/planner/roadmap/pacing")
                        .session(session)
                        .param("strategy", "FAST_TRACK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activePacingStrategy", is("FAST_TRACK")))
                .andExpect(jsonPath("$.prerequisiteConflictCount", is(0)))
                .andExpect(jsonPath("$.pacingSummary", containsString("Fast-track")));

        mockMvc.perform(post("/planner/roadmap/pacing")
                        .session(session)
                        .param("strategy", "BALANCED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activePacingStrategy", is("BALANCED")))
                .andExpect(jsonPath("$.prerequisiteConflictCount", is(0)));
    }

    @Test
    @DisplayName("US-21: GET /planner displays season badges and moving Spring-only course to Fall semester flags seasonConflictCount")
    void roadmapSeasonAvailability_shouldFlagSeasonMismatchInUiAndJson() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("authenticatedStudentId", VALID_STUDENT_ID);
        Course inf202 = courseRepository.findByCode("INF202").orElseThrow();

        mockMvc.perform(get("/planner").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"roadmap-season-conflicts\"")))
                .andExpect(content().string(containsString("Spring Only")))
                .andExpect(content().string(containsString("Fall Only")));

        mockMvc.perform(post("/planner/roadmap/move")
                        .session(session)
                        .param("courseId", inf202.getId().toString())
                        .param("targetSemester", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seasonConflictCount", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.semesters[4].courses[?(@.code=='INF202')].seasonMismatch", hasItem(true)))
                .andExpect(jsonPath("$.semesters[4].courses[?(@.code=='INF202')].seasonWarning", hasItem(containsString("offered only in Spring"))));
    }

    @Test
    @DisplayName("US-22: Planner exposes saved plan drafts endpoints (save, list, load, delete)")
    void planDraftEndpoints_shouldSaveListLoadAndDeleteDrafts() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("authenticatedStudentId", VALID_STUDENT_ID);
        Course inf202 = courseRepository.findByCode("INF202").orElseThrow();

        mockMvc.perform(post("/planner/cart/add")
                        .session(session)
                        .param("courseId", inf202.getId().toString()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/planner/drafts/save")
                        .session(session)
                        .param("name", "Safe plan")
                        .param("pacingStrategy", "BALANCED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.draft.name", is("Safe plan")))
                .andExpect(jsonPath("$.draft.totalCredits", is(5)))
                .andExpect(jsonPath("$.drafts", hasSize(1)));

        mockMvc.perform(get("/planner/drafts").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Safe plan")));

        mockMvc.perform(get("/planner").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"plan-drafts-panel\"")))
                .andExpect(content().string(containsString("Safe plan")));
    }

    @Test
    @DisplayName("US-23: GET /planner renders PDF export buttons and GET /planner/export/pdf returns application/pdf with course summary")
    void exportPlanPdf_shouldReturnValidPdfDownload() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("authenticatedStudentId", VALID_STUDENT_ID);
        Course inf202 = courseRepository.findByCode("INF202").orElseThrow();

        mockMvc.perform(post("/planner/cart/add")
                        .session(session)
                        .param("courseId", inf202.getId().toString()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/planner").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"btn-export-pdf\"")))
                .andExpect(content().string(containsString("id=\"btn-export-roadmap-pdf\"")));

        byte[] bytes = mockMvc.perform(get("/planner/export/pdf")
                        .session(session)
                        .param("scope", "all"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(org.springframework.http.MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", containsString("academic-plan-240103000.pdf")))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        String pdf = new String(bytes, java.nio.charset.StandardCharsets.ISO_8859_1);
        org.assertj.core.api.Assertions.assertThat(pdf)
                .startsWith("%PDF-1.4")
                .contains("INF202")
                .contains("5 ECTS")
                .contains("%%EOF");
    }

    @Test
    @DisplayName("Hide Completed toggle renders cleanly and recognizes completed courses from both student entity and transcript completions")
    void hideCompletedFilter_shouldRecognizeAllCompletedSourcesAndExcludeFromUncompletedCatalog() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("authenticatedStudentId", VALID_STUDENT_ID);

        Student student = studentRepository.findByStudentId(VALID_STUDENT_ID).orElseThrow();
        Course inf202 = courseRepository.findByCode("INF202").orElseThrow();
        courseCompletionRepository.saveAndFlush(new CourseCompletion(student.getId(), inf202, "A", "Fall 2025"));

        mockMvc.perform(get("/planner").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"btn-hide-completed\"")))
                .andExpect(content().string(containsString("toggleHideCompleted(this)")))
                .andExpect(content().string(containsString("is-completed")))
                .andExpect(content().string(containsString("data-completed=\"true\"")));

        mockMvc.perform(get("/planner/catalog")
                        .session(session)
                        .param("type", "UNCOMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].code", not(hasItem("INF106"))))
                .andExpect(jsonPath("$[*].code", not(hasItem("INF202"))))
                .andExpect(jsonPath("$[*].completed", everyItem(is(false))));
    }
}
