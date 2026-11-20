package kz.edu.sdu.advisor.controller;

import kz.edu.sdu.advisor.model.Course;
import kz.edu.sdu.advisor.model.Student;
import kz.edu.sdu.advisor.repository.CartItemRepository;
import kz.edu.sdu.advisor.repository.CourseRepository;
import kz.edu.sdu.advisor.repository.StudentRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class GpaControllerTest {

    private static final String VALID_STUDENT_ID = "240103000";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Test
    @DisplayName("US-25: Unauthenticated /gpa redirects to /login and /gpa/simulate returns 401")
    void unauthenticatedRequests_shouldBeProtected() throws Exception {
        mockMvc.perform(get("/gpa"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        mockMvc.perform(post("/gpa/simulate"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", containsString("Session expired")));
    }

    @Test
    @DisplayName("US-25: Authenticated GET /gpa renders GPA simulator and POST /gpa/simulate calculates projected GPA")
    void gpaPlannerAndSimulateEndpoint_shouldRenderAndCalculate() throws Exception {
        cartItemRepository.deleteAll();
        Course inf202 = courseRepository.findByCode("INF202").orElseThrow();
        Course inf995 = courseRepository.findByCode("INF995").orElseThrow();

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("authenticatedStudentId", VALID_STUDENT_ID);

        mockMvc.perform(post("/planner/cart/add")
                        .session(session)
                        .param("courseId", inf202.getId().toString()))
                .andExpect(status().isOk());
        mockMvc.perform(post("/planner/cart/add")
                        .session(session)
                        .param("courseId", inf995.getId().toString()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/gpa").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("gpa"))
                .andExpect(model().attributeExists("student", "standing", "simulation", "supportedGrades"))
                .andExpect(content().string(containsString("GPA Planner")))
                .andExpect(content().string(containsString("INF202")));

        mockMvc.perform(post("/gpa/simulate")
                        .session(session)
                        .param("grade_" + inf202.getId(), "A")
                        .param("grade_" + inf995.getId(), "A"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.semesterGpa", is(4.0)))
                .andExpect(jsonPath("$.projectedCumulativeGpa", is(3.84)))
                .andExpect(jsonPath("$.items", hasSize(2)));
    }

    @Test
    @DisplayName("US-26: GET /gpa and POST /gpa/simulate surface scholarship and academic probation warnings")
    void gpaRiskWarnings_shouldAppearWhenThresholdBreached() throws Exception {
        cartItemRepository.deleteAll();
        Course inf202 = courseRepository.findByCode("INF202").orElseThrow();
        Student student = studentRepository.findByStudentId(VALID_STUDENT_ID).orElseThrow();
        student.setGpa(1.90);
        studentRepository.saveAndFlush(student);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("authenticatedStudentId", VALID_STUDENT_ID);

        mockMvc.perform(post("/planner/cart/add")
                        .session(session)
                        .param("courseId", inf202.getId().toString()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/gpa").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"gpa-scholarship-warning\"")))
                .andExpect(content().string(containsString("id=\"gpa-probation-warning\"")));

        mockMvc.perform(post("/gpa/simulate")
                        .session(session)
                        .param("grade_" + inf202.getId(), "F"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.belowScholarshipThreshold", is(true)))
                .andExpect(jsonPath("$.hasFailingGrade", is(true)))
                .andExpect(jsonPath("$.academicProbationRisk", is(true)))
                .andExpect(jsonPath("$.riskLevel", is("PROBATION_DANGER")))
                .andExpect(jsonPath("$.scholarshipWarningMessage", containsString("3.00")))
                .andExpect(jsonPath("$.probationWarningMessage", containsString("2.00")));
    }
}
