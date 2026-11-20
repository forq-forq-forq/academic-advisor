package kz.edu.sdu.advisor.service;

import kz.edu.sdu.advisor.model.Course;
import kz.edu.sdu.advisor.model.dto.RoadmapBoardDto;
import kz.edu.sdu.advisor.model.dto.RoadmapCourseCardDto;
import kz.edu.sdu.advisor.model.dto.RoadmapSemesterDto;
import kz.edu.sdu.advisor.repository.CourseRepository;
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
class RoadmapServiceTest {

    @Autowired
    private RoadmapService roadmapService;

    @Autowired
    private CourseRepository courseRepository;

    @Test
    @DisplayName("US-18: getRoadmap populates all 8 semesters with completed, enrolled, and planned curriculum courses")
    void getRoadmap_shouldPopulateAllEightSemesters() {
        RoadmapBoardDto board = roadmapService.getRoadmap("240103000");

        assertThat(board.semesters()).hasSize(8);
        assertThat(board.totalCourses()).isGreaterThanOrEqualTo(20);
        assertThat(board.totalCredits()).isGreaterThan(100);

        for (int i = 1; i <= 8; i++) {
            RoadmapSemesterDto sem = board.semesters().get(i - 1);
            assertThat(sem.semester()).isEqualTo(i);
            assertThat(sem.courses()).isNotEmpty();
        }

        // Completed courses in Sem 1-2, enrolled in Sem 3
        assertThat(board.semesters().get(0).courses())
                .anyMatch(c -> "CSS105".equals(c.code()) && "COMPLETED".equals(c.status()));
        assertThat(board.semesters().get(2).courses())
                .anyMatch(c -> "CSS215".equals(c.code()) && "ENROLLED".equals(c.status()));
    }

    @Test
    @DisplayName("US-19: moving a course before its prerequisite flags prerequisiteViolation and overloading >30 ECTS flags semester overloaded")
    void moveAndRemoveCourse_shouldFlagPrerequisiteAndOverloadViolations() {
        roadmapService.getRoadmap("240103000");
        Course css217 = courseRepository.findByCode("CSS217").orElseThrow();

        // CSS217 requires CSS215 (enrolled in Sem 3); moving CSS217 to Sem 2 triggers prerequisiteViolation
        RoadmapBoardDto afterViolation = roadmapService.moveCourseToSemester("240103000", css217.getId(), 2);
        RoadmapCourseCardDto movedCard = afterViolation.semesters().get(1).courses().stream()
                .filter(c -> "CSS217".equals(c.code()))
                .findFirst()
                .orElseThrow();

        assertThat(movedCard.prerequisiteViolation()).isTrue();
        assertThat(movedCard.violatedPrerequisites()).contains("CSS215");
        assertThat(afterViolation.prerequisiteConflictCount()).isGreaterThanOrEqualTo(1);

        // Moving extra courses into Sem 4 (> 30 ECTS) flags semester overloaded
        Course inf345 = courseRepository.findByCode("INF345").orElseThrow();
        Course inf317 = courseRepository.findByCode("INF317").orElseThrow();
        roadmapService.moveCourseToSemester("240103000", inf345.getId(), 4);
        RoadmapBoardDto overloadedBoard = roadmapService.moveCourseToSemester("240103000", inf317.getId(), 4);

        RoadmapSemesterDto sem4 = overloadedBoard.semesters().get(3);
        assertThat(sem4.totalCredits()).isGreaterThan(30);
        assertThat(sem4.overloaded()).isTrue();
        assertThat(sem4.workloadState()).isEqualTo("Overload");

        // Removing a course removes it from the board
        RoadmapBoardDto afterRemove = roadmapService.removeCourseFromRoadmap("240103000", inf317.getId());
        assertThat(afterRemove.semesters().get(3).courses())
                .noneMatch(c -> "INF317".equals(c.code()));
    }

    @Test
    @DisplayName("US-20: applyPacingStrategy supports BALANCED and FAST_TRACK with zero prerequisite violations and lighter 4th year in FAST_TRACK")
    void applyPacingStrategy_shouldRearrangeSemestersWithLighterFourthYearInFastTrack() {
        RoadmapBoardDto balanced = roadmapService.applyPacingStrategy("240103000", "BALANCED");
        int balancedYear4Credits = balanced.semesters().get(6).totalCredits()
                + balanced.semesters().get(7).totalCredits();

        assertThat(balanced.activePacingStrategy()).isEqualTo("BALANCED");
        assertThat(balanced.prerequisiteConflictCount()).isZero();

        RoadmapBoardDto fastTrack = roadmapService.applyPacingStrategy("240103000", "FAST_TRACK");
        int fastTrackYear4Credits = fastTrack.semesters().get(6).totalCredits()
                + fastTrack.semesters().get(7).totalCredits();

        assertThat(fastTrack.activePacingStrategy()).isEqualTo("FAST_TRACK");
        assertThat(fastTrack.prerequisiteConflictCount()).isZero();
        assertThat(fastTrackYear4Credits).isLessThan(balancedYear4Credits);
    }

    @Test
    @DisplayName("US-21: placing a Spring-only course in a Fall semester flags seasonMismatch and warning")
    void seasonMismatchFlagging_shouldFlagFallOnlyAndSpringOnlyConflicts() {
        roadmapService.getRoadmap("240103000");
        Course inf202 = courseRepository.findByCode("INF202").orElseThrow(); // Sem 4 -> SPRING
        Course inf381 = courseRepository.findByCode("INF381").orElseThrow(); // Sem 5 -> FALL

        // Move Spring-only INF202 into Fall Semester 5 -> flagged
        RoadmapBoardDto mismatchBoard = roadmapService.moveCourseToSemester("240103000", inf202.getId(), 5);
        RoadmapCourseCardDto inf202Card = mismatchBoard.semesters().get(4).courses().stream()
                .filter(c -> "INF202".equals(c.code()))
                .findFirst()
                .orElseThrow();
        assertThat(inf202Card.seasonMismatch()).isTrue();
        assertThat(inf202Card.seasonWarning()).contains("offered only in Spring");
        assertThat(mismatchBoard.seasonConflictCount()).isGreaterThanOrEqualTo(1);

        // Move INF202 back to Spring Semester 6 -> no mismatch
        RoadmapBoardDto validBoard = roadmapService.moveCourseToSemester("240103000", inf202.getId(), 6);
        RoadmapCourseCardDto fixedInf202Card = validBoard.semesters().get(5).courses().stream()
                .filter(c -> "INF202".equals(c.code()))
                .findFirst()
                .orElseThrow();
        assertThat(fixedInf202Card.seasonMismatch()).isFalse();
        assertThat(fixedInf202Card.seasonWarning()).isNull();

        // Move Fall-only INF381 into Spring Semester 6 -> flagged
        RoadmapBoardDto fallInSpring = roadmapService.moveCourseToSemester("240103000", inf381.getId(), 6);
        RoadmapCourseCardDto inf381Card = fallInSpring.semesters().get(5).courses().stream()
                .filter(c -> "INF381".equals(c.code()))
                .findFirst()
                .orElseThrow();
        assertThat(inf381Card.seasonMismatch()).isTrue();
        assertThat(inf381Card.seasonWarning()).contains("offered only in Fall");
    }
}
