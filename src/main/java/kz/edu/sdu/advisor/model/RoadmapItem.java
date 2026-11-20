package kz.edu.sdu.advisor.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "roadmap_items",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_roadmap_items_student_course",
        columnNames = {"student_id", "course_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
public class RoadmapItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false)
    private Integer semester;

    public RoadmapItem(Student student, Course course, Integer semester) {
        this.student = student;
        this.course = course;
        this.semester = semester;
    }
}
