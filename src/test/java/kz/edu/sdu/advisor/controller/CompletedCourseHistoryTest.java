package kz.edu.sdu.advisor.controller;

import kz.edu.sdu.advisor.model.Student;
import kz.edu.sdu.advisor.model.dto.CompletedCourseDto;
import kz.edu.sdu.advisor.repository.CourseRepository;
import kz.edu.sdu.advisor.repository.StudentRepository;
import kz.edu.sdu.advisor.service.CartService;
import kz.edu.sdu.advisor.exception.PrerequisiteNotMetException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CompletedCourseHistoryTest {
    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private StudentRepository students;
    @Autowired private CourseRepository courses;
    @Autowired private CartService cart;

    private MockHttpSession session(String studentId) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("authenticatedStudentId", studentId);
        return session;
    }

    @Test
    void rendersRecordedHistoryAndEscapesSemester() throws Exception {
        Long id = students.findByStudentId("240103000").orElseThrow().getId();
        jdbc.update("update student_courses set letter_grade = ?, semester_taken = ? where student_id = ? and course_id = (select id from courses where code = ?)",
                "A-", "Fall 2024 <script>alert(1)</script>", id, "CSS105");

        String html = mvc.perform(get("/dashboard").session(session("240103000")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("completedCourses", hasSize(8)))
                .andExpect(content().string(containsString("Fall 2024 &lt;script&gt;")))
                .andReturn().getResponse().getContentAsString();
        String table = completedTable(html);
        assertTrue(table.matches("(?s).*<td>CSS105</td>\\s*<td>Fundamentals of Programming</td>\\s*<td>A-</td>\\s*<td>5</td>.*"));
        assertFalse(table.contains("<script>"));
        assertFalse(table.contains("CSS215"));
        assertTrue(table.indexOf("CSS105") < table.indexOf("INF106"));
    }

    @Test
    void preservesLegacyCompletionsWithMissingMetadata() throws Exception {
        String html = mvc.perform(get("/dashboard").session(session("240103000")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("completedCourses", hasSize(8)))
                .andReturn().getResponse().getContentAsString();
        assertEquals(16, completedTable(html).split("<td>Not recorded</td>", -1).length - 1);
    }

    @Test
    void emptyStudentCannotSeeOtherStudentsHistory() throws Exception {
        Student empty = new Student();
        empty.setStudentId("us29-empty");
        empty.setName("Empty History");
        students.saveAndFlush(empty);

        mvc.perform(get("/dashboard").session(session("us29-empty")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("completedCourses", empty()))
                .andExpect(content().string(containsString("No completed courses yet.")))
                .andExpect(content().string(not(containsString("aria-label=\"Completed course history\""))))
                .andExpect(content().string(not(containsString("id=\"completed-courses-table\""))));
    }

    @Test
    void sameCourseHasStudentSpecificGradesAndTermsAndIgnoresRequestStudentIds() throws Exception {
        Long demoId = students.findByStudentId("240103000").orElseThrow().getId();
        Long courseId = courses.findByCode("CSS105").orElseThrow().getId();
        Student other = new Student();
        other.setStudentId("us29-other");
        other.setName("Other Student");
        students.saveAndFlush(other);
        jdbc.update("insert into student_courses (student_id, course_id, letter_grade, semester_taken) values (?, ?, ?, ?)",
                other.getId(), courseId, "B+", "Spring 2025");
        jdbc.update("update student_courses set letter_grade = ?, semester_taken = ? where student_id = ? and course_id = ?",
                "A-", "Fall 2024", demoId, courseId);

        mvc.perform(get("/dashboard").session(session("us29-other"))
                        .param("studentId", "240103000").param("student_id", demoId.toString()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("completedCourses", List.of(new CompletedCourseDto(
                        "CSS105", "Fundamentals of Programming", 5, "B+", "Spring 2025"))));
        mvc.perform(get("/dashboard").session(session("240103000")))
                .andExpect(model().attribute("completedCourses", hasItem(new CompletedCourseDto(
                        "CSS105", "Fundamentals of Programming", 5, "A-", "Fall 2024"))));
    }

    @Test
    void unauthenticatedStudentSelectorCannotAccessHistory() throws Exception {
        mvc.perform(get("/dashboard").param("studentId", "240103000"))
                .andExpect(redirectedUrl("/login"))
                .andExpect(content().string(not(containsString("CSS105"))));
    }

    @Test
    void recordedMetadataDoesNotChangeStandingCreditsOrPrerequisites() throws Exception {
        Long id = students.findByStudentId("240103000").orElseThrow().getId();
        jdbc.update("update student_courses set letter_grade = ?, semester_taken = ? where student_id = ?",
                "A", "Fall 2024", id);

        mvc.perform(get("/dashboard").session(session("240103000")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("41 / 240 ECTS completed")))
                .andExpect(content().string(containsString("20 ECTS")))
                .andExpect(content().string(containsString("4.00")))
                .andExpect(content().string(containsString("Honor Standing")));
        Student student = students.findByStudentId("240103000").orElseThrow();
        assertEquals(41, student.getCompletedCredits());
        assertDoesNotThrow(() -> cart.validatePrerequisites(student, courses.findByCode("INF202").orElseThrow()));
        assertThrows(PrerequisiteNotMetException.class,
                () -> cart.validatePrerequisites(student, courses.findByCode("CSS217").orElseThrow()));
    }

    @Test
    void blankGradesAndTermsUseMissingDataFallback() throws Exception {
        Long id = students.findByStudentId("240103000").orElseThrow().getId();
        jdbc.update("update student_courses set letter_grade = ?, semester_taken = ? where student_id = ?",
                "   ", "", id);
        String html = mvc.perform(get("/dashboard").session(session("240103000")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertEquals(16, completedTable(html).split("<td>Not recorded</td>", -1).length - 1);
    }

    @Test
    void staleSessionRedirectsToLogin() throws Exception {
        mvc.perform(get("/dashboard").session(session("does-not-exist")))
                .andExpect(redirectedUrl("/login"));
    }

    private String completedTable(String html) {
        int start = html.indexOf("<table class=\"completed-table\"");
        assertTrue(start >= 0, "Completed course table is present");
        return html.substring(start, html.indexOf("</table>", start));
    }
}
