package kz.edu.sdu.advisor.exception;

import kz.edu.sdu.advisor.model.Course;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Thrown when a student attempts to add a course to their semester cart without having completed
 * all of its required prerequisites (US-12).
 */
@Getter
public class PrerequisiteNotMetException extends IllegalArgumentException {

    private final Course targetCourse;
    private final List<Course> missingPrerequisites;

    public PrerequisiteNotMetException(Course targetCourse, List<Course> missingPrerequisites) {
        super(buildMessage(targetCourse, missingPrerequisites));
        this.targetCourse = targetCourse;
        this.missingPrerequisites = missingPrerequisites != null ? List.copyOf(missingPrerequisites) : List.of();
    }

    public Course getCourse() {
        return targetCourse;
    }

    private static String buildMessage(Course targetCourse, List<Course> missing) {
        String courseCode = targetCourse != null ? targetCourse.getCode() : "course";
        if (missing == null || missing.isEmpty()) {
            return "Cannot add " + courseCode + ": prerequisite requirements not met.";
        }

        String formattedMissing = missing.stream()
                .map(c -> c.getCode() + " (" + c.getName() + ")")
                .collect(Collectors.joining(", "));

        if (missing.size() == 1) {
            return String.format("Cannot add %s: missing prerequisite %s", courseCode, formattedMissing);
        } else {
            return String.format("Cannot add %s: missing prerequisites: %s", courseCode, formattedMissing);
        }
    }
}
