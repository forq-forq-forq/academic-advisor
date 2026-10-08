package kz.edu.sdu.advisor.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "students")
@Getter
@Setter
@NoArgsConstructor
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false, unique = true)
    private String studentId;

    @Column(nullable = false)
    private String name;

    @Column(unique = true)
    private String email;

    private Double gpa;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "major_id")
    private Major major;

    @Column(name = "catalog_year")
    private Integer catalogYear;

    @Column(name = "current_semester")
    private Integer currentSemester;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "account_id")
    private StudentAccount account;

    @ManyToMany
    @JoinTable(
        name = "student_courses",
        joinColumns = @JoinColumn(name = "student_id"),
        inverseJoinColumns = @JoinColumn(name = "course_id")
    )
    private Set<Course> completedCourses = new HashSet<>();

    @ManyToMany
    @JoinTable(
        name = "student_enrolled_courses",
        joinColumns = @JoinColumn(name = "student_id"),
        inverseJoinColumns = @JoinColumn(name = "course_id")
    )
    private Set<Course> enrolledCourses = new HashSet<>();

    public int getCompletedCredits() {
        if (completedCourses == null) {
            return 0;
        }
        return completedCourses.stream()
                .mapToInt(c -> c.getCredits() != null ? c.getCredits() : 0)
                .sum();
    }

    public int getEnrolledCredits() {
        if (enrolledCourses == null) {
            return 0;
        }
        return enrolledCourses.stream()
                .mapToInt(c -> c.getCredits() != null ? c.getCredits() : 0)
                .sum();
    }
}