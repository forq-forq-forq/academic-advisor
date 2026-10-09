package kz.edu.sdu.advisor.model.dto;

import java.util.List;

/**
 * Representation of a course available to be added to the semester cart,
 * enriched with prerequisite verification metadata for the active student (US-12).
 */
public record AvailableCourseDto(
        Long id,
        String code,
        String name,
        Integer credits,
        List<String> prerequisiteCodes,
        boolean hasPrerequisites,
        boolean prerequisitesMet,
        List<String> missingPrerequisiteCodes,
        String missingSummary
) {}

