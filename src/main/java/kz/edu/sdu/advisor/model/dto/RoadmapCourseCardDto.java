package kz.edu.sdu.advisor.model.dto;

import java.util.List;

/**
 * Represents a single course card placed in a semester column of the 8-semester roadmap (US-18..21).
 */
public record RoadmapCourseCardDto(
        Long courseId,
        String code,
        String name,
        int credits,
        int difficulty,
        String courseType,
        String category,
        String season,
        String status,
        int semester,
        List<String> prerequisiteCodes,
        boolean prerequisiteViolation,
        List<String> violatedPrerequisites,
        boolean seasonMismatch,
        String seasonWarning
) {
}
