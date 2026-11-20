package kz.edu.sdu.advisor.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "plan_drafts")
@Getter
@Setter
@NoArgsConstructor
public class PlanDraft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "pacing_strategy", length = 40)
    private String pacingStrategy;

    @Column(name = "total_credits", nullable = false)
    private Integer totalCredits = 0;

    @Column(name = "items_json", nullable = false, columnDefinition = "TEXT")
    private String itemsJson;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
