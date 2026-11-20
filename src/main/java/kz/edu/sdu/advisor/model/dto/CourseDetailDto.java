package kz.edu.sdu.advisor.model.dto;

import java.util.List;

/**
 * Rich course detail card payload showing description, credits, difficulty rating,
 * full upstream prerequisite chain, downstream unlocked courses, and season availability (US-17).
 */
public record CourseDetailDto(
        Long id,
        String code,
        String name,
        Integer credits,
        Integer difficulty,
        String difficultyLabel,
        String description,
        String courseType,
        String category,
        Integer recommendedSemester,
        String season,
        List<String> prerequisiteCodes,
        List<String> prerequisiteChain,
        List<String> unlocksCourseCodes,
        boolean completed,
        boolean enrolled,
        boolean inCart,
        boolean prerequisitesMet,
        String missingSummary
) {
}
