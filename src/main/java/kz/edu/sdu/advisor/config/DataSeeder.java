package kz.edu.sdu.advisor.config;

import kz.edu.sdu.advisor.model.Course;
import kz.edu.sdu.advisor.model.Student;
import kz.edu.sdu.advisor.repository.CourseRepository;
import kz.edu.sdu.advisor.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final CourseRepository courseRepository;
    private final StudentRepository studentRepository;

    @Override
    public void run(String... args) throws Exception {
        if (courseRepository.count() == 0) {
            log.info("Populating initial test courses...");

            Course cs101 = new Course();
            cs101.setCode("CS101");
            cs101.setName("Introduction to Computer Science");
            cs101.setCredits(3);

            Course math101 = new Course();
            math101.setCode("MATH101");
            math101.setName("Calculus I");
            math101.setCredits(4);

            courseRepository.save(cs101);
            courseRepository.save(math101);

            log.info("Initial test courses seeded.");
        } else {
            log.info("Database already contains courses. Skipping seeding.");
        }

        if (studentRepository.count() == 0) {
            log.info("Populating initial test students...");

            Student student = new Student();
            student.setStudentId("240103000");
            student.setName("Jhon Doe");
            student.setEmail("240103000@sdu.edu.kz");
            student.setGpa(3.8);

            studentRepository.save(student);
            log.info("Initial test student seeded: 240103000");
        } else {
            log.info("Database already contains students. Skipping seeding.");
        }
    }
}
