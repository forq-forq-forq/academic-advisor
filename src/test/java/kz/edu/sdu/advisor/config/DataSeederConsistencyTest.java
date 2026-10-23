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

        // US-02 support: major (IS), catalog year, and current semester
        assertThat(student.getMajor()).isNotNull();
        assertThat(student.getMajor().getCode()).isEqualTo("IS");
        assertThat(student.getMajor().getName()).isEqualTo("Information Systems");
        assertThat(student.getMajor().getFaculty().getCode()).isEqualTo("SITAM");
        assertThat(student.getCatalogYear()).isEqualTo(2024);
        assertThat(student.getCurrentSemester()).isEqualTo(3);

        // US-07 support: completed credits calculation (8 courses = 41 ECTS)
        assertThat(student.getCompletedCourses()).isNotEmpty();
        assertThat(student.getCompletedCredits()).isEqualTo(41);

        // US-08 support: currently enrolled courses for this semester (4 courses = 20 ECTS)
        assertThat(student.getEnrolledCourses()).isNotEmpty();
        assertThat(student.getEnrolledCredits()).isEqualTo(20);
    }

    @Test
    void seededAcademicStructure_shouldProvideSitamAndInformationSystemsCurriculum() {
        Faculty faculty = facultyRepository.findByCode("SITAM").orElse(null);
        assertThat(faculty).isNotNull();
        assertThat(faculty.getName()).isEqualTo("SITAM");

        Major major = majorRepository.findByCode("IS").orElse(null);
        assertThat(major).isNotNull();
        assertThat(major.getName()).isEqualTo("Information Systems");
        assertThat(major.getFaculty().getCode()).isEqualTo("SITAM");
        assertThat(major.getTotalCredits()).isEqualTo(240);

        List<CurriculumCourse> curriculum = curriculumCourseRepository
                .findByMajor_CodeAndCatalogYearOrderBySemesterAsc("IS", 2024);
        assertThat(curriculum).hasSize(98);
        assertThat(curriculum).anyMatch(c -> c.getSemester() == 1 && c.getCourse().getCode().equals("INF106"));
        assertThat(curriculum).anyMatch(c -> c.getSemester() == 1 && c.getCourse().getCode().equals("CSS105"));
        assertThat(curriculum).anyMatch(c -> c.getSemester() == 2 && c.getCourse().getCode().equals("INF108"));
        assertThat(curriculum).anyMatch(c -> c.getSemester() == 3 && c.getCourse().getCode().equals("CSS215"));
        assertThat(curriculum.stream().map(CurriculumCourse::getSemester).distinct().sorted().toList())
                .containsExactly(1, 2, 3, 4, 5, 6, 7, 8);
    }
}
