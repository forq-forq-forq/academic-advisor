package kz.edu.sdu.advisor.model.dto;

import kz.edu.sdu.advisor.model.Course;

/**
 * DTO representing an enrolled course with its code, name, and credit weight (US-08).
 */
public record EnrolledCourseDto(
        Long id,
        String code,
        String name,
        int credits,
        String formattedCredits
) {
    public static EnrolledCourseDto from(Course course) {
        if (course == null) {
            return null;
        }
        int cr = course.getCredits() != null ? course.getCredits() : 0;
        return new EnrolledCourseDto(
                course.getId(),
                course.getCode() != null ? course.getCode() : "—",
                course.getName() != null ? course.getName() : "—",
                cr,
                cr + " ECTS"
        );
    }
}

