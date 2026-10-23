package kz.edu.sdu.advisor.service;

import kz.edu.sdu.advisor.exception.PrerequisiteNotMetException;
import kz.edu.sdu.advisor.model.CartItem;
import kz.edu.sdu.advisor.model.Course;
import kz.edu.sdu.advisor.model.Prerequisite;
import kz.edu.sdu.advisor.model.Student;
import kz.edu.sdu.advisor.model.dto.CartDto;
import kz.edu.sdu.advisor.model.dto.CartItemDto;
import kz.edu.sdu.advisor.model.dto.EnrolledCourseDto;
import kz.edu.sdu.advisor.repository.CartItemRepository;
import kz.edu.sdu.advisor.repository.CourseRepository;
import kz.edu.sdu.advisor.repository.PrerequisiteRepository;
import kz.edu.sdu.advisor.repository.StudentRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    public static final int LIGHT_THRESHOLD = 15;
    public static final int OVERLOAD_THRESHOLD = 30;

    private final CartItemRepository cartItemRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final PrerequisiteRepository prerequisiteRepository;

    @Getter
    @Value("${advisor.cart.max-credits:40}")
    private int maxCredits = 40;

    @Transactional(readOnly = true)
    public CartDto getCart(String studentId) {
        List<CartItem> items = cartItemRepository.findByStudent_StudentIdOrderByCourse_CodeAsc(studentId);
        List<CartItemDto> itemDtos = items.stream()
                .map(item -> new CartItemDto(
                        item.getId(),
                        item.getCourse().getId(),
                        item.getCourse().getCode(),
                        item.getCourse().getName(),
                        item.getCourse().getCredits() != null ? item.getCourse().getCredits() : 0
                ))
                .toList();

        int totalCredits = itemDtos.stream().mapToInt(CartItemDto::credits).sum();
        String workloadState = getWorkloadState(totalCredits);
        String workloadColor = getWorkloadColor(totalCredits);

        return new CartDto(itemDtos, totalCredits, maxCredits, workloadState, workloadColor, itemDtos.isEmpty());
    }

    @Transactional(readOnly = true)
    public int getTotalCredits(String studentId) {
        return getCart(studentId).totalCredits();
    }

    @Transactional
    public CartDto addCourse(String studentId, Long courseId) {
        Student student = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + courseId));

        // US-12: Verify that all required prerequisites have been completed
        validatePrerequisites(student, course);

        if (!cartItemRepository.existsByStudent_StudentIdAndCourse_Id(studentId, courseId)) {
            CartItem item = new CartItem(student, course);
            cartItemRepository.save(item);
            log.info("Course {} added to cart for student {}", course.getCode(), studentId);
        } else {
            log.debug("Course {} already in cart for student {}", course.getCode(), studentId);
        }

        return getCart(studentId);
    }

    public void validatePrerequisites(Student student, Course course) {
        List<Prerequisite> prerequisites = prerequisiteRepository.findByCourse_Id(course.getId());
        if (prerequisites.isEmpty()) {
            return;
        }

        Set<Long> satisfiedCourseIds = new HashSet<>();
        if (student.getCompletedCourses() != null) {
            satisfiedCourseIds.addAll(student.getCompletedCourses().stream().map(Course::getId).toList());
        }
        List<CartItem> inCartItems = cartItemRepository.findByStudent_StudentIdOrderByCourse_CodeAsc(student.getStudentId());
        if (inCartItems != null) {
            satisfiedCourseIds.addAll(inCartItems.stream().map(item -> item.getCourse().getId()).toList());
        }

        List<Course> missingPrerequisites = prerequisites.stream()
                .map(Prerequisite::getPrerequisiteCourse)
                .filter(req -> req != null && !satisfiedCourseIds.contains(req.getId()))
                .toList();

        if (!missingPrerequisites.isEmpty()) {
            log.warn("Student {} blocked from adding {}: missing prerequisites {}",
                    student.getStudentId(), course.getCode(),
                    missingPrerequisites.stream().map(Course::getCode).toList());
            throw new PrerequisiteNotMetException(course, missingPrerequisites);
        }
    }

    @Transactional
    public CartDto removeCourse(String studentId, Long courseId) {
        cartItemRepository.deleteByStudent_StudentIdAndCourse_Id(studentId, courseId);
        log.info("Course ID {} removed from cart for student {}", courseId, studentId);

        // Cascade remove any dependent courses in the cart whose prerequisites are no longer satisfied
        studentRepository.findByStudentId(studentId).ifPresent(this::pruneUnsatisfiedCartItems);

        return getCart(studentId);
    }

    private void pruneUnsatisfiedCartItems(Student student) {
        Set<Long> completedIds = student.getCompletedCourses() == null
                ? Set.of()
                : new HashSet<>(student.getCompletedCourses().stream().map(Course::getId).toList());

        boolean removedAny;
        do {
            removedAny = false;
            List<CartItem> currentItems = cartItemRepository.findByStudent_StudentIdOrderByCourse_CodeAsc(student.getStudentId());
            Set<Long> satisfiedIds = new HashSet<>(completedIds);
            satisfiedIds.addAll(currentItems.stream().map(i -> i.getCourse().getId()).toList());

            for (CartItem item : currentItems) {
                List<Prerequisite> prereqs = prerequisiteRepository.findByCourse_Id(item.getCourse().getId());
                boolean missingPrereq = prereqs.stream()
                        .map(Prerequisite::getPrerequisiteCourse)
                        .anyMatch(req -> req != null && !satisfiedIds.contains(req.getId()));
                if (missingPrereq) {
                    cartItemRepository.delete(item);
                    log.info("Cascade-removed dependent course {} from cart for student {} due to missing prerequisite",
                            item.getCourse().getCode(), student.getStudentId());
                    removedAny = true;
                }
            }
        } while (removedAny);
    }

    public String getWorkloadState(int credits) {
        if (credits < LIGHT_THRESHOLD) {
            return "Light";
        } else if (credits < OVERLOAD_THRESHOLD) {
            return "Balanced";
        } else {
            return "Overload";
        }
    }

    public String getWorkloadColor(int credits) {
        if (credits < LIGHT_THRESHOLD) {
            return "green";
        } else if (credits < OVERLOAD_THRESHOLD) {
            return "yellow";
        } else {
            return "red";
        }
    }

    @Transactional(readOnly = true)
    public List<EnrolledCourseDto> getEnrolledCourses(String studentId) {
        Student student = studentRepository.findByStudentId(studentId).orElse(null);
        if (student == null || student.getEnrolledCourses() == null) {
            return List.of();
        }
        return student.getEnrolledCourses().stream()
                .filter(Objects::nonNull)
                .map(EnrolledCourseDto::from)
                .sorted(Comparator.comparing(EnrolledCourseDto::code))
                .toList();
    }

    @Transactional(readOnly = true)
    public int getEnrolledCredits(String studentId) {
        return getEnrolledCourses(studentId).stream().mapToInt(EnrolledCourseDto::credits).sum();
    }

    /**
     * Confirms/enrolls all courses currently in the student's Semester Cart so they become
     * Currently Enrolled Courses on the Dashboard, then clears the cart.
     */
    @Transactional
    public List<EnrolledCourseDto> enrollCartCourses(String studentId) {
        Student student = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));

        List<CartItem> items = cartItemRepository.findByStudent_StudentIdOrderByCourse_CodeAsc(studentId);
        if (items.isEmpty()) {
            throw new IllegalArgumentException("Your cart is empty. Add courses before confirming enrollment.");
        }

        if (student.getEnrolledCourses() == null) {
            student.setEnrolledCourses(new HashSet<>());
        }
        for (CartItem item : items) {
            if (item.getCourse() != null) {
                student.getEnrolledCourses().add(item.getCourse());
            }
        }
        studentRepository.save(student);
        cartItemRepository.deleteAll(items);
        log.info("Enrolled {} course(s) from cart for student {}", items.size(), studentId);
        return getEnrolledCourses(studentId);
    }

    /**
     * Cancels/unenrolls a single currently enrolled course for the student.
     */
    @Transactional
    public List<EnrolledCourseDto> cancelEnrolledCourse(String studentId, Long courseId) {
        Student student = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));

        if (student.getEnrolledCourses() != null) {
            student.getEnrolledCourses().removeIf(c -> c != null && Objects.equals(c.getId(), courseId));
            studentRepository.save(student);
            log.info("Cancelled enrolled course ID {} for student {}", courseId, studentId);
        }
        return getEnrolledCourses(studentId);
    }

    /**
     * Moves all currently enrolled courses back into the student's Semester Cart so the student
     * can modify their selection at any time.
     */
    @Transactional
    public CartDto returnEnrolledToCart(String studentId) {
        Student student = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));

        if (student.getEnrolledCourses() != null && !student.getEnrolledCourses().isEmpty()) {
            for (Course course : new HashSet<>(student.getEnrolledCourses())) {
                if (course != null && !cartItemRepository.existsByStudent_StudentIdAndCourse_Id(studentId, course.getId())) {
                    cartItemRepository.save(new CartItem(student, course));
                }
            }
            student.getEnrolledCourses().clear();
            studentRepository.save(student);
            log.info("Returned enrolled courses to cart for student {}", studentId);
        }
        return getCart(studentId);
    }
}
