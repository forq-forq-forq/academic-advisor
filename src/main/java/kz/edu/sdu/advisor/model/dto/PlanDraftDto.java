package kz.edu.sdu.advisor.model.dto;

import java.time.LocalDateTime;

/**
 * Summary representation of a saved named academic plan draft (US-22).
 */
public record PlanDraftDto(
        Long id,
        String name,
        String pacingStrategy,
        int totalCredits,
        int cartCredits,
        int cartCourseCount,
        int roadmapCourseCount,
        String cartCodesSummary,
        LocalDateTime updatedAt
) {
}
