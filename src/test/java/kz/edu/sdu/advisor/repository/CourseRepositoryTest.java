package kz.edu.sdu.advisor.repository;

import kz.edu.sdu.advisor.model.Course;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.orm.jpa.JpaSystemException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class CourseRepositoryTest {

    @Autowired
    private CourseRepository courseRepository;

    @Test
    public void savingCourse_MissingMandatoryField_ThrowsException() {
        Course course = new Course();
        // course.setCode("CS102"); // intentionally missing code
        course.setName("Data Structures");
        course.setCredits(4);

        assertThatThrownBy(() -> {
            courseRepository.saveAndFlush(course);
        }).isInstanceOf(JpaSystemException.class);
    }
}
