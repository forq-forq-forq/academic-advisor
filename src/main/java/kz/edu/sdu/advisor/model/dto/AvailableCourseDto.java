package kz.edu.sdu.advisor.model.dto;

import java.util.List;

/**
 * Representation of a course available in the course planner catalog,
 * enriched with prerequisite verification, curriculum type/category, season,
 * difficulty, and completion/cart status for the active student (US-11, US-12, US-16).
 */
public record AvailableCourseDto(
        Long id,
        String code,
        String name,
        Integer credits,
        Integer difficulty,
        String description,
        String courseType,
        String category,
        String season,
        Integer recommendedSemester,
        boolean completed,
        boolean enrolled,
        boolean inCart,
        List<String> prerequisiteCodes,
        boolean hasPrerequisites,
        boolean prerequisitesMet,
        List<String> missingPrerequisiteCodes,
        String missingSummary
) {
    public AvailableCourseDto(
            Long id,
            String code,
            String name,
            Integer credits,
            Integer difficulty,
            String description,
            String courseType,
            Integer recommendedSemester,
            boolean completed,
            boolean enrolled,
            boolean inCart,
            List<String> prerequisiteCodes,
            boolean hasPrerequisites,
            boolean prerequisitesMet,
            List<String> missingPrerequisiteCodes,
            String missingSummary
    ) {
        this(
                id,
                code,
                name,
                credits,
                difficulty,
                description,
                courseType,
                courseType,
                "BOTH",
                recommendedSemester,
                completed,
                enrolled,
                inCart,
                prerequisiteCodes,
                hasPrerequisites,
                prerequisitesMet,
                missingPrerequisiteCodes,
                missingSummary
        );
    }

    public AvailableCourseDto(
            Long id,
            String code,
            String name,
            Integer credits,
            List<String> prerequisiteCodes,
            boolean hasPrerequisites,
            boolean prerequisitesMet,
            List<String> missingPrerequisiteCodes,
            String missingSummary
    ) {
        this(
                id,
                code,
                name,
                credits,
                3,
                null,
                "CORE",
                "CORE",
                "BOTH",
                null,
                false,
                false,
                false,
                prerequisiteCodes,
                hasPrerequisites,
                prerequisitesMet,
                missingPrerequisiteCodes,
                missingSummary
        );
    }
}
