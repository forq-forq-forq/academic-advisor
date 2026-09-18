package kz.edu.sdu.advisor.config;

import kz.edu.sdu.advisor.model.Course;
import kz.edu.sdu.advisor.model.Student;
import kz.edu.sdu.advisor.repository.CourseRepository;
import kz.edu.sdu.advisor.repository.StudentRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;

    public DataSeeder(StudentRepository studentRepository,
                      CourseRepository courseRepository) {
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
    }

    @Override
    public void run(String... args) {

        if (studentRepository.count() == 0) {
            Student student = new Student();
            student.setStudentId("250103018");
            student.setName("Test Student");
            student.setEmail("student@sdu.edu.kz");
            student.setGpa(3.5);

            studentRepository.save(student);
        }

        if (courseRepository.count() == 0) {
            Course course1 = new Course();
            course1.setCode("INF101");
            course1.setName("Introduction to Programming");
            course1.setCredits(5);

            Course course2 = new Course();
            course2.setCode("INF202");
            course2.setName("Database Systems");
            course2.setCredits(5);

            courseRepository.save(course1);
            courseRepository.save(course2);
        }
    }
}