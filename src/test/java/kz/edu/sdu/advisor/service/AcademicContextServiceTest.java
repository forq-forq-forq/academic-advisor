package kz.edu.sdu.advisor.service;

import kz.edu.sdu.advisor.model.Course;
import kz.edu.sdu.advisor.model.Student;
import kz.edu.sdu.advisor.repository.CourseRepository;
import kz.edu.sdu.advisor.repository.StudentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AcademicContextServiceTest {

    @Autowired
    private AcademicContextService academicContextService;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CartService cartService;

    @Autowired
    private TranscriptService transcriptService;

    @Test
    @DisplayName("US-41: buildContextualPrompt includes student profile, completed courses, enrolled courses, cart items, and eligible courses")
    void buildContextualPrompt_includesFullStudentContext() {
        Course cs101 = courseRepository.findByCode("CS101").orElseThrow();
        Course cs204 = courseRepository.findByCode("CS204").orElseThrow();

        transcriptService.saveCompletion("240103000", cs101.getId(), "A", "Fall 2024");
        transcriptService.updateAcademicProfile("240103000", 3, "Backend Software Engineer");
        cartService.addCourse("240103000", cs204.getId());

        Student student = studentRepository.findByStudentId("240103000").orElseThrow();
        String prompt = academicContextService.buildContextualPrompt(
                student, "What courses should I take next?", "/planner");

        assertTrue(prompt.contains("John Doe (ID: 240103000)"));
        assertTrue(prompt.contains("Major: Computer Science"));
        assertTrue(prompt.contains("Career Goal / Track: Backend Software Engineer"));
        assertTrue(prompt.contains("Cumulative GPA: 4.00 (Standing: Honor Standing)"));
        assertTrue(prompt.contains("Student's Active Page in App: /planner"));
        assertTrue(prompt.contains("CS101 (Introduction to Computer Science, 3 ECTS): Grade=A, Semester=Fall 2024"));
        assertTrue(prompt.contains("CS201 (Data Structures and Algorithms, 4 ECTS)"));
        assertTrue(prompt.contains("CS204 (Database Systems, 4 ECTS)"));
        assertTrue(prompt.contains("What courses should I take next?"));
    }
}

