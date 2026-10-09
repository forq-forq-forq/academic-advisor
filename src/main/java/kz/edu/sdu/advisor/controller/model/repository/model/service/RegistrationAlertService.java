package kz.edu.sdu.advisor.service;

import kz.edu.sdu.advisor.model.RegistrationAlert;
import kz.edu.sdu.advisor.repository.RegistrationPeriodRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
public class RegistrationAlertService {

    private static final int ALERT_WINDOW_DAYS = 14;

    private final RegistrationPeriodRepository periodRepository;

    public RegistrationAlertService(RegistrationPeriodRepository periodRepository) {
        this.periodRepository = periodRepository;
    }

    public Optional<RegistrationAlert> getAlert(String studentId) {
        LocalDate today = LocalDate.now();

        return periodRepository
                .findFirstByOpensAtGreaterThanEqualOrderByOpensAtAsc(today)
                .map(period -> new RegistrationAlert(
                        period.getName(),
                        ChronoUnit.DAYS.between(today, period.getOpensAt()),
                        getCartSize(studentId)))
                .filter(alert -> alert.daysUntilOpen() <= ALERT_WINDOW_DAYS);
    }

    private int getCartSize(String studentId) {
        return 0;
    }
}
