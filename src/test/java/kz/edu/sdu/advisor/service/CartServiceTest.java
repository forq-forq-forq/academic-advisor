package kz.edu.sdu.advisor.service;

import kz.edu.sdu.advisor.model.Course;
import kz.edu.sdu.advisor.model.dto.CartDto;
import kz.edu.sdu.advisor.repository.CartItemRepository;
import kz.edu.sdu.advisor.repository.CourseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CartServiceTest {

    private static final String STUDENT_ID = "240103000";

    @Autowired
    private CartService cartService;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private CourseRepository courseRepository;

    private Course course3cr;
    private Course course4cr;
    private Course course0cr;

    @BeforeEach
    void setUp() {
        cartItemRepository.deleteAll();

        course3cr = courseRepository.findByCode("HIST101").orElseGet(() -> {
            Course c = new Course();
            c.setCode("HIST101");
            c.setName("History");
            c.setCredits(3);
            return courseRepository.save(c);
        });

        course4cr = courseRepository.findByCode("MATH101").orElseGet(() -> {
            Course c = new Course();
            c.setCode("MATH101");
            c.setName("Math");
            c.setCredits(4);
            return courseRepository.save(c);
        });

        course0cr = courseRepository.findByCode("ZERO101").orElseGet(() -> {
            Course c = new Course();
            c.setCode("ZERO101");
            c.setName("Zero Credit Course");
            c.setCredits(0);
            return courseRepository.save(c);
        });
    }

    @Test
    @DisplayName("QA-1: Empty cart display -> 0 / 40 credits, Light, green")
    void emptyCart_shouldHaveZeroCreditsAndLightWorkload() {
        CartDto cart = cartService.getCart(STUDENT_ID);

        assertThat(cart.items()).isEmpty();
        assertThat(cart.totalCredits()).isEqualTo(0);
        assertThat(cart.maxCredits()).isEqualTo(40);
        assertThat(cart.workloadState()).isEqualTo("Light");
        assertThat(cart.workloadColor()).isEqualTo("green");
        assertThat(cart.empty()).isTrue();
    }

    @Test
    @DisplayName("QA-2: Add course with credits -> Light workload (0-15 credits)")
    void addCourse_shouldIncreaseCreditsAndRetainLightWorkload() {
        CartDto cart = cartService.addCourse(STUDENT_ID, course4cr.getId());

        assertThat(cart.items()).hasSize(1);
        assertThat(cart.totalCredits()).isEqualTo(4);
        assertThat(cart.workloadState()).isEqualTo("Light");
        assertThat(cart.workloadColor()).isEqualTo("green");
        assertThat(cart.empty()).isFalse();
    }

    @Test
    @DisplayName("QA-3: Balanced load threshold (15-30 credits)")
    void balancedLoadThreshold_shouldEvaluateToBalancedAndYellow() {
        assertThat(cartService.getWorkloadState(15)).isEqualTo("Balanced");
        assertThat(cartService.getWorkloadColor(15)).isEqualTo("yellow");

        assertThat(cartService.getWorkloadState(20)).isEqualTo("Balanced");
        assertThat(cartService.getWorkloadColor(20)).isEqualTo("yellow");

        assertThat(cartService.getWorkloadState(29)).isEqualTo("Balanced");
        assertThat(cartService.getWorkloadColor(29)).isEqualTo("yellow");
    }

    @Test
    @DisplayName("QA-4: Overload state threshold (>= 30 credits)")
    void overloadThreshold_shouldEvaluateToOverloadAndRed() {
        assertThat(cartService.getWorkloadState(30)).isEqualTo("Overload");
        assertThat(cartService.getWorkloadColor(30)).isEqualTo("red");

        assertThat(cartService.getWorkloadState(40)).isEqualTo("Overload");
        assertThat(cartService.getWorkloadColor(40)).isEqualTo("red");
    }

    @Test
    @DisplayName("QA-5: Remove course updates credits and workload immediately")
    void removeCourse_shouldDecreaseCreditsAndReevaluateWorkload() {
        cartService.addCourse(STUDENT_ID, course3cr.getId());
        cartService.addCourse(STUDENT_ID, course4cr.getId());

        CartDto cartBefore = cartService.getCart(STUDENT_ID);
        assertThat(cartBefore.totalCredits()).isEqualTo(7);

        CartDto cartAfter = cartService.removeCourse(STUDENT_ID, course3cr.getId());
        assertThat(cartAfter.items()).hasSize(1);
        assertThat(cartAfter.totalCredits()).isEqualTo(4);
        assertThat(cartAfter.workloadState()).isEqualTo("Light");
    }

    @Test
    @DisplayName("QA-9: Adding a zero ECTS course should not break or increment counter")
    void zeroEctsCourse_shouldNotChangeTotalCredits() {
        CartDto cart = cartService.addCourse(STUDENT_ID, course0cr.getId());

        assertThat(cart.items()).hasSize(1);
        assertThat(cart.totalCredits()).isEqualTo(0);
        assertThat(cart.workloadState()).isEqualTo("Light");
    }

    @Test
    @DisplayName("Duplicate course addition should be idempotent (prevent duplicates)")
    void addSameCourseTwice_shouldNotDuplicateInCart() {
        cartService.addCourse(STUDENT_ID, course3cr.getId());
        CartDto cart = cartService.addCourse(STUDENT_ID, course3cr.getId());

        assertThat(cart.items()).hasSize(1);
        assertThat(cart.totalCredits()).isEqualTo(3);
    }

    @Test
    @DisplayName("Adding with non-existent student or course should throw IllegalArgumentException")
    void invalidStudentOrCourse_shouldThrowException() {
        assertThatThrownBy(() -> cartService.addCourse("999999999", course3cr.getId()))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> cartService.addCourse(STUDENT_ID, 999999L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("US-12 QA-1: Add course with satisfied prerequisites -> succeeds")
    void addCourse_withSatisfiedPrerequisites_shouldSucceed() {
        Course cs204 = courseRepository.findByCode("CS204").orElseThrow();
        CartDto cart = cartService.addCourse(STUDENT_ID, cs204.getId());

        assertThat(cart.items()).hasSize(1);
        assertThat(cart.items().get(0).code()).isEqualTo("CS204");
    }

    @Test
    @DisplayName("US-12 QA-2: Add course with uncompleted prerequisite -> throws PrerequisiteNotMetException")
    void addCourse_withMissingPrerequisites_shouldThrowException() {
        Course cs202 = courseRepository.findByCode("CS202").orElseThrow();

        assertThatThrownBy(() -> cartService.addCourse(STUDENT_ID, cs202.getId()))
                .isInstanceOf(kz.edu.sdu.advisor.exception.PrerequisiteNotMetException.class)
                .hasMessageContaining("Cannot add CS202: missing prerequisite CS201")
                .satisfies(ex -> {
                    kz.edu.sdu.advisor.exception.PrerequisiteNotMetException pEx =
                            (kz.edu.sdu.advisor.exception.PrerequisiteNotMetException) ex;
                    assertThat(pEx.getCourse().getCode()).isEqualTo("CS202");
                    assertThat(pEx.getMissingPrerequisites())
                            .extracting(Course::getCode)
                            .containsExactly("CS201");
                });
    }

    @Test
    @DisplayName("US-12 QA-3: Currently enrolled courses do NOT satisfy prerequisite requirement")
    void currentlyEnrolledCourse_doesNotSatisfyPrerequisite() {
        // CS201 is in student's enrolledCourses, but not completedCourses
        Course cs202 = courseRepository.findByCode("CS202").orElseThrow();

        assertThatThrownBy(() -> cartService.addCourse(STUDENT_ID, cs202.getId()))
                .isInstanceOf(kz.edu.sdu.advisor.exception.PrerequisiteNotMetException.class);
    }

    @Test
    @DisplayName("US-12 QA-4: Add course with no prerequisites -> succeeds")
    void addCourse_withoutPrerequisites_shouldSucceed() {
        Course cs101 = courseRepository.findByCode("CS101").orElseThrow();
        CartDto cart = cartService.addCourse(STUDENT_ID, cs101.getId());

        assertThat(cart.items()).hasSize(1);
        assertThat(cart.items().get(0).code()).isEqualTo("CS101");
    }

    @Test
    @DisplayName("US-13 QA-1 & QA-5: CartDto formattedCodes provides comma-separated course codes in order")
    void cartDto_formattedCodes_shouldReturnCommaSeparatedCodes() {
        // Empty cart
        CartDto emptyCart = cartService.getCart(STUDENT_ID);
        assertThat(emptyCart.formattedCodes()).isEmpty();

        // Add 1 course
        cartService.addCourse(STUDENT_ID, course3cr.getId()); // HIST101
        CartDto singleCart = cartService.getCart(STUDENT_ID);
        assertThat(singleCart.formattedCodes()).isEqualTo("HIST101");

        // Add 2nd course
        cartService.addCourse(STUDENT_ID, course4cr.getId()); // MATH101
        CartDto twoCart = cartService.getCart(STUDENT_ID);
        assertThat(twoCart.formattedCodes()).isEqualTo("HIST101, MATH101");

        // Remove 1 course
        CartDto afterRemove = cartService.removeCourse(STUDENT_ID, course3cr.getId());
        assertThat(afterRemove.formattedCodes()).isEqualTo("MATH101");
    }
}

