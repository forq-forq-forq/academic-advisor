package kz.edu.sdu.advisor.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Academic course registration period window (US-09).
 */
@Entity
@Table(name = "registration_periods")
@Getter
@Setter
@NoArgsConstructor
public class RegistrationPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "term_name", nullable = false, length = 120)
    private String termName;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private boolean active = true;

    @Column(length = 500)
    private String description;

    public boolean isOpenOn(LocalDate date) {
        return active && startDate != null && endDate != null
                && !date.isBefore(startDate) && !date.isAfter(endDate);
    }
}
