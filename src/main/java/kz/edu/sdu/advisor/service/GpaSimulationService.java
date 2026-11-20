package kz.edu.sdu.advisor.service;

import kz.edu.sdu.advisor.model.Course;
import kz.edu.sdu.advisor.model.Student;
import kz.edu.sdu.advisor.model.dto.CartDto;
import kz.edu.sdu.advisor.model.dto.CartItemDto;
import kz.edu.sdu.advisor.model.dto.CompletedCourseDto;
import kz.edu.sdu.advisor.model.dto.GpaSimulationItemDto;
import kz.edu.sdu.advisor.model.dto.GpaSimulationResultDto;
import kz.edu.sdu.advisor.repository.CourseCompletionRepository;
import kz.edu.sdu.advisor.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Simulates projected semester and cumulative GPA based on expected letter grades
 * for courses in the student's Semester Cart or currently enrolled courses (US-25).
 */
@Service
@RequiredArgsConstructor
public class GpaSimulationService {

    public static final String DEFAULT_EXPECTED_GRADE = "B+";

    public static final List<String> GPA_LETTER_GRADES = List.of(
            "A", "A-", "B+", "B", "B-", "C+", "C", "C-", "D+", "D", "F", "FX", "AW"
    );

    // SDU Student Handbook 2026-2027 grade point scale (scaled by 100 for exact arithmetic)
    private static final Map<String, Integer> GRADE_POINTS_X100 = Map.ofEntries(
            Map.entry("A", 400), Map.entry("A-", 367),
            Map.entry("B+", 333), Map.entry("B", 300), Map.entry("B-", 267),
            Map.entry("C+", 233), Map.entry("C", 200), Map.entry("C-", 167),
            Map.entry("D+", 133), Map.entry("D", 100),
            Map.entry("F", 0), Map.entry("FX", 0), Map.entry("AW", 0)
    );

    private final StudentRepository studentRepository;
    private final CourseCompletionRepository courseCompletionRepository;
    private final CartService cartService;

    @Transactional(readOnly = true)
    public GpaSimulationResultDto simulate(String studentId, Map<String, String> gradesByCourseKey) {
        Student student = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));

        List<CompletedCourseDto> completedCourses = courseCompletionRepository.findForStudent(student.getId())
                .stream()
                .map(CompletedCourseDto::of)
                .toList();

        BaselineTotals baseline = computeBaseline(student, completedCourses);
        List<SimulatedTarget> targets = resolveSimulationTargets(student);

        Map<String, String> safeGrades = gradesByCourseKey != null ? gradesByCourseKey : Map.of();
        List<GpaSimulationItemDto> items = new ArrayList<>();
        BigDecimal semesterQualityPoints = BigDecimal.ZERO;
        int simulatedCredits = 0;

        for (SimulatedTarget target : targets) {
            String rawGrade = lookupExpectedGrade(safeGrades, target.courseId(), target.code());
            String grade = normalizeAndValidateGpaGrade(rawGrade);
            int pointsX100 = GRADE_POINTS_X100.get(grade);
            double gradePoints = BigDecimal.valueOf(pointsX100, 2).doubleValue();

            if (target.credits() > 0) {
                semesterQualityPoints = semesterQualityPoints.add(
                        BigDecimal.valueOf(pointsX100, 2).multiply(BigDecimal.valueOf(target.credits())));
                simulatedCredits += target.credits();
            }

            items.add(new GpaSimulationItemDto(
                    target.courseId(),
                    target.code(),
                    target.name(),
                    target.credits(),
                    grade,
                    gradePoints,
                    target.source()
            ));
        }

        Double semesterGpa = simulatedCredits > 0
                ? semesterQualityPoints.divide(BigDecimal.valueOf(simulatedCredits), 2, RoundingMode.HALF_UP).doubleValue()
                : null;

        int totalCredits = baseline.gradedCredits() + simulatedCredits;
        Double projectedCumulativeGpa;
        if (totalCredits > 0) {
            BigDecimal totalQualityPoints = baseline.qualityPoints().add(semesterQualityPoints);
            projectedCumulativeGpa = totalQualityPoints
                    .divide(BigDecimal.valueOf(totalCredits), 2, RoundingMode.HALF_UP)
                    .doubleValue();
        } else {
            projectedCumulativeGpa = baseline.currentGpa();
        }

        double delta = (baseline.currentGpa() != null && projectedCumulativeGpa != null)
                ? BigDecimal.valueOf(projectedCumulativeGpa - baseline.currentGpa())
                        .setScale(2, RoundingMode.HALF_UP)
                        .doubleValue()
                : 0.0;

        String formattedCurrent = formatGpa(baseline.currentGpa());
        String formattedSemester = formatGpa(semesterGpa);
        String formattedProjected = formatGpa(projectedCumulativeGpa);
        String formattedDelta = (delta > 0 ? "+" : "") + String.format(Locale.US, "%.2f", delta);
        String standingLabel = resolveStandingLabel(projectedCumulativeGpa);

        return new GpaSimulationResultDto(
                baseline.currentGpa(),
                formattedCurrent,
                baseline.gradedCredits(),
                semesterGpa,
                formattedSemester,
                simulatedCredits,
                projectedCumulativeGpa,
                formattedProjected,
                delta,
                formattedDelta,
                standingLabel,
                items
        );
    }

    private BaselineTotals computeBaseline(Student student, List<CompletedCourseDto> completedCourses) {
        BigDecimal qualityPoints = BigDecimal.ZERO;
        int gradedCredits = 0;

        for (CompletedCourseDto c : completedCourses) {
            if (c == null || c.credits() == null || c.credits() <= 0 || c.letterGrade() == null) {
                continue;
            }
            Integer pts = GRADE_POINTS_X100.get(c.letterGrade().trim().toUpperCase(Locale.ROOT));
            if (pts == null) {
                continue;
            }
            qualityPoints = qualityPoints.add(BigDecimal.valueOf(pts, 2).multiply(BigDecimal.valueOf(c.credits())));
            gradedCredits += c.credits();
        }

        if (gradedCredits > 0) {
            double gpa = qualityPoints.divide(BigDecimal.valueOf(gradedCredits), 2, RoundingMode.HALF_UP).doubleValue();
            return new BaselineTotals(gpa, gradedCredits, qualityPoints);
        }

        if (student.getGpa() != null && student.getCompletedCredits() > 0) {
            int credits = student.getCompletedCredits();
            BigDecimal qp = BigDecimal.valueOf(student.getGpa()).multiply(BigDecimal.valueOf(credits));
            return new BaselineTotals(student.getGpa(), credits, qp);
        }

        return new BaselineTotals(null, 0, BigDecimal.ZERO);
    }

    private List<SimulatedTarget> resolveSimulationTargets(Student student) {
        CartDto cart = cartService.getCart(student.getStudentId());
        if (cart.items() != null && !cart.items().isEmpty()) {
            return cart.items().stream()
                    .map(item -> new SimulatedTarget(
                            item.courseId(),
                            item.code(),
                            item.name(),
                            item.credits(),
                            "CART"))
                    .toList();
        }
        if (student.getEnrolledCourses() != null && !student.getEnrolledCourses().isEmpty()) {
            return student.getEnrolledCourses().stream()
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(Course::getCode))
                    .map(c -> new SimulatedTarget(
                            c.getId(),
                            c.getCode(),
                            c.getName(),
                            c.getCredits() != null ? c.getCredits() : 0,
                            "ENROLLED"))
                    .toList();
        }
        return List.of();
    }

    private String lookupExpectedGrade(Map<String, String> map, Long courseId, String code) {
        Map<String, String> candidateLookup = new LinkedHashMap<>(map);
        String[] keys = new String[]{
                "grade_" + courseId,
                String.valueOf(courseId),
                "grade_" + code,
                code
        };
        for (String key : keys) {
            String value = candidateLookup.get(key);
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return DEFAULT_EXPECTED_GRADE;
    }

    private String normalizeAndValidateGpaGrade(String rawGrade) {
        if (!StringUtils.hasText(rawGrade)) {
            return DEFAULT_EXPECTED_GRADE;
        }
        String normalized = rawGrade.trim().toUpperCase(Locale.ROOT);
        if (!GRADE_POINTS_X100.containsKey(normalized)) {
            throw new IllegalArgumentException("Unsupported GPA letter grade: " + rawGrade);
        }
        return normalized;
    }

    private String formatGpa(Double gpa) {
        return gpa != null ? String.format(Locale.US, "%.2f", gpa) : "N/A";
    }

    private String resolveStandingLabel(Double gpa) {
        if (gpa == null) {
            return "Not Available";
        }
        if (gpa >= 3.50) {
            return "Honor Standing";
        }
        if (gpa >= 2.67) {
            return "Good Standing";
        }
        if (gpa >= 2.00) {
            return "Satisfactory";
        }
        return "Academic Probation Risk";
    }

    private record BaselineTotals(Double currentGpa, int gradedCredits, BigDecimal qualityPoints) {}

    private record SimulatedTarget(Long courseId, String code, String name, int credits, String source) {}
}
