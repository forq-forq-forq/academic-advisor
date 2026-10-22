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
import java.util.Optional;
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
        Major csMajor = seedMajor(fens, "CS", "Computer Science");
        Major seMajor = seedMajor(fens, "SE", "Software Engineering");
        Major isMajor = seedMajor(fens, "IS", "Information Systems");

        Map<String, Course> courses = seedCourses();
        seedPrerequisites(courses);
        seedCurriculum(csMajor, courses);
        seedCurriculum(seMajor, courses);
        seedCurriculum(isMajor, courses);
        seedDemoStudent(csMajor, courses);
    }

    private Faculty seedFaculties() {
        return facultyRepository.findByCode("FE&NS").orElseGet(() -> {
            log.info("Seeding faculty: FE&NS");
            Faculty faculty = new Faculty("FE&NS", "Faculty of Engineering and Natural Sciences");
            return facultyRepository.save(faculty);
        });
    }

    private Major seedMajor(Faculty faculty, String code, String name) {
        return majorRepository.findByCode(code).orElseGet(() -> {
            log.info("Seeding major: {}", code);
            Major major = new Major(code, name, faculty, 240);
            return majorRepository.save(major);
        });
    }

    private Map<String, Course> seedCourses() {
        Map<String, Course> map = new HashMap<>();

        Map<String, CourseMeta> catalog = Map.ofEntries(
                Map.entry("CS101", new CourseMeta("Introduction to Computer Science", 3, 2, "Foundational programming concepts, computational thinking, and basic algorithms.")),
                Map.entry("MATH101", new CourseMeta("Calculus I", 4, 3, "Limits, derivatives, integrals, and single-variable calculus applications.")),
                Map.entry("ENG101", new CourseMeta("Academic English I", 3, 1, "Academic reading, essay writing, and university communication skills.")),
                Map.entry("HIST101", new CourseMeta("History of Kazakhstan", 3, 1, "Key periods and milestones in the modern and ancient history of Kazakhstan.")),
                Map.entry("CS102", new CourseMeta("Object-Oriented Programming", 4, 3, "Classes, inheritance, polymorphism, encapsulation, and Java design patterns.")),
                Map.entry("MATH102", new CourseMeta("Calculus II", 4, 4, "Integration techniques, infinite series, and multivariable introduction.")),
                Map.entry("ENG102", new CourseMeta("Academic English II", 3, 2, "Research writing, critical argumentation, and technical presentations.")),
                Map.entry("PHIL101", new CourseMeta("Philosophy", 3, 1, "History of philosophical thought, logic, ethics, and epistemology.")),
                Map.entry("CS201", new CourseMeta("Data Structures and Algorithms", 4, 4, "Trees, graphs, hashing, sorting, and algorithmic complexity analysis.")),
                Map.entry("CS205", new CourseMeta("Discrete Mathematics", 3, 3, "Logic, set theory, combinatorics, graph theory, and proofs for CS.")),
                Map.entry("MATH201", new CourseMeta("Linear Algebra", 3, 3, "Vector spaces, matrices, eigenvalues, and linear transformations.")),
                Map.entry("PHYS101", new CourseMeta("General Physics", 4, 3, "Classical mechanics, thermodynamics, and electromagnetism fundamentals.")),
                Map.entry("CS202", new CourseMeta("Algorithms Design and Analysis", 4, 5, "Dynamic programming, greedy algorithms, graph algorithms, and NP-completeness.")),
                Map.entry("CS204", new CourseMeta("Database Systems", 4, 3, "Relational algebra, SQL, normalization, indexing, and transaction processing.")),
                Map.entry("CS301", new CourseMeta("Operating Systems", 4, 5, "Processes, threads, concurrency, memory management, and file systems.")),
                Map.entry("CS302", new CourseMeta("Software Engineering", 3, 3, "Agile lifecycle, requirements engineering, architecture, testing, and CI/CD.")),
                Map.entry("CS303", new CourseMeta("Computer Networks", 4, 4, "TCP/IP stack, routing protocols, socket programming, and network security.")),
                Map.entry("CS304", new CourseMeta("Artificial Intelligence", 3, 4, "Search agents, knowledge representation, probabilistic reasoning, and ML basics.")),
                Map.entry("CS401", new CourseMeta("Senior Design Project I", 4, 4, "First capstone semester: problem definition, architecture, and prototype.")),
                Map.entry("CS402", new CourseMeta("Cloud Computing", 3, 3, "Virtualization, containers, microservices, and scalable cloud infrastructure.")),
                Map.entry("CS403", new CourseMeta("Senior Design Project II", 4, 4, "Second capstone semester: implementation, testing, and thesis defense.")),
                Map.entry("CS404", new CourseMeta("Professional Practice", 3, 2, "Engineering ethics, technical career preparation, and industry standards.")),
                // Additional Electives & High-ECTS courses to test credit overload (US-14)
                Map.entry("CS310", new CourseMeta("Web Application Development", 5, 3, "Full-stack web engineering with modern backend frameworks and REST APIs.")),
                Map.entry("CS320", new CourseMeta("Mobile Application Development", 5, 3, "Native and cross-platform mobile development, UI lifecycle, and state management.")),
                Map.entry("CS330", new CourseMeta("Cybersecurity Fundamentals", 5, 4, "Cryptography, network defense, vulnerability assessment, and secure coding.")),
                Map.entry("CS340", new CourseMeta("Machine Learning and Data Mining", 5, 5, "Supervised/unsupervised learning, feature engineering, and model evaluation.")),
                Map.entry("CS410", new CourseMeta("Distributed Systems and DevOps", 5, 4, "Consensus protocols, Kubernetes, observability, and automated delivery pipelines.")),
                Map.entry("CS420", new CourseMeta("Deep Learning and Neural Networks", 6, 5, "CNNs, transformers, backprop optimization, and modern generative AI architectures.")),
                Map.entry("CS490", new CourseMeta("Advanced Software Engineering Practicum", 6, 4, "Team-based production software delivery with real-world architectural constraints.")),
                Map.entry("INTERN401", new CourseMeta("Industrial Internship", 6, 2, "Supervised industry placement in a software engineering or data role.")),
                Map.entry("ROBO301", new CourseMeta("Robotics and Embedded Systems", 5, 4, "Microcontrollers, sensors, actuators, and real-time embedded control.")),
                Map.entry("DATA201", new CourseMeta("Big Data Analytics", 5, 3, "Distributed data processing, MapReduce, Spark, and analytical data warehouses."))
        );

        for (Map.Entry<String, CourseMeta> entry : catalog.entrySet()) {
            String code = entry.getKey();
            CourseMeta meta = entry.getValue();
            Course course = courseRepository.findByCode(code).orElseGet(() -> {
                Course c = new Course();
                c.setCode(code);
                c.setName(meta.name());
                c.setCredits(meta.credits());
                c.setDifficulty(meta.difficulty());
                c.setDescription(meta.description());
                return courseRepository.save(c);
            });
            boolean updated = false;
            if (course.getDifficulty() == null) {
                course.setDifficulty(meta.difficulty());
                updated = true;
            }
            if (course.getDescription() == null) {
                course.setDescription(meta.description());
                updated = true;
            }
            if (updated) {
                course = courseRepository.save(course);
            }
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
        addPrereq(courses.get("CS310"), courses.get("CS102"));
        addPrereq(courses.get("CS330"), courses.get("CS101"));
        addPrereq(courses.get("CS340"), courses.get("MATH201"));
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
        log.info("Seeding demo 4-year curriculum for major {} (catalog year 2024)...", major.getCode());
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

        // Electives & Practicums
        addCurriculum(major, courses.get("CS310"), 5, 2024, "ELECTIVE");
        addCurriculum(major, courses.get("CS320"), 5, 2024, "ELECTIVE");
        addCurriculum(major, courses.get("CS330"), 6, 2024, "ELECTIVE");
        addCurriculum(major, courses.get("CS340"), 6, 2024, "ELECTIVE");
        addCurriculum(major, courses.get("CS410"), 7, 2024, "ELECTIVE");
        addCurriculum(major, courses.get("CS420"), 7, 2024, "ELECTIVE");
        addCurriculum(major, courses.get("CS490"), 8, 2024, "ELECTIVE");
        addCurriculum(major, courses.get("INTERN401"), 8, 2024, "ELECTIVE");
        addCurriculum(major, courses.get("ROBO301"), 6, 2024, "ELECTIVE");
        addCurriculum(major, courses.get("DATA201"), 5, 2024, "ELECTIVE");
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

        Optional<Student> existingStudent = studentRepository.findByStudentId(studentId);
        if (existingStudent.isPresent()) {
            Student student = existingStudent.get();
            boolean updated = false;
            if (student.getAccount() == null) {
                student.setAccount(account);
                updated = true;
            }
            if (student.getPasswordHash() == null) {
                student.setPasswordHash(account.getPasswordHash());
                updated = true;
            }
            if (student.getCareerGoal() == null) {
                student.setCareerGoal("Backend & Cloud Engineering");
                updated = true;
            }
            if (updated) {
                studentRepository.save(student);
            }
        } else {
            log.info("Seeding demo student: {}", studentId);
            Student student = new Student();
            student.setStudentId(studentId);
            student.setName("John Doe");
            student.setEmail(email);
            student.setPasswordHash(account.getPasswordHash());
            student.setGpa(3.8);
            student.setMajor(major);
            student.setCatalogYear(2024);
            student.setCurrentSemester(3);
            student.setCareerGoal("Backend & Cloud Engineering");
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

    private record CourseMeta(String name, int credits, int difficulty, String description) {}
}
