package kz.edu.sdu.advisor.model.dto;

/**
 * Single course entry participating in a GPA "what-if" simulation (US-25).
 */
public record GpaSimulationItemDto(
        Long courseId,
        String code,
        String name,
        int credits,
        String expectedGrade,
        double gradePoints,
        String source
) {
}
