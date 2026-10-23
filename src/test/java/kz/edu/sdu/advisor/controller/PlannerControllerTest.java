package kz.edu.sdu.advisor.controller;

import kz.edu.sdu.advisor.model.Course;
import kz.edu.sdu.advisor.repository.CartItemRepository;
import kz.edu.sdu.advisor.repository.CourseRepository;
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
}
