package kz.edu.sdu.advisor.service;

import kz.edu.sdu.advisor.model.dto.RoadmapBoardDto;
import kz.edu.sdu.advisor.model.dto.RoadmapSemesterDto;
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
}
