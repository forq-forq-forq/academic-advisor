package kz.edu.sdu.advisor.repository;

import kz.edu.sdu.advisor.model.RegistrationPeriod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface RegistrationPeriodRepository extends JpaRepository<RegistrationPeriod, Long> {
    Optional<RegistrationPeriod> findFirstByOpensAtGreaterThanEqualOrderByOpensAtAsc(LocalDate date);
}
