package kz.edu.sdu.advisor.model.dto;

/**
 * View model representing the primary recommended next action card on the dashboard (US-10).
 */
public record RecommendedActionDto(
        String headline,
        String description,
        String ctaLabel,
        String ctaUrl,
        String category
) {
}
