package kz.edu.sdu.advisor.controller;

import jakarta.persistence.EntityManager;
import kz.edu.sdu.advisor.model.Student;
import kz.edu.sdu.advisor.model.dto.AcademicStandingDto;
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

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CumulativeGpaControllerTest {
    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private StudentRepository students;
    @Autowired private EntityManager entityManager;

    @Test
    void recalculatesOnEveryDashboardRequestAfterGradeAdditionUpdateAndRemoval() throws Exception {
        Long id = demoId();
        MockHttpSession session = session("240103000");
        assertNull(standing(session).gpa());

        grade(id, "CS101", "A"); // 3 ECTS
        assertEquals(4.0, standing(session).gpa());
        grade(id, "MATH101", "B"); // 4 ECTS, previously ungraded
        assertEquals(3.43, standing(session).gpa());
        grade(id, "CS101", "C");
        AcademicStandingDto updated = standing(session);
        assertEquals(2.57, updated.gpa());
        assertEquals("Satisfactory", updated.standingLabel());
        grade(id, "MATH101", null);
        assertEquals(2.0, standing(session).gpa());
        grade(id, "CS101", null);
        assertNull(standing(session).gpa());
        // The legacy stored value is neither used as fallback nor overwritten by a GET.
        assertEquals(3.8, jdbc.queryForObject("select gpa from students where id = ?", Double.class, id));
    }

    @Test
    void removingACompletionAlsoRemovesItsContribution() throws Exception {
        Long id = demoId();
        grade(id, "CS101", "A");
        assertEquals(4.0, standing(session("240103000")).gpa());
        jdbc.update("delete from student_courses where student_id = ? and course_id = (select id from courses where code = ?)",
                id, "CS101");
        entityManager.clear();
        assertNull(standing(session("240103000")).gpa());
    }

    @Test
    void isolatesTwoStudentsEvenWhenTheyCompleteTheSameCourse() throws Exception {
        Long id = demoId();
        grade(id, "CS101", "A");
        Student other = new Student();
        other.setStudentId("us31-other");
        other.setName("Other Student");
        other.setGpa(4.0); // Deliberately stale legacy field.
        students.saveAndFlush(other);
        jdbc.update("insert into student_courses (student_id, course_id, letter_grade) select ?, id, ? from courses where code = ?",
                other.getId(), "D", "CS101");
        entityManager.clear();

        AcademicStandingDto actual = (AcademicStandingDto) mvc.perform(get("/dashboard")
                        .session(session("us31-other")).param("studentId", "240103000"))
                .andExpect(status().isOk()).andReturn().getModelAndView().getModel().get("standing");
        assertEquals(1.0, actual.gpa());
        assertEquals("Academic Warning", actual.standingLabel());
        assertEquals(4.0, standing(session("240103000")).gpa());
    }

    @Test
    void ungradedHistoryShowsUnavailableInsteadOfStaleSeededGpa() throws Exception {
        mvc.perform(get("/dashboard").session(session("240103000")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("N/A")))
                .andExpect(content().string(containsString("Not Available")))
                .andExpect(content().string(containsString("27 / 240 ECTS completed")))
                .andExpect(content().string(containsString("14 ECTS")));
        Student empty = new Student();
        empty.setStudentId("us31-empty");
        empty.setName("Empty Student");
        students.saveAndFlush(empty);
        assertNull(standing(session("us31-empty")).gpa());
    }

    @Test
    void excludesEnrolledOnlyCoursesAndZeroCreditCourses() throws Exception {
        Long id = demoId();
        grade(id, "CS101", "A");
        grade(id, "MATH101", "F");
        jdbc.update("update courses set credits = 0 where code = ?", "MATH101");
        entityManager.clear();
        assertEquals(4.0, standing(session("240103000")).gpa());
    }

    @Test
    void rejectsUnauthenticatedAndStaleSessions() throws Exception {
        mvc.perform(get("/dashboard").param("studentId", "240103000"))
                .andExpect(redirectedUrl("/login"));
        mvc.perform(get("/dashboard").session(session("missing-student")))
                .andExpect(redirectedUrl("/login"));
    }

    private Long demoId() {
        return students.findByStudentId("240103000").orElseThrow().getId();
    }

    private void grade(Long studentId, String code, String grade) {
        jdbc.update("update student_courses set letter_grade = ? where student_id = ? and course_id = (select id from courses where code = ?)",
                grade, studentId, code);
        entityManager.clear();
    }

    private MockHttpSession session(String studentId) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("authenticatedStudentId", studentId);
        return session;
    }

    private AcademicStandingDto standing(MockHttpSession session) throws Exception {
        return (AcademicStandingDto) mvc.perform(get("/dashboard").session(session))
                .andExpect(status().isOk()).andReturn().getModelAndView().getModel().get("standing");
    }
}

