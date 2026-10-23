package kz.edu.sdu.advisor.service;

import kz.edu.sdu.advisor.model.Student;
import kz.edu.sdu.advisor.model.dto.CourseRecommendationDto;
import kz.edu.sdu.advisor.repository.StudentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SemesterPlanServiceTest {

    @Autowired
    private SemesterPlanService semesterPlanService;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private TranscriptService transcriptService;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AiService aiService;

    @Test
    @DisplayName("US-43: buildRecommendedPlan returns prerequisite-valid courses within balanced 30 ECTS limit and includes 'Why this course?' reasons")
    void buildRecommendedPlan_returnsBalancedPrerequisiteValidCoursesWithReasons() {
        Student student = studentRepository.findByStudentId("240103000").orElseThrow();

        List<CourseRecommendationDto> plan = semesterPlanService.buildRecommendedPlan(
                student, "Build my next semester");

        assertFalse(plan.isEmpty(), "Should recommend eligible courses for next semester");
        int totalRecommendedCredits = plan.stream().mapToInt(CourseRecommendationDto::credits).sum();
        assertTrue(totalRecommendedCredits <= SemesterPlanService.TARGET_BALANCED_CREDITS,
                "Recommended credits should not exceed 30 ECTS balanced threshold");

        // INF202 (Database Management Systems 1) and INF995 (Fundamentals of Web Development) have satisfied prereqs!
        // CSS217 (Software Architecture and Design Patterns) has prereq CSS215 (enrolled, not completed) -> must NOT be recommended!
        assertTrue(plan.stream().anyMatch(r -> "INF202".equals(r.code()) || "CSS118".equals(r.code())));
        assertFalse(plan.stream().anyMatch(r -> "CSS217".equals(r.code())),
                "Courses with unmet prerequisites must never be recommended");
        assertFalse(plan.stream().anyMatch(r -> "CSS105".equals(r.code())),
                "Already completed courses must not be recommended");
        assertFalse(plan.stream().anyMatch(r -> "CSS215".equals(r.code())),
                "Currently enrolled courses must not be recommended");

        for (CourseRecommendationDto rec : plan) {
            assertNotNull(rec.courseId());
            assertNotNull(rec.reason());
            assertFalse(rec.reason().isBlank());
        }
    }

    @Test
    @DisplayName("US-43: Career-aligned elective query prioritizes electives matching student's careerGoal")
    void buildRecommendedPlan_electivesQueryAlignsWithCareerGoal() {
        transcriptService.updateAcademicProfile("240103000", 4, "Mobile & Go Backend Engineer");
        Student student = studentRepository.findByStudentId("240103000").orElseThrow();

        List<CourseRecommendationDto> plan = semesterPlanService.buildRecommendedPlan(
                student, "What electives fit my career?");

        assertFalse(plan.isEmpty());
        // CSS216 (Mobile Programming) and INF368 (The Go Programming Language) have prereq INF108 (completed)
        assertTrue(plan.stream().anyMatch(r -> "CSS216".equals(r.code()) || "INF368".equals(r.code())),
                "Should recommend career-aligned electives for Mobile & Go Backend Engineer");
    }

    @Test
    @DisplayName("US-43: POST /dashboard/chat and POST /api/chat/plan return structured recommendations alongside reply")
    void chatAndPlanEndpoints_returnStructuredRecommendations() throws Exception {
        when(aiService.sendPrompt(anyString())).thenReturn("Here is your personalized semester plan.");

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("authenticatedStudentId", "240103000");

        mockMvc.perform(post("/dashboard/chat")
                        .session(session)
                        .param("message", "Build my next semester")
                        .param("page", "/planner"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply", is("Here is your personalized semester plan.")))
                .andExpect(jsonPath("$.recommendations", is(not(empty()))))
                .andExpect(jsonPath("$.recommendations[0].code", notNullValue()))
                .andExpect(jsonPath("$.recommendations[0].reason", notNullValue()));

        mockMvc.perform(post("/api/chat/plan")
                        .session(session)
                        .param("focus", "Build my next semester"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply", containsString("balanced semester plan")))
                .andExpect(jsonPath("$.recommendations", is(not(empty()))));
    }

    @Test
    @DisplayName("US-40/US-42: 'Explain this course' and 'Explain INF202' work reliably even when Gemini API fails")
    void explainCoursePrompts_returnDetailedCourseExplanationsWhenExternalAiFails() throws Exception {
        when(aiService.sendPrompt(anyString())).thenThrow(new RuntimeException("Simulated Gemini API key error"));

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("authenticatedStudentId", "240103000");

        mockMvc.perform(post("/dashboard/chat")
                        .session(session)
                        .param("message", "Explain this course")
                        .param("page", "/planner"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply", containsString("CSS215")))
                .andExpect(jsonPath("$.reply", not(containsString("temporarily unavailable"))));

        mockMvc.perform(post("/dashboard/chat")
                        .session(session)
                        .param("message", "Explain INF 202")
                        .param("page", "/planner"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply", containsString("INF202 — Database Management Systems 1")))
                .andExpect(jsonPath("$.reply", containsString("Difficulty:")))
                .andExpect(jsonPath("$.reply", containsString("Prerequisites: CSS105")));
    }
}
