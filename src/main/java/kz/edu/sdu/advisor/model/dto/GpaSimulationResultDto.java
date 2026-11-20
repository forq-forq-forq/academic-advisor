package kz.edu.sdu.advisor.model.dto;

import java.util.List;

/**
 * Result of a projected cumulative GPA simulation across upcoming courses,
 * including scholarship threshold and academic probation risk warnings (US-25, US-26).
 */
public record GpaSimulationResultDto(
        Double currentGpa,
        String formattedCurrentGpa,
        int completedGradedCredits,
        Double semesterGpa,
        String formattedSemesterGpa,
        int simulatedCredits,
        Double projectedCumulativeGpa,
        String formattedProjectedGpa,
        double gpaDelta,
        String formattedGpaDelta,
        String projectedStandingLabel,
        boolean belowScholarshipThreshold,
        boolean hasFailingGrade,
        boolean academicProbationRisk,
        String riskLevel,
        String scholarshipWarningMessage,
        String probationWarningMessage,
        List<GpaSimulationItemDto> items
) {
}
