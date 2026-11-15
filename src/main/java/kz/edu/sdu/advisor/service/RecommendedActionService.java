package kz.edu.sdu.advisor.service;

import kz.edu.sdu.advisor.model.CartItem;
import kz.edu.sdu.advisor.model.Student;
import kz.edu.sdu.advisor.model.dto.RecommendedActionDto;
import kz.edu.sdu.advisor.model.dto.RegistrationAlertDto;
import kz.edu.sdu.advisor.repository.CartItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Determines the most helpful next academic planning step for the dashboard hero card (US-10).
 */
@Service
@RequiredArgsConstructor
public class RecommendedActionService {

    private final RegistrationAlertService registrationAlertService;
    private final CartItemRepository cartItemRepository;

    @Transactional(readOnly = true)
    public RecommendedActionDto determineNextAction(Student student, int completedCredits, int enrolledCredits) {
        List<CartItem> cartItems = student != null && student.getStudentId() != null
                ? cartItemRepository.findByStudent_StudentIdOrderByCourse_CodeAsc(student.getStudentId())
                : List.of();
        int cartCredits = cartItems.stream()
                .mapToInt(item -> item.getCourse() != null && item.getCourse().getCredits() != null
                        ? item.getCourse().getCredits()
                        : 0)
                .sum();

        Optional<RegistrationAlertDto> alertOpt = registrationAlertService.getActiveAlert();

        if (alertOpt.isPresent() && cartItems.isEmpty()) {
            String term = extractTermLabel(alertOpt.get().termName());
            return new RecommendedActionDto(
                    "Build your " + term + " plan",
                    "You have no courses staged in your semester cart yet. Explore eligible courses, check prerequisites, and plan up to 30 ECTS for " + term + ".",
                    "Open Planner",
                    "/planner",
                    "PLAN_NEXT_TERM"
            );
        }

        if (!cartItems.isEmpty()) {
            return new RecommendedActionDto(
                    "Finalize your semester cart (" + cartItems.size() + " courses, " + cartCredits + " ECTS)",
                    "Review prerequisite checks and credit workload, then submit your enrollment before registration closes.",
                    "Open Planner",
                    "/planner",
                    "FINALIZE_CART"
            );
        }

        if (completedCredits <= 0) {
            return new RecommendedActionDto(
                    "Record your completed coursework",
                    "Add completed courses and letter grades in your Transcript Profile to unlock accurate prerequisite checks and GPA tracking.",
                    "Update Transcript",
                    "/profile",
                    "UPDATE_TRANSCRIPT"
            );
        }

        return new RecommendedActionDto(
                "Explore courses for your next semester",
                "You currently carry " + enrolledCredits + " enrolled ECTS and " + completedCredits
                        + " completed ECTS. Preview upcoming core courses and career-aligned electives in the Planner.",
                "Open Planner",
                "/planner",
                "REVIEW_PLAN"
        );
    }

    private String extractTermLabel(String termName) {
        if (termName == null || termName.isBlank()) {
            return "next semester";
        }
        String cleaned = termName
                .replace("Course Registration", "")
                .replace("Add/Drop", "")
                .replace("Registration", "")
                .trim();
        return cleaned.isEmpty() ? termName.trim() : cleaned;
    }
}
