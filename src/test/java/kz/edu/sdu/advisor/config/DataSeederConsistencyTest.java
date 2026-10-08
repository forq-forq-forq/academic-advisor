package kz.edu.sdu.advisor.config;

import kz.edu.sdu.advisor.model.CurriculumCourse;
import kz.edu.sdu.advisor.model.Faculty;
import kz.edu.sdu.advisor.model.Major;
import kz.edu.sdu.advisor.model.Student;
import kz.edu.sdu.advisor.repository.CurriculumCourseRepository;
import kz.edu.sdu.advisor.repository.FacultyRepository;
import kz.edu.sdu.advisor.repository.MajorRepository;
import kz.edu.sdu.advisor.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class DataSeederConsistencyTest {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private FacultyRepository facultyRepository;

    @Autowired
    private MajorRepository majorRepository;

    @Autowired
    private CurriculumCourseRepository curriculumCourseRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @Transactional
    void seededDemoStudent_shouldMatchDocumentedIdentityAndSupportUserStories() {
        Student student = studentRepository.findByStudentId("240103000").orElse(null);

        assertThat(student).isNotNull();
        assertThat(student.getName()).isEqualTo("John Doe");
        assertThat(student.getEmail()).isEqualTo("240103000@sdu.edu.kz");
        assertThat(student.getGpa()).isEqualTo(3.8);

        // US-01 & US-04 support: linked account with hashed password
        assertThat(student.getAccount()).isNotNull();
        assertThat(student.getAccount().getStudentId()).isEqualTo("240103000");
        assertThat(student.getAccount().getEmail()).isEqualTo("240103000@sdu.edu.kz");
        assertThat(passwordEncoder.matches("Student123!@#", student.getAccount().getPasswordHash())).isTrue();

        // US-02 support: major, catalog year, and current semester
        assertThat(student.getMajor()).isNotNull();
        assertThat(student.getMajor().getCode()).isEqualTo("CS");
        assertThat(student.getCatalogYear()).isEqualTo(2024);
        assertThat(student.getCurrentSemester()).isEqualTo(3);

        // US-07 support: completed credits calculation
        assertThat(student.getCompletedCourses()).isNotEmpty();
        assertThat(student.getCompletedCredits()).isEqualTo(27);

        // US-08 support: currently enrolled courses for this semester
        assertThat(student.getEnrolledCourses()).isNotEmpty();
        assertThat(student.getEnrolledCredits()).isEqualTo(14);
    }

    @Test
    void seededAcademicStructure_shouldProvideCurriculumAndFaculty() {
        Faculty faculty = facultyRepository.findByCode("FE&NS").orElse(null);
        assertThat(faculty).isNotNull();
        assertThat(faculty.getName()).isEqualTo("Faculty of Engineering and Natural Sciences");

        Major major = majorRepository.findByCode("CS").orElse(null);
        assertThat(major).isNotNull();
        assertThat(major.getFaculty().getCode()).isEqualTo("FE&NS");
        assertThat(major.getTotalCredits()).isEqualTo(240);

        List<CurriculumCourse> curriculum = curriculumCourseRepository
                .findByMajor_CodeAndCatalogYearOrderBySemesterAsc("CS", 2024);
        assertThat(curriculum).isNotEmpty();
        assertThat(curriculum).anyMatch(c -> c.getSemester() == 1 && c.getCourse().getCode().equals("CS101"));
        assertThat(curriculum).anyMatch(c -> c.getSemester() == 2 && c.getCourse().getCode().equals("CS102"));
        assertThat(curriculum).anyMatch(c -> c.getSemester() == 3 && c.getCourse().getCode().equals("CS201"));
    }
}
