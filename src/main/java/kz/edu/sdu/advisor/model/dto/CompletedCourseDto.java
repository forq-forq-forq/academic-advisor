package kz.edu.sdu.advisor.model.dto;

import kz.edu.sdu.advisor.model.CourseCompletion;

public record CompletedCourseDto(String code, String name, Integer credits,
                                 String letterGrade, String semesterTaken) {
    public static CompletedCourseDto of(CourseCompletion completion) {
        return new CompletedCourseDto(completion.getCourse().getCode(), completion.getCourse().getName(),
                completion.getCourse().getCredits(), recorded(completion.getLetterGrade()),
                recorded(completion.getSemesterTaken()));
    }

    private static String recorded(String value) {
        return value == null || value.isBlank() ? "Not recorded" : value;
    }
}
