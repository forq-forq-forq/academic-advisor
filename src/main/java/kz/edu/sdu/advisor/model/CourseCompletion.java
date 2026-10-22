package kz.edu.sdu.advisor.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.io.Serializable;

/** Read-only transcript view of the existing completed-course join table. */
@Entity
@Immutable
@Table(name = "student_courses")
@Getter
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

    @Embeddable
    @Getter
    @NoArgsConstructor
    @EqualsAndHashCode
    public static class Id implements Serializable {
        @Column(name = "student_id")
        private Long studentId;

        @Column(name = "course_id")
        private Long courseId;
    }
}
