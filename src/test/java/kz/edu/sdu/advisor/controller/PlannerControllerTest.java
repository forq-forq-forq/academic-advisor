package kz.edu.sdu.advisor.controller;

import kz.edu.sdu.advisor.model.Course;
import kz.edu.sdu.advisor.repository.CartItemRepository;
import kz.edu.sdu.advisor.repository.CourseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PlannerControllerTest {

    private static final String VALID_STUDENT_ID = "240103000";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private CourseRepository courseRepository;

    private Course testCourse;

    @BeforeEach
    void setUp() {
        cartItemRepository.deleteAll();
        testCourse = courseRepository.findByCode("CS201").orElseGet(() -> {
            Course c = new Course();
            c.setCode("CS201");
            c.setName("Data Structures");
            c.setCredits(4);
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
    @DisplayName("Authenticated GET /planner should render view with empty cart placeholder")
    void getPlanner_authenticated_shouldRenderEmptyCart() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("authenticatedStudentId", VALID_STUDENT_ID);

        mockMvc.perform(get("/planner").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("planner"))
                .andExpect(model().attributeExists("student", "cart", "availableCourses"))
                .andExpect(content().string(containsString("Your Semester Cart")))
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
}

