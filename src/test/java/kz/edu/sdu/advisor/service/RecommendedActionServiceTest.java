package kz.edu.sdu.advisor.service;

import kz.edu.sdu.advisor.model.CartItem;
import kz.edu.sdu.advisor.model.Course;
import kz.edu.sdu.advisor.model.Student;
import kz.edu.sdu.advisor.model.dto.RecommendedActionDto;
import kz.edu.sdu.advisor.repository.CartItemRepository;
import kz.edu.sdu.advisor.repository.CourseRepository;
import kz.edu.sdu.advisor.repository.RegistrationPeriodRepository;
import kz.edu.sdu.advisor.repository.StudentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RecommendedActionServiceTest {

    @Autowired
    private RecommendedActionService recommendedActionService;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private RegistrationPeriodRepository registrationPeriodRepository;

    @Test
    @DisplayName("Active registration period with empty cart recommends building the Fall 2026 plan")
    void activeAlertAndEmptyCartRecommendsPlanning() {
        Student student = studentRepository.findByStudentId("240103000").orElseThrow();
        RecommendedActionDto action = recommendedActionService.determineNextAction(student, 41, 20);

        assertThat(action.headline()).isEqualTo("Build your Fall 2026 plan");
        assertThat(action.ctaLabel()).isEqualTo("Open Planner");
        assertThat(action.ctaUrl()).isEqualTo("/planner");
        assertThat(action.category()).isEqualTo("PLAN_NEXT_TERM");
    }

    @Test
    @DisplayName("Non-empty semester cart recommends finalizing staged cart courses")
    void populatedCartRecommendsFinalizingCart() {
        Student student = studentRepository.findByStudentId("240103000").orElseThrow();
        Course course = courseRepository.findByCode("INF202").orElseThrow();
        CartItem item = new CartItem();
        item.setStudent(student);
        item.setCourse(course);
        cartItemRepository.save(item);

        RecommendedActionDto action = recommendedActionService.determineNextAction(student, 41, 20);

        assertThat(action.headline()).contains("Finalize your semester cart");
        assertThat(action.ctaUrl()).isEqualTo("/planner");
        assertThat(action.category()).isEqualTo("FINALIZE_CART");
    }

    @Test
    @DisplayName("No registration alert and zero completed credits recommends updating transcript")
    void zeroCompletedCreditsWithoutAlertRecommendsTranscript() {
        registrationPeriodRepository.deleteAll();
        Student student = studentRepository.findByStudentId("240103000").orElseThrow();

        RecommendedActionDto action = recommendedActionService.determineNextAction(student, 0, 0);

        assertThat(action.headline()).isEqualTo("Record your completed coursework");
        assertThat(action.ctaLabel()).isEqualTo("Update Transcript");
        assertThat(action.ctaUrl()).isEqualTo("/profile");
        assertThat(action.category()).isEqualTo("UPDATE_TRANSCRIPT");
    }
}
