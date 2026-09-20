package kz.edu.sdu.advisor.config;

import kz.edu.sdu.advisor.model.Student;
import kz.edu.sdu.advisor.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class DataSeederConsistencyTest {

    @Autowired
    private StudentRepository studentRepository;

    @Test
    void seededDemoStudent_shouldMatchDocumentedIdentity() {
        Student student = studentRepository.findByStudentId("240103000").orElse(null);

        assertThat(student).isNotNull();
        assertThat(student.getName()).isEqualTo("John Doe");
        assertThat(student.getEmail()).isEqualTo("240103000@sdu.edu.kz");
    }
}
