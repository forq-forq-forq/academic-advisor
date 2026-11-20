package kz.edu.sdu.advisor.service;

import kz.edu.sdu.advisor.model.Course;
import kz.edu.sdu.advisor.model.Student;
import kz.edu.sdu.advisor.model.dto.GpaSimulationResultDto;
import kz.edu.sdu.advisor.repository.CartItemRepository;
import kz.edu.sdu.advisor.repository.CourseRepository;
import kz.edu.sdu.advisor.repository.StudentRepository;
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

    @Autowired
    private StudentRepository studentRepository;

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

    @Test
    @DisplayName("US-26: warns when projected GPA drops below 3.00 scholarship threshold, includes failing grade F, or triggers < 2.00 academic probation")
    void simulate_shouldFlagScholarshipAndProbationRisks() {
        cartItemRepository.deleteAll();
        Course inf202 = courseRepository.findByCode("INF202").orElseThrow();
        Course inf995 = courseRepository.findByCode("INF995").orElseThrow();
        cartService.addCourse("240103000", inf202.getId());
        cartService.addCourse("240103000", inf995.getId());

        // 1. Failing grade F triggers scholarship risk even if cumulative GPA >= 3.00
        GpaSimulationResultDto failingSim = gpaSimulationService.simulate(
                "240103000",
                Map.of(
                        "grade_" + inf202.getId(), "F",
                        "grade_" + inf995.getId(), "A"
                )
        );
        assertThat(failingSim.hasFailingGrade()).isTrue();
        assertThat(failingSim.belowScholarshipThreshold()).isTrue();
        assertThat(failingSim.academicProbationRisk()).isFalse();
        assertThat(failingSim.riskLevel()).isEqualTo("SCHOLARSHIP_WARNING");
        assertThat(failingSim.scholarshipWarningMessage()).contains("failing grade");

        // 2. Low-GPA student dropping below 3.00 scholarship threshold and < 2.00 probation threshold
        Student student = studentRepository.findByStudentId("240103000").orElseThrow();
        student.setGpa(1.90);
        studentRepository.saveAndFlush(student);

        GpaSimulationResultDto probationSim = gpaSimulationService.simulate(
                "240103000",
                Map.of(
                        "grade_" + inf202.getId(), "D",
                        "grade_" + inf995.getId(), "D"
                )
        );
        assertThat(probationSim.projectedCumulativeGpa()).isLessThan(2.00);
        assertThat(probationSim.belowScholarshipThreshold()).isTrue();
        assertThat(probationSim.academicProbationRisk()).isTrue();
        assertThat(probationSim.riskLevel()).isEqualTo("PROBATION_DANGER");
        assertThat(probationSim.probationWarningMessage()).contains("below 2.00");
    }
}
