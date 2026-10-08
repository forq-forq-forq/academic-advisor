package kz.edu.sdu.advisor.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "majors")
@Getter
@Setter
@NoArgsConstructor
public class Major {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String code;

    @Column(nullable = false, length = 128)
    private String name;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "faculty_id", nullable = false)
    private Faculty faculty;

    @Column(name = "total_credits", nullable = false)
    private Integer totalCredits = 240;

    public Major(String code, String name, Faculty faculty, Integer totalCredits) {
        this.code = code;
        this.name = name;
        this.faculty = faculty;
        this.totalCredits = totalCredits != null ? totalCredits : 240;
    }
}

