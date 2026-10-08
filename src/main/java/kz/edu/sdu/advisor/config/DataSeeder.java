package kz.edu.sdu.advisor.config;

import kz.edu.sdu.advisor.model.*;
import kz.edu.sdu.advisor.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final CourseRepository courseRepository;
    private final PrerequisiteRepository prerequisiteRepository;
    private final FacultyRepository facultyRepository;
    private final MajorRepository majorRepository;
    private final CurriculumCourseRepository curriculumCourseRepository;
    private final StudentAccountRepository studentAccountRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        Faculty fens = seedFaculties();
        Major csMajor = seedMajors(fens);
        Map<String, Course> courses = seedCourses();
        seedPrerequisites(courses);
        seedCurriculum(csMajor, courses);
        seedDemoStudent(csMajor, courses);
    }

    private Faculty seedFaculties() {
        return facultyRepository.findByCode("FE&NS").orElseGet(() -> {
            log.info("Seeding faculty: FE&NS");
            Faculty faculty = new Faculty("FE&NS", "Faculty of Engineering and Natural Sciences");
            return facultyRepository.save(faculty);
        });
    }

    private Major seedMajors(Faculty faculty) {
        return majorRepository.findByCode("CS").orElseGet(() -> {
            log.info("Seeding major: CS");
            Major major = new Major("CS", "Computer Science", faculty, 240);
            return majorRepository.save(major);
        });
    }

    private Map<String, Course> seedCourses() {
        Map<String, Course> map = new HashMap<>();

        Map<String, CourseMeta> catalog = Map.ofEntries(
                Map.entry("CS101", new CourseMeta("Introduction to Computer Science", 3)),
                Map.entry("MATH101", new CourseMeta("Calculus I", 4)),
                Map.entry("ENG101", new CourseMeta("Academic English I", 3)),
                Map.entry("HIST101", new CourseMeta("History of Kazakhstan", 3)),
                Map.entry("CS102", new CourseMeta("Object-Oriented Programming", 4)),
                Map.entry("MATH102", new CourseMeta("Calculus II", 4)),
                Map.entry("ENG102", new CourseMeta("Academic English II", 3)),
                Map.entry("PHIL101", new CourseMeta("Philosophy", 3)),
                Map.entry("CS201", new CourseMeta("Data Structures and Algorithms", 4)),
                Map.entry("CS205", new CourseMeta("Discrete Mathematics", 3)),
                Map.entry("MATH201", new CourseMeta("Linear Algebra", 3)),
                Map.entry("PHYS101", new CourseMeta("General Physics", 4)),
                Map.entry("CS202", new CourseMeta("Algorithms Design and Analysis", 4)),
                Map.entry("CS204", new CourseMeta("Database Systems", 4)),
                Map.entry("CS301", new CourseMeta("Operating Systems", 4)),
                Map.entry("CS302", new CourseMeta("Software Engineering", 3)),
                Map.entry("CS303", new CourseMeta("Computer Networks", 4)),
                Map.entry("CS304", new CourseMeta("Artificial Intelligence", 3)),
                Map.entry("CS401", new CourseMeta("Senior Design Project I", 4)),
                Map.entry("CS402", new CourseMeta("Cloud Computing", 3)),
                Map.entry("CS403", new CourseMeta("Senior Design Project II", 4)),
                Map.entry("CS404", new CourseMeta("Professional Practice", 3))
        );

        for (Map.Entry<String, CourseMeta> entry : catalog.entrySet()) {
            String code = entry.getKey();
            CourseMeta meta = entry.getValue();
            Course course = courseRepository.findByCode(code).orElseGet(() -> {
                Course c = new Course();
                c.setCode(code);
                c.setName(meta.name());
                c.setCredits(meta.credits());
                return courseRepository.save(c);
            });
            map.put(code, course);
        }

        return map;
    }

    private void seedPrerequisites(Map<String, Course> courses) {
        if (prerequisiteRepository.count() > 0) {
            return;
        }

        addPrereq(courses.get("CS102"), courses.get("CS101"));
        addPrereq(courses.get("MATH102"), courses.get("MATH101"));
        addPrereq(courses.get("ENG102"), courses.get("ENG101"));
        addPrereq(courses.get("CS201"), courses.get("CS102"));
        addPrereq(courses.get("CS205"), courses.get("MATH101"));
        addPrereq(courses.get("MATH201"), courses.get("MATH101"));
        addPrereq(courses.get("CS202"), courses.get("CS201"));
        addPrereq(courses.get("CS204"), courses.get("CS102"));
    }

    private void addPrereq(Course target, Course required) {
        if (target != null && required != null) {
            Prerequisite prereq = new Prerequisite();
            prereq.setCourse(target);
            prereq.setPrerequisiteCourse(required);
            prerequisiteRepository.save(prereq);
        }
    }

    private void seedCurriculum(Major major, Map<String, Course> courses) {
        log.info("Seeding demo 4-year curriculum for major CS (catalog year 2024)...");
        // Semester 1
        addCurriculum(major, courses.get("CS101"), 1, 2024, "CORE");
        addCurriculum(major, courses.get("MATH101"), 1, 2024, "CORE");
        addCurriculum(major, courses.get("ENG101"), 1, 2024, "CORE");
        addCurriculum(major, courses.get("HIST101"), 1, 2024, "GENERAL");

        // Semester 2
        addCurriculum(major, courses.get("CS102"), 2, 2024, "CORE");
        addCurriculum(major, courses.get("MATH102"), 2, 2024, "CORE");
        addCurriculum(major, courses.get("ENG102"), 2, 2024, "CORE");
        addCurriculum(major, courses.get("PHIL101"), 2, 2024, "GENERAL");

        // Semester 3
        addCurriculum(major, courses.get("CS201"), 3, 2024, "CORE");
        addCurriculum(major, courses.get("CS205"), 3, 2024, "CORE");
        addCurriculum(major, courses.get("MATH201"), 3, 2024, "CORE");
        addCurriculum(major, courses.get("PHYS101"), 3, 2024, "GENERAL");

        // Semester 4
        addCurriculum(major, courses.get("CS202"), 4, 2024, "CORE");
        addCurriculum(major, courses.get("CS204"), 4, 2024, "CORE");

        // Semesters 5-8 are demo data until official curricula are imported.
        addCurriculum(major, courses.get("CS301"), 5, 2024, "CORE");
        addCurriculum(major, courses.get("CS302"), 5, 2024, "CORE");
        addCurriculum(major, courses.get("CS303"), 6, 2024, "CORE");
        addCurriculum(major, courses.get("CS304"), 6, 2024, "CORE");
        addCurriculum(major, courses.get("CS401"), 7, 2024, "CORE");
        addCurriculum(major, courses.get("CS402"), 7, 2024, "CORE");
        addCurriculum(major, courses.get("CS403"), 8, 2024, "CORE");
        addCurriculum(major, courses.get("CS404"), 8, 2024, "CORE");
    }

    private void addCurriculum(Major major, Course course, int semester, int catalogYear, String type) {
        if (major != null && course != null
                && !curriculumCourseRepository.existsByMajor_IdAndCourse_IdAndCatalogYear(
                        major.getId(), course.getId(), catalogYear)) {
            curriculumCourseRepository.save(new CurriculumCourse(major, course, semester, catalogYear, type));
        }
    }

    private void seedDemoStudent(Major major, Map<String, Course> courses) {
        String studentId = "240103000";
        String email = "240103000@sdu.edu.kz";

        StudentAccount account = studentAccountRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
            StudentAccount acc = new StudentAccount();
            acc.setEmail(email);
            acc.setStudentId(studentId);
            acc.setPasswordHash(passwordEncoder.encode("Student123!@#"));
            return studentAccountRepository.save(acc);
        });
        if (account.getMajor() == null) {
            account.setMajor(major);
        }
        if (account.getCatalogYear() == null) {
            account.setCatalogYear(2024);
        }
        studentAccountRepository.save(account);

        if (studentRepository.findByStudentId(studentId).isEmpty()) {
            log.info("Seeding demo student: {}", studentId);
            Student student = new Student();
            student.setStudentId(studentId);
            student.setName("John Doe");
            student.setEmail(email);
            student.setGpa(3.8);
            student.setMajor(major);
            student.setCatalogYear(2024);
            student.setCurrentSemester(3);
            student.setAccount(account);

            // Completed courses: Semesters 1 and 2 (Total: 27 ECTS)
            student.setCompletedCourses(Set.of(
                    courses.get("CS101"),
                    courses.get("MATH101"),
                    courses.get("ENG101"),
                    courses.get("HIST101"),
                    courses.get("CS102"),
                    courses.get("MATH102"),
                    courses.get("ENG102"),
                    courses.get("PHIL101")
            ));

            // Currently enrolled courses: Semester 3 (Total: 14 ECTS)
            student.setEnrolledCourses(Set.of(
                    courses.get("CS201"),
                    courses.get("CS205"),
                    courses.get("MATH201"),
                    courses.get("PHYS101")
            ));

            studentRepository.save(student);
            account.setStudent(student);
            log.info("Demo student seeded with major CS, 27 completed ECTS, 14 enrolled ECTS");
        }
    }

    private record CourseMeta(String name, int credits) {}
}
