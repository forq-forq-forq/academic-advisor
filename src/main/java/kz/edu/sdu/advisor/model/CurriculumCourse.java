package kz.edu.sdu.advisor.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "curriculum_courses",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"major_id", "course_id", "catalog_year"}
    )
)
@Getter
@Setter
@NoArgsConstructor
public class CurriculumCourse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "major_id", nullable = false)
    private Major major;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false)
    private Integer semester;

    @Column(name = "catalog_year", nullable = false)
    private Integer catalogYear;

    @Column(name = "course_type", nullable = false, length = 32)
    private String courseType = "CORE";

    public CurriculumCourse(Major major, Course course, Integer semester, Integer catalogYear, String courseType) {
        this.major = major;
        this.course = course;
        this.semester = semester;
        this.catalogYear = catalogYear;
        this.courseType = courseType != null ? courseType : "CORE";
    }
}

