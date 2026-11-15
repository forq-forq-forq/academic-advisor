package kz.edu.sdu.advisor.repository;

import kz.edu.sdu.advisor.model.RegistrationPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegistrationPeriodRepository extends JpaRepository<RegistrationPeriod, Long> {

    List<RegistrationPeriod> findByActiveTrueOrderByStartDateAsc();
}
