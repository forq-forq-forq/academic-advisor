package kz.edu.sdu.advisor.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Transcript entity mapping the {@code student_courses} table with per-student
 * course completion metadata ({@code letter_grade} and {@code semester_taken}).
 */
@Entity
@Table(name = "student_courses")
@Getter
@Setter
@NoArgsConstructor
public class CourseCompletion {

    @EmbeddedId
    private Id id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", insertable = false, updatable = false)
    private Course course;

    @Column(name = "letter_grade", length = 10)
    private String letterGrade;

    @Column(name = "semester_taken", length = 50)
    private String semesterTaken;

    public CourseCompletion(Long studentId, Course course, String letterGrade, String semesterTaken) {
        this.id = new Id(studentId, course != null ? course.getId() : null);
        this.course = course;
        this.letterGrade = letterGrade;
        this.semesterTaken = semesterTaken;
    }

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Id implements Serializable {
        @Column(name = "student_id")
        private Long studentId;

        @Column(name = "course_id")
        private Long courseId;
    }
}
