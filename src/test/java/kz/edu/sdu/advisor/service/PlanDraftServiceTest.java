package kz.edu.sdu.advisor.service;

import kz.edu.sdu.advisor.model.Course;
import kz.edu.sdu.advisor.model.dto.CartDto;
import kz.edu.sdu.advisor.model.dto.LoadedPlanDraftDto;
import kz.edu.sdu.advisor.model.dto.PlanDraftDto;
import kz.edu.sdu.advisor.repository.CartItemRepository;
import kz.edu.sdu.advisor.repository.CourseRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PlanDraftServiceTest {

    @Autowired
    private PlanDraftService planDraftService;

    @Autowired
    private CartService cartService;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Test
    @DisplayName("US-22: student can save multiple drafts (Safe plan vs Ambitious plan), load a draft to restore cart, and delete a draft")
    void saveLoadAndDeleteMultipleDrafts_shouldPersistAndRestorePlans() {
        cartItemRepository.deleteAll();
        Course css215 = courseRepository.findByCode("CSS215").orElseThrow();
        Course inf202 = courseRepository.findByCode("INF202").orElseThrow();
        Course inf995 = courseRepository.findByCode("INF995").orElseThrow();

        // Build Safe plan (2 courses = 10 ECTS)
        cartService.addCourse("240103000", css215.getId());
        cartService.addCourse("240103000", inf202.getId());
        PlanDraftDto safeDraft = planDraftService.saveDraft("240103000", "Safe plan", "BALANCED");

        // Build Ambitious plan (3 courses = 15 ECTS)
        cartService.addCourse("240103000", inf995.getId());
        PlanDraftDto ambitiousDraft = planDraftService.saveDraft("240103000", "Ambitious plan", "FAST_TRACK");

        List<PlanDraftDto> drafts = planDraftService.getDrafts("240103000");
        assertThat(drafts).hasSize(2);
        assertThat(drafts).extracting(PlanDraftDto::name).containsExactlyInAnyOrder("Safe plan", "Ambitious plan");
        assertThat(safeDraft.totalCredits()).isEqualTo(10);
        assertThat(ambitiousDraft.totalCredits()).isEqualTo(15);

        // Load Safe plan -> cart reverts to 2 courses (10 ECTS)
        LoadedPlanDraftDto loaded = planDraftService.loadDraft("240103000", safeDraft.id());
        CartDto restoredCart = loaded.cart();
        assertThat(restoredCart.items()).hasSize(2);
        assertThat(restoredCart.totalCredits()).isEqualTo(10);

        // Delete Ambitious plan -> only Safe plan remains
        planDraftService.deleteDraft("240103000", ambitiousDraft.id());
        assertThat(planDraftService.getDrafts("240103000")).hasSize(1);
    }
}
