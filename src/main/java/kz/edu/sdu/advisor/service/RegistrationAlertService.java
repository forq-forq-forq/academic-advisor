package kz.edu.sdu.advisor.service;

import kz.edu.sdu.advisor.model.RegistrationPeriod;
import kz.edu.sdu.advisor.model.dto.RegistrationAlertDto;
import kz.edu.sdu.advisor.repository.RegistrationPeriodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Determines whether an upcoming or open course registration alert should be displayed (US-09).
 */
@Service
@RequiredArgsConstructor
public class RegistrationAlertService {

    public static final int UPCOMING_WINDOW_DAYS = 30;
    public static final int CLOSING_SOON_DAYS = 3;

    private final RegistrationPeriodRepository registrationPeriodRepository;

    @Transactional(readOnly = true)
    public Optional<RegistrationAlertDto> getActiveAlert() {
        return getActiveAlert(LocalDate.now());
    }

    @Transactional(readOnly = true)
    public Optional<RegistrationAlertDto> getActiveAlert(LocalDate referenceDate) {
        List<RegistrationPeriod> periods = registrationPeriodRepository.findByActiveTrueOrderByStartDateAsc();

        for (RegistrationPeriod period : periods) {
            if (period.isOpenOn(referenceDate)) {
                long daysLeft = ChronoUnit.DAYS.between(referenceDate, period.getEndDate());
                boolean closingSoon = daysLeft <= CLOSING_SOON_DAYS;
                String urgency = closingSoon ? "CLOSING_SOON" : "OPEN";
                String title = closingSoon
                        ? period.getTermName() + " closes soon"
                        : period.getTermName() + " is open";
                String dayLabel = daysLeft == 0 ? "today" : "in " + daysLeft + (daysLeft == 1 ? " day" : " days");
                String msg = "Registration closes " + dayLabel + " (" + period.getEndDate() + ")."
                        + appendDescription(period.getDescription());
                return Optional.of(new RegistrationAlertDto(
                        period.getTermName(), title, msg, urgency, daysLeft,
                        period.getStartDate(), period.getEndDate(), "Open Planner", "/planner"
                ));
            }
        }

        for (RegistrationPeriod period : periods) {
            if (period.getStartDate() != null && period.getStartDate().isAfter(referenceDate)) {
                long daysUntilStart = ChronoUnit.DAYS.between(referenceDate, period.getStartDate());
                if (daysUntilStart <= UPCOMING_WINDOW_DAYS) {
                    String title = period.getTermName() + " opens in "
                            + daysUntilStart + (daysUntilStart == 1 ? " day" : " days");
                    String msg = "Registration window runs " + period.getStartDate()
                            + " to " + period.getEndDate() + "."
                            + appendDescription(period.getDescription());
                    return Optional.of(new RegistrationAlertDto(
                            period.getTermName(), title, msg, "UPCOMING", daysUntilStart,
                            period.getStartDate(), period.getEndDate(), "Open Planner", "/planner"
                    ));
                }
            }
        }

        return Optional.empty();
    }

    private String appendDescription(String description) {
        return StringUtils.hasText(description) ? " " + description.trim() : "";
    }
}
