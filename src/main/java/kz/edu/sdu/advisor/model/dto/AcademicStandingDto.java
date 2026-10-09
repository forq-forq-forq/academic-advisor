package kz.edu.sdu.advisor.model.dto;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Encapsulates the student's academic standing, GPA, degree progress metrics (US-07),
 * and currently enrolled courses with credit weights (US-08).
 */
public record AcademicStandingDto(
        Double gpa,
        String formattedGpa,
        String standingLabel,
        String standingClass,
        int completedCredits,
        int enrolledCredits,
        int totalDegreeCredits,
        int remainingCredits,
        double completionPercent,
        String majorName,
        String facultyName,
        Integer catalogYear,
        Integer currentSemester,
        List<EnrolledCourseDto> enrolledCourses
) {
    public static AcademicStandingDto of(kz.edu.sdu.advisor.model.Student student) {
        if (student == null) {
            return new AcademicStandingDto(
                    0.0, "0.00", "Not Available", "standing-none",
                    0, 0, 240, 240, 0.0,
                    "—", "—", null, null,
                    List.of()
            );
        }

        Double gpa = student.getGpa();
        String formattedGpa = gpa != null ? String.format(java.util.Locale.US, "%.2f", gpa) : "N/A";
        String standingLabel;
        String standingClass;

        if (gpa == null) {
            standingLabel = "Not Available";
            standingClass = "standing-none";
        } else if (gpa >= 3.50) {
            standingLabel = "Honor Standing";
            standingClass = "standing-honor";
        } else if (gpa >= 2.67) {
            standingLabel = "Good Standing";
            standingClass = "standing-good";
        } else if (gpa >= 2.00) {
            standingLabel = "Satisfactory";
            standingClass = "standing-satisfactory";
        } else {
            standingLabel = "Academic Warning";
            standingClass = "standing-warning";
        }

        int totalDegreeCredits = student.getMajor() != null && student.getMajor().getTotalCredits() != null
                ? student.getMajor().getTotalCredits()
                : 240;
        int completedCredits = student.getCompletedCredits();
        int enrolledCredits = student.getEnrolledCredits();
        int remainingCredits = Math.max(0, totalDegreeCredits - completedCredits);
        double completionPercent = totalDegreeCredits > 0
                ? Math.min(100.0, (completedCredits * 100.0) / totalDegreeCredits)
                : 0.0;

        String majorName = student.getMajor() != null ? student.getMajor().getName() : "Undeclared";
        String facultyName = (student.getMajor() != null && student.getMajor().getFaculty() != null)
                ? student.getMajor().getFaculty().getName()
                : "—";

        List<EnrolledCourseDto> enrolledCoursesList = student.getEnrolledCourses() != null
                ? student.getEnrolledCourses().stream()
                        .filter(Objects::nonNull)
                        .map(EnrolledCourseDto::from)
                        .sorted(Comparator.comparing(EnrolledCourseDto::code))
                        .toList()
                : List.of();

        return new AcademicStandingDto(
                gpa,
                formattedGpa,
                standingLabel,
                standingClass,
                completedCredits,
                enrolledCredits,
                totalDegreeCredits,
                remainingCredits,
                completionPercent,
                majorName,
                facultyName,
                student.getCatalogYear(),
                student.getCurrentSemester(),
                enrolledCoursesList
        );
    }
}
