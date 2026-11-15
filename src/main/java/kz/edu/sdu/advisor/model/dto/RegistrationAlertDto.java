package kz.edu.sdu.advisor.model.dto;

import java.time.LocalDate;

/**
 * View model representing an upcoming or active registration period alert (US-09).
 */
public record RegistrationAlertDto(
        String termName,
        String title,
        String message,
        String urgencyLevel,
        long daysRemaining,
        LocalDate startDate,
        LocalDate endDate,
        String ctaLabel,
        String ctaUrl
) {
}
