package kz.edu.sdu.advisor.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kz.edu.sdu.advisor.model.CartItem;
import kz.edu.sdu.advisor.model.Course;
import kz.edu.sdu.advisor.model.PlanDraft;
import kz.edu.sdu.advisor.model.RoadmapItem;
import kz.edu.sdu.advisor.model.Student;
import kz.edu.sdu.advisor.model.dto.CartDto;
import kz.edu.sdu.advisor.model.dto.LoadedPlanDraftDto;
import kz.edu.sdu.advisor.model.dto.PlanDraftDto;
import kz.edu.sdu.advisor.model.dto.RoadmapBoardDto;
import kz.edu.sdu.advisor.repository.CartItemRepository;
import kz.edu.sdu.advisor.repository.CourseRepository;
import kz.edu.sdu.advisor.repository.PlanDraftRepository;
import kz.edu.sdu.advisor.repository.RoadmapItemRepository;
import kz.edu.sdu.advisor.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Persists, lists, restores, and deletes named academic plan drafts (US-22).
 */
@Service
@RequiredArgsConstructor
public class PlanDraftService {

    private final PlanDraftRepository planDraftRepository;
    private final StudentRepository studentRepository;
    private final CartItemRepository cartItemRepository;
    private final RoadmapItemRepository roadmapItemRepository;
    private final CourseRepository courseRepository;
    private final CartService cartService;
    private final RoadmapService roadmapService;
    private final ObjectMapper objectMapper;

    @Transactional
    public PlanDraftDto saveDraft(String studentId, String name, String pacingStrategy) {
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("Draft name is required.");
        }
        String trimmedName = name.trim();
        if (trimmedName.length() > 120) {
            throw new IllegalArgumentException("Draft name must not exceed 120 characters.");
        }

        Student student = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));

        List<CartItem> cartItems = cartItemRepository.findByStudent_StudentIdOrderByCourse_CodeAsc(studentId);
        List<RoadmapItem> roadmapItems = roadmapItemRepository.findByStudent_StudentIdOrderBySemesterAscCourse_CodeAsc(studentId);

        List<Long> cartCourseIds = cartItems.stream()
                .map(i -> i.getCourse().getId())
                .toList();
        int cartCredits = cartItems.stream()
                .mapToInt(i -> i.getCourse().getCredits() != null ? i.getCourse().getCredits() : 0)
                .sum();
        String cartCodesSummary = cartItems.stream()
                .map(i -> i.getCourse().getCode())
                .collect(Collectors.joining(", "));

        List<RoadmapSlot> roadmapSlots = roadmapItems.stream()
                .filter(i -> i.getCourse() != null && i.getCourse().getId() != null)
                .map(i -> new RoadmapSlot(i.getCourse().getId(), i.getSemester()))
                .toList();
        int roadmapCredits = roadmapItems.stream()
                .filter(i -> i.getCourse() != null)
                .mapToInt(i -> i.getCourse().getCredits() != null ? i.getCourse().getCredits() : 0)
                .sum();

        int effectiveCredits = cartCredits > 0 ? cartCredits : roadmapCredits;
        String strategy = StringUtils.hasText(pacingStrategy)
                ? pacingStrategy.trim().toUpperCase()
                : RoadmapService.DEFAULT_PACING_STRATEGY;

        DraftPayload payload = new DraftPayload(
                cartCourseIds,
                cartCredits,
                cartCodesSummary,
                roadmapSlots,
                roadmapCredits
        );

        String json = writePayload(payload);
        LocalDateTime now = LocalDateTime.now();

        PlanDraft draft = planDraftRepository.findByStudent_StudentIdAndNameIgnoreCase(studentId, trimmedName)
                .orElseGet(PlanDraft::new);
        draft.setStudent(student);
        draft.setName(trimmedName);
        draft.setPacingStrategy(strategy);
        draft.setTotalCredits(effectiveCredits);
        draft.setItemsJson(json);
        if (draft.getCreatedAt() == null) {
            draft.setCreatedAt(now);
        }
        draft.setUpdatedAt(now);

        PlanDraft saved = planDraftRepository.saveAndFlush(draft);
        return toDto(saved, payload);
    }

    @Transactional(readOnly = true)
    public List<PlanDraftDto> getDrafts(String studentId) {
        return planDraftRepository.findByStudent_StudentIdOrderByUpdatedAtDesc(studentId).stream()
                .map(d -> toDto(d, readPayload(d.getItemsJson())))
                .toList();
    }

    @Transactional
    public LoadedPlanDraftDto loadDraft(String studentId, Long draftId) {
        Student student = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));
        PlanDraft draft = planDraftRepository.findByIdAndStudent_StudentId(draftId, studentId)
                .orElseThrow(() -> new IllegalArgumentException("Plan draft not found: " + draftId));

        DraftPayload payload = readPayload(draft.getItemsJson());

        // Restore cart items cleanly
        List<CartItem> existingCart = cartItemRepository.findByStudent_StudentIdOrderByCourse_CodeAsc(studentId);
        if (!existingCart.isEmpty()) {
            cartItemRepository.deleteAll(existingCart);
            cartItemRepository.flush();
        }
        Set<Long> addedCartIds = new LinkedHashSet<>();
        if (payload.cartCourseIds() != null) {
            for (Long courseId : payload.cartCourseIds()) {
                if (courseId == null || !addedCartIds.add(courseId)) continue;
                courseRepository.findById(courseId).ifPresent(course ->
                        cartItemRepository.save(new CartItem(student, course)));
            }
        }
        cartItemRepository.flush();

        // Restore roadmap placements if saved
        if (payload.roadmap() != null && !payload.roadmap().isEmpty()) {
            List<RoadmapItem> existingRoadmap = roadmapItemRepository
                    .findByStudent_StudentIdOrderBySemesterAscCourse_CodeAsc(studentId);
            if (!existingRoadmap.isEmpty()) {
                roadmapItemRepository.deleteAll(existingRoadmap);
                roadmapItemRepository.flush();
            }
            Set<Long> addedRoadmapIds = new LinkedHashSet<>();
            List<RoadmapItem> restored = new ArrayList<>();
            for (RoadmapSlot slot : payload.roadmap()) {
                if (slot.courseId() == null || !addedRoadmapIds.add(slot.courseId())) continue;
                courseRepository.findById(slot.courseId()).ifPresent(course -> {
                    int sem = Math.max(1, Math.min(RoadmapService.TOTAL_SEMESTERS, slot.semester()));
                    restored.add(new RoadmapItem(student, course, sem));
                });
            }
            roadmapItemRepository.saveAllAndFlush(restored);
        }

        CartDto cart = cartService.getCart(studentId);
        RoadmapBoardDto roadmap = roadmapService.getRoadmap(studentId);
        return new LoadedPlanDraftDto(toDto(draft, payload), cart, roadmap);
    }

    @Transactional
    public void deleteDraft(String studentId, Long draftId) {
        PlanDraft draft = planDraftRepository.findByIdAndStudent_StudentId(draftId, studentId)
                .orElseThrow(() -> new IllegalArgumentException("Plan draft not found: " + draftId));
        planDraftRepository.delete(draft);
        planDraftRepository.flush();
    }

    private PlanDraftDto toDto(PlanDraft draft, DraftPayload payload) {
        int cartCount = payload.cartCourseIds() != null ? payload.cartCourseIds().size() : 0;
        int roadmapCount = payload.roadmap() != null ? payload.roadmap().size() : 0;
        String summary = StringUtils.hasText(payload.cartCodesSummary())
                ? payload.cartCodesSummary()
                : (roadmapCount > 0 ? ("4-year roadmap (" + roadmapCount + " courses)") : "Empty draft");

        return new PlanDraftDto(
                draft.getId(),
                draft.getName(),
                draft.getPacingStrategy(),
                draft.getTotalCredits() != null ? draft.getTotalCredits() : 0,
                payload.cartCredits(),
                cartCount,
                roadmapCount,
                summary,
                draft.getUpdatedAt()
        );
    }

    private String writePayload(DraftPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize plan draft", ex);
        }
    }

    private DraftPayload readPayload(String json) {
        if (!StringUtils.hasText(json)) {
            return new DraftPayload(List.of(), 0, "", List.of(), 0);
        }
        try {
            return objectMapper.readValue(json, DraftPayload.class);
        } catch (JsonProcessingException ex) {
            return new DraftPayload(List.of(), 0, "", List.of(), 0);
        }
    }

    public record DraftPayload(
            List<Long> cartCourseIds,
            int cartCredits,
            String cartCodesSummary,
            List<RoadmapSlot> roadmap,
            int roadmapCredits
    ) {}

    public record RoadmapSlot(Long courseId, int semester) {}
}
