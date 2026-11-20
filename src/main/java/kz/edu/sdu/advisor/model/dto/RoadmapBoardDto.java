package kz.edu.sdu.advisor.model.dto;

import java.util.List;

/**
 * Complete 8-semester roadmap board payload with validation counters and active pacing strategy (US-18..21).
 */
public record RoadmapBoardDto(
        List<RoadmapSemesterDto> semesters,
        int totalCredits,
        int totalCourses,
        int prerequisiteConflictCount,
        int seasonConflictCount,
        int overloadedSemesterCount,
        String activePacingStrategy,
        String pacingSummary
) {
}
