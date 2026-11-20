package kz.edu.sdu.advisor.model.dto;

import java.util.List;

/**
 * Represents one semester column (1..8) on the multi-year degree roadmap board (US-18..21).
 */
public record RoadmapSemesterDto(
        int semester,
        String title,
        String season,
        int totalCredits,
        String workloadState,
        boolean overloaded,
        int prerequisiteConflictCount,
        int seasonConflictCount,
        List<RoadmapCourseCardDto> courses
) {
}
