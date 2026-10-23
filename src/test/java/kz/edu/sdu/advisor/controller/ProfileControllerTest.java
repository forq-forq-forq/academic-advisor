package kz.edu.sdu.advisor.controller;

import kz.edu.sdu.advisor.model.Course;
import kz.edu.sdu.advisor.model.Student;
import kz.edu.sdu.advisor.model.dto.CompletedCourseDto;
import kz.edu.sdu.advisor.repository.CourseRepository;
import kz.edu.sdu.advisor.repository.StudentRepository;
import kz.edu.sdu.advisor.service.TranscriptService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private TranscriptService transcriptService;

    private MockHttpSession authSession(String studentId) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("authenticatedStudentId", studentId);
        return session;
    }

    @Test
    void unauthenticatedAccessRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/profile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        mockMvc.perform(post("/profile/transcript/save")
                        .param("courseId", "1")
                        .param("letterGrade", "A"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void showProfileRendersCompletedCoursesAndCatalog() throws Exception {
        mockMvc.perform(get("/profile").session(authSession("240103000")))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andExpect(model().attributeExists("student", "standing", "completedCourses", "allCourses", "supportedGrades"))
                .andExpect(model().attribute("completedCourses", hasSize(8)))
                .andExpect(content().string(containsString("Manage Transcript (US-30)")));
    }

    @Test
    void saveTranscriptCourseAddsNewCompletionAndUpdatesExisting() throws Exception {
        Course inf202 = courseRepository.findByCode("INF202").orElseThrow();
        Student student = studentRepository.findByStudentId("240103000").orElseThrow();

        // Add INF202 as completed with grade A- in Spring 2025
        mockMvc.perform(post("/profile/transcript/save")
                        .session(authSession("240103000"))
                        .param("courseId", inf202.getId().toString())
                        .param("letterGrade", "a-")
                        .param("semesterTaken", "Spring 2025"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attribute("transcriptSuccess", containsString("INF202 (A-)")));

        List<CompletedCourseDto> afterAdd = transcriptService.getCompletedCourses(student.getId());
        assertEquals(9, afterAdd.size());
        assertTrue(afterAdd.stream().anyMatch(c ->
                "INF202".equals(c.code()) && "A-".equals(c.letterGrade()) && "Spring 2025".equals(c.semesterTaken())));

        // Update existing INF202 grade to A
        mockMvc.perform(post("/profile/transcript/save")
                        .session(authSession("240103000"))
                        .param("courseId", inf202.getId().toString())
                        .param("letterGrade", "A")
                        .param("semesterTaken", "Fall 2025"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"));

        List<CompletedCourseDto> afterUpdate = transcriptService.getCompletedCourses(student.getId());
        assertEquals(9, afterUpdate.size());
        assertTrue(afterUpdate.stream().anyMatch(c ->
                "INF202".equals(c.code()) && "A".equals(c.letterGrade()) && "Fall 2025".equals(c.semesterTaken())));
    }

    @Test
    void saveTranscriptCourseRejectsInvalidGrade() throws Exception {
        Course css105 = courseRepository.findByCode("CSS105").orElseThrow();

        mockMvc.perform(post("/profile/transcript/save")
                        .session(authSession("240103000"))
                        .param("courseId", css105.getId().toString())
                        .param("letterGrade", "INVALID_GRADE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attribute("transcriptError", containsString("Unsupported letter grade")));
    }

    @Test
    void removeTranscriptCourseDeletesCompletion() throws Exception {
        Course css105 = courseRepository.findByCode("CSS105").orElseThrow();
        Student student = studentRepository.findByStudentId("240103000").orElseThrow();

        mockMvc.perform(post("/profile/transcript/remove")
                        .session(authSession("240103000"))
                        .param("courseId", css105.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attributeExists("transcriptSuccess"));

        List<CompletedCourseDto> remaining = transcriptService.getCompletedCourses(student.getId());
        assertEquals(7, remaining.size());
        assertFalse(remaining.stream().anyMatch(c -> "CSS105".equals(c.code())));
    }

    @Test
    void updateAcademicProfileUpdatesSemesterAndCareerGoal() throws Exception {
        mockMvc.perform(post("/profile/update")
                        .session(authSession("240103000"))
                        .param("currentSemester", "5")
                        .param("careerGoal", "AI / Machine Learning Engineer"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attribute("profileSuccess", containsString("Academic profile updated")));

        Student updated = studentRepository.findByStudentId("240103000").orElseThrow();
        assertEquals(5, updated.getCurrentSemester());
        assertEquals("AI / Machine Learning Engineer", updated.getCareerGoal());
    }
}
