package kz.edu.sdu.advisor.service;

import kz.edu.sdu.advisor.model.Course;
import kz.edu.sdu.advisor.model.dto.GpaSimulationResultDto;
import kz.edu.sdu.advisor.repository.CartItemRepository;
import kz.edu.sdu.advisor.repository.CourseRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class GpaSimulationServiceTest {

    @Autowired
    private GpaSimulationService gpaSimulationService;

    @Autowired
    private CartService cartService;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Test
    @DisplayName("US-25: simulates credit-weighted projected cumulative GPA from cart courses and expected letter grades")
    void simulate_shouldCalculateProjectedCumulativeGpaFromCartCourses() {
        cartItemRepository.deleteAll();
        Course inf202 = courseRepository.findByCode("INF202").orElseThrow(); // 5 ECTS
        Course inf995 = courseRepository.findByCode("INF995").orElseThrow(); // 5 ECTS
        cartService.addCourse("240103000", inf202.getId());
        cartService.addCourse("240103000", inf995.getId());

        // Demo student baseline: 41 credits at 3.80 GPA (155.80 qp)
        // Simulated: 5 ECTS A (4.00) + 5 ECTS C (2.00) -> 30.00 qp / 10 ECTS = 3.00 semester GPA
        // Cumulative: (155.80 + 30.00) / 51 = 3.64
        GpaSimulationResultDto result = gpaSimulationService.simulate(
                "240103000",
                Map.of(
                        "grade_" + inf202.getId(), "A",
                        "grade_" + inf995.getId(), "C"
                )
        );

        assertThat(result.completedGradedCredits()).isEqualTo(41);
        assertThat(result.simulatedCredits()).isEqualTo(10);
        assertThat(result.semesterGpa()).isEqualTo(3.00);
        assertThat(result.projectedCumulativeGpa()).isEqualTo(3.64);
        assertThat(result.formattedProjectedGpa()).isEqualTo("3.64");
        assertThat(result.items()).hasSize(2);
    }

    @Test
    @DisplayName("US-25: rejects unsupported letter grade during simulation")
    void simulate_shouldRejectInvalidLetterGrade() {
        cartItemRepository.deleteAll();
        Course inf202 = courseRepository.findByCode("INF202").orElseThrow();
        cartService.addCourse("240103000", inf202.getId());

        assertThatThrownBy(() -> gpaSimulationService.simulate(
                "240103000", Map.of("grade_" + inf202.getId(), "Z+")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported GPA letter grade");
    }
}
