package kz.edu.sdu.advisor.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "prerequisites",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"course_id", "prerequisite_course_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
public class Prerequisite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne
    @JoinColumn(name = "prerequisite_course_id", nullable = false)
    private Course prerequisiteCourse;
}