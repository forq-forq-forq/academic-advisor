package kz.edu.sdu.advisor.service;

import kz.edu.sdu.advisor.model.Course;
import kz.edu.sdu.advisor.model.CourseCompletion;
import kz.edu.sdu.advisor.model.Student;
import kz.edu.sdu.advisor.model.dto.CompletedCourseDto;
import kz.edu.sdu.advisor.repository.CartItemRepository;
import kz.edu.sdu.advisor.repository.CourseCompletionRepository;
import kz.edu.sdu.advisor.repository.CourseRepository;
import kz.edu.sdu.advisor.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Manages student transcript records (completed courses, letter grades, and semesters taken)
 * for manual transcript entry and updates (US-30).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TranscriptService {

    public static final List<String> SUPPORTED_GRADES = List.of(
            "A", "A-", "B+", "B", "B-", "C+", "C", "C-", "D+", "D", "F", "FX", "AW", "P", "NP", "W", "I", "IP"
    );

    private static final Set<String> VALID_GRADE_SET = Set.copyOf(SUPPORTED_GRADES);

    private final CourseCompletionRepository courseCompletionRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final CartItemRepository cartItemRepository;

    @Transactional(readOnly = true)
    public List<CompletedCourseDto> getCompletedCourses(Long studentDbId) {
        return courseCompletionRepository.findForStudent(studentDbId)
                .stream()
                .map(CompletedCourseDto::of)
                .toList();
    }

    @Transactional
    public CompletedCourseDto saveCompletion(String studentId, Long courseId, String letterGrade, String semesterTaken) {
        Student student = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + courseId));

        String normalizedGrade = normalizeAndValidateGrade(letterGrade);
        String normalizedSemester = normalizeSemester(semesterTaken);

        CourseCompletion.Id completionId = new CourseCompletion.Id(student.getId(), course.getId());
        CourseCompletion completion = courseCompletionRepository.findById(completionId)
                .orElseGet(() -> new CourseCompletion(student.getId(), course, normalizedGrade, normalizedSemester));

        completion.setLetterGrade(normalizedGrade);
        completion.setSemesterTaken(normalizedSemester);
        courseCompletionRepository.saveAndFlush(completion);

        // If the course was in the student's cart, remove it since it is now completed
        if (cartItemRepository.existsByStudent_StudentIdAndCourse_Id(studentId, courseId)) {
            cartItemRepository.deleteByStudent_StudentIdAndCourse_Id(studentId, courseId);
        }

        log.info("Saved transcript record for student {}: course={}, grade={}, semester={}",
                studentId, course.getCode(), normalizedGrade, normalizedSemester);

        return CompletedCourseDto.of(completion);
    }

    @Transactional
    public void removeCompletion(String studentId, Long courseId) {
        Student student = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));

        CourseCompletion.Id completionId = new CourseCompletion.Id(student.getId(), courseId);
        if (courseCompletionRepository.existsById(completionId)) {
            courseCompletionRepository.deleteById(completionId);
            courseCompletionRepository.flush();
            log.info("Removed course ID {} from transcript for student {}", courseId, studentId);
        }
    }

    @Transactional
    public Student updateAcademicProfile(String studentId, Integer currentSemester, String careerGoal) {
        Student student = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));

        if (currentSemester != null && currentSemester >= 1 && currentSemester <= 8) {
            student.setCurrentSemester(currentSemester);
        }
        if (careerGoal != null) {
            String trimmed = careerGoal.trim();
            student.setCareerGoal(trimmed.isEmpty() ? null : (trimmed.length() > 255 ? trimmed.substring(0, 255) : trimmed));
        }
        return studentRepository.save(student);
    }

    public static String normalizeAndValidateGrade(String letterGrade) {
        if (!StringUtils.hasText(letterGrade)) {
            throw new IllegalArgumentException("Letter grade is required.");
        }
        String normalized = letterGrade.trim().toUpperCase(Locale.ROOT);
        if (!VALID_GRADE_SET.contains(normalized)) {
            throw new IllegalArgumentException("Unsupported letter grade: " + letterGrade);
        }
        return normalized;
    }

    public static String normalizeSemester(String semesterTaken) {
        if (!StringUtils.hasText(semesterTaken)) {
            return null;
        }
        String trimmed = semesterTaken.trim();
        return trimmed.length() > 50 ? trimmed.substring(0, 50) : trimmed;
    }
}

