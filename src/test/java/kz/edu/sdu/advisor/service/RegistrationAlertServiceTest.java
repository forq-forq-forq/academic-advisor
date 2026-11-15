package kz.edu.sdu.advisor.service;

import kz.edu.sdu.advisor.model.RegistrationPeriod;
import kz.edu.sdu.advisor.model.dto.RegistrationAlertDto;
import kz.edu.sdu.advisor.repository.RegistrationPeriodRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RegistrationAlertServiceTest {

    @Autowired
    private RegistrationAlertService registrationAlertService;

    @Autowired
    private RegistrationPeriodRepository registrationPeriodRepository;

    @Test
    @DisplayName("Seeded Fall 2026 registration window produces an upcoming registration alert")
    void seededPeriodProducesActiveAlert() {
        Optional<RegistrationAlertDto> alertOpt = registrationAlertService.getActiveAlert();
        assertThat(alertOpt).isPresent();
        RegistrationAlertDto alert = alertOpt.get();
        assertThat(alert.termName()).isEqualTo("Fall 2026 Course Registration");
        assertThat(alert.urgencyLevel()).isEqualTo("UPCOMING");
        assertThat(alert.ctaUrl()).isEqualTo("/planner");
    }

    @Test
    @DisplayName("Open and closing-soon periods take priority over upcoming windows")
    void openAndClosingSoonPeriodsComputeCorrectUrgency() {
        registrationPeriodRepository.deleteAll();

        LocalDate today = LocalDate.of(2026, 8, 25);
        RegistrationPeriod openPeriod = new RegistrationPeriod();
        openPeriod.setTermName("Fall 2026 Add/Drop");
        openPeriod.setStartDate(today.minusDays(2));
        openPeriod.setEndDate(today.plusDays(2));
        openPeriod.setActive(true);
        openPeriod.setDescription("Finalize your timetable.");
        registrationPeriodRepository.save(openPeriod);

        RegistrationAlertDto closingAlert = registrationAlertService.getActiveAlert(today).orElseThrow();
        assertThat(closingAlert.urgencyLevel()).isEqualTo("CLOSING_SOON");
        assertThat(closingAlert.daysRemaining()).isEqualTo(2);
        assertThat(closingAlert.title()).contains("closes soon");

        openPeriod.setEndDate(today.plusDays(7));
        registrationPeriodRepository.save(openPeriod);

        RegistrationAlertDto openAlert = registrationAlertService.getActiveAlert(today).orElseThrow();
        assertThat(openAlert.urgencyLevel()).isEqualTo("OPEN");
        assertThat(openAlert.daysRemaining()).isEqualTo(7);
        assertThat(openAlert.title()).contains("is open");
    }

    @Test
    @DisplayName("Inactive or distant registration windows return empty alert")
    void inactiveOrDistantPeriodsReturnEmpty() {
        registrationPeriodRepository.deleteAll();

        LocalDate today = LocalDate.of(2026, 8, 25);
        RegistrationPeriod distant = new RegistrationPeriod();
        distant.setTermName("Spring 2027 Registration");
        distant.setStartDate(today.plusDays(60));
        distant.setEndDate(today.plusDays(74));
        distant.setActive(true);
        registrationPeriodRepository.save(distant);

        assertThat(registrationAlertService.getActiveAlert(today)).isEmpty();
    }
}
