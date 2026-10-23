package kz.edu.sdu.advisor.service;

import kz.edu.sdu.advisor.model.dto.CompletedCourseDto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Calculates GPA from the same current completion records displayed by US-29. */
@Service
public class CumulativeGpaService {

    // SDU Student Handbook 2026-2027, p. 28. See docs/US-31.md for sources.
    // Hundredths preserve the published decimal points exactly; FX and AW are zero.
    private static final Map<String, Integer> POINTS = Map.ofEntries(
            Map.entry("A", 400), Map.entry("A-", 367),
            Map.entry("B+", 333), Map.entry("B", 300), Map.entry("B-", 267),
            Map.entry("C+", 233), Map.entry("C", 200), Map.entry("C-", 167),
            Map.entry("D+", 133), Map.entry("D", 100),
            Map.entry("F", 0), Map.entry("FX", 0), Map.entry("AW", 0));

    /** Returns null when no eligible graded credits exist, rather than a misleading 0.00. */
    public Double calculate(List<CompletedCourseDto> completedCourses) {
        if (completedCourses == null || completedCourses.isEmpty()) {
            return null;
        }
        BigDecimal qualityPoints = BigDecimal.ZERO;
        BigDecimal gradedCredits = BigDecimal.ZERO;

        for (CompletedCourseDto course : completedCourses) {
            if (course == null || course.credits() == null || course.credits() <= 0
                    || course.letterGrade() == null) {
                continue;
            }
            Integer points = POINTS.get(course.letterGrade().trim().toUpperCase(Locale.ROOT));
            // Includes the policy's non-GPA codes, missing metadata and unsupported input.
            if (points == null) {
                continue;
            }
            BigDecimal credits = BigDecimal.valueOf(course.credits());
            qualityPoints = qualityPoints.add(BigDecimal.valueOf(points, 2).multiply(credits));
            gradedCredits = gradedCredits.add(credits);
        }

        return gradedCredits.signum() == 0 ? null
                : qualityPoints.divide(gradedCredits, 2, RoundingMode.HALF_UP).doubleValue();
    }
}

