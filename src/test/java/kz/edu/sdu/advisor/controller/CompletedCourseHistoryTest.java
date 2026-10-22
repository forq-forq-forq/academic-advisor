package kz.edu.sdu.advisor.controller;

import kz.edu.sdu.advisor.model.Student;
import kz.edu.sdu.advisor.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

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

    private MockHttpSession session(String studentId) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("authenticatedStudentId", studentId);
        return session;
    }

    @Test
    void rendersRecordedHistoryAndEscapesSemester() throws Exception {
        Long id = students.findByStudentId("240103000").orElseThrow().getId();
        jdbc.update("update student_courses set letter_grade = ?, semester_taken = ? where student_id = ? and course_id = (select id from courses where code = ?)",
                "A-", "Fall 2024 <script>alert(1)</script>", id, "CS101");

        String html = mvc.perform(get("/dashboard").session(session("240103000")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("completedCourses", hasSize(8)))
                .andExpect(content().string(containsString("Fall 2024 &lt;script&gt;")))
                .andReturn().getResponse().getContentAsString();
        String table = completedTable(html);
        assertTrue(table.matches("(?s).*<td>CS101</td>\\s*<td>Introduction to Computer Science</td>\\s*<td>A-</td>\\s*<td>3</td>.*"));
        assertFalse(table.contains("<script>"));
        assertFalse(table.contains("CS201"));
        assertTrue(table.indexOf("CS101") < table.indexOf("CS102"));
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
                .andExpect(content().string(not(containsString("id=\"completed-courses-table\""))));
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
