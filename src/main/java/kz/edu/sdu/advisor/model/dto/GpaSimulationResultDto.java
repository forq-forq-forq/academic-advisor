package kz.edu.sdu.advisor.model.dto;

import java.util.List;

/**
 * Result of a projected cumulative GPA simulation across upcoming courses (US-25).
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
        List<GpaSimulationItemDto> items
) {
}
