package kz.edu.sdu.advisor.controller;

import jakarta.servlet.http.HttpSession;
import kz.edu.sdu.advisor.exception.PrerequisiteNotMetException;
import kz.edu.sdu.advisor.model.Course;
import kz.edu.sdu.advisor.model.CourseCompletion;
import kz.edu.sdu.advisor.model.CurriculumCourse;
import kz.edu.sdu.advisor.model.Prerequisite;
import kz.edu.sdu.advisor.model.Student;
import kz.edu.sdu.advisor.model.dto.AvailableCourseDto;
import kz.edu.sdu.advisor.model.dto.CartDto;
import kz.edu.sdu.advisor.model.dto.CourseDetailDto;
import kz.edu.sdu.advisor.model.dto.EnrolledCourseDto;
import kz.edu.sdu.advisor.model.dto.LoadedPlanDraftDto;
import kz.edu.sdu.advisor.model.dto.PlanDraftDto;
import kz.edu.sdu.advisor.model.dto.RoadmapBoardDto;
import kz.edu.sdu.advisor.repository.CourseCompletionRepository;
import kz.edu.sdu.advisor.repository.CourseRepository;
import kz.edu.sdu.advisor.repository.CurriculumCourseRepository;
import kz.edu.sdu.advisor.repository.PrerequisiteRepository;
import kz.edu.sdu.advisor.service.AuthSessionHelper;
import kz.edu.sdu.advisor.service.CartService;
import kz.edu.sdu.advisor.service.PlanDraftService;
import kz.edu.sdu.advisor.service.PlanPdfExportService;
import kz.edu.sdu.advisor.service.RoadmapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequiredArgsConstructor
public class PlannerController {

    private final AuthSessionHelper authSessionHelper;
    private final CourseRepository courseRepository;
    private final CourseCompletionRepository courseCompletionRepository;
    private final CurriculumCourseRepository curriculumCourseRepository;
    private final PrerequisiteRepository prerequisiteRepository;
    private final CartService cartService;
    private final RoadmapService roadmapService;
    private final PlanDraftService planDraftService;
    private final PlanPdfExportService planPdfExportService;

    @GetMapping("/planner")
    public String showPlanner(HttpSession session, Model model) {
        Student student = authSessionHelper.resolveAuthenticatedStudent(session).orElse(null);
        if (student == null) {
            return "redirect:/login";
        }
        String studentId = student.getStudentId();

        CartDto cart = cartService.getCart(studentId);
        List<AvailableCourseDto> availableCourseDtos = buildAvailableCourses(student, cart);
        List<String> completedCodes = resolveCompletedCourses(student).values().stream()
                .map(Course::getCode)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();

        List<EnrolledCourseDto> enrolledCourses = cartService.getEnrolledCourses(studentId);
        int enrolledCredits = enrolledCourses.stream().mapToInt(EnrolledCourseDto::credits).sum();
        RoadmapBoardDto roadmap = roadmapService.getRoadmap(studentId);
        List<PlanDraftDto> planDrafts = planDraftService.getDrafts(studentId);

        model.addAttribute("student", student);
        model.addAttribute("cart", cart);
        model.addAttribute("enrolledCourses", enrolledCourses);
        model.addAttribute("enrolledCredits", enrolledCredits);
        model.addAttribute("availableCourses", availableCourseDtos);
        model.addAttribute("completedCourseCodes", completedCodes);
        model.addAttribute("roadmap", roadmap);
        model.addAttribute("planDrafts", planDrafts);

        return "planner";
    }

    @GetMapping("/planner/roadmap")
    @ResponseBody
    public ResponseEntity<?> getRoadmap(HttpSession session) {
        String studentId = resolveStudentId(session);
        if (studentId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Session expired. Please log in again."));
        }
        return ResponseEntity.ok(roadmapService.getRoadmap(studentId));
    }

    @GetMapping(value = "/planner/export/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportPlanPdf(
            @RequestParam(value = "scope", defaultValue = "all") String scope,
            HttpSession session) {
        Student student = authSessionHelper.resolveAuthenticatedStudent(session).orElse(null);
        if (student == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String studentId = student.getStudentId();
        CartDto cart = cartService.getCart(studentId);
        RoadmapBoardDto roadmap = roadmapService.getRoadmap(studentId);
        byte[] pdfBytes = planPdfExportService.generatePlanPdf(student, cart, roadmap, scope);
        String safeId = studentId.replaceAll("[^a-zA-Z0-9_-]", "");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"academic-plan-" + safeId + ".pdf\"")
                .body(pdfBytes);
    }

    @PostMapping("/planner/roadmap/move")
    @ResponseBody
    public ResponseEntity<?> moveRoadmapCourse(
            @RequestParam("courseId") Long courseId,
            @RequestParam("targetSemester") int targetSemester,
            HttpSession session) {
        String studentId = resolveStudentId(session);
        if (studentId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Session expired. Please log in again."));
        }
        try {
            return ResponseEntity.ok(roadmapService.moveCourseToSemester(studentId, courseId, targetSemester));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/planner/roadmap/remove")
    @ResponseBody
    public ResponseEntity<?> removeRoadmapCourse(
            @RequestParam("courseId") Long courseId,
            HttpSession session) {
        String studentId = resolveStudentId(session);
        if (studentId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Session expired. Please log in again."));
        }
        try {
            return ResponseEntity.ok(roadmapService.removeCourseFromRoadmap(studentId, courseId));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/planner/roadmap/pacing")
    @ResponseBody
    public ResponseEntity<?> applyRoadmapPacing(
            @RequestParam("strategy") String strategy,
            HttpSession session) {
        String studentId = resolveStudentId(session);
        if (studentId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Session expired. Please log in again."));
        }
        try {
            return ResponseEntity.ok(roadmapService.applyPacingStrategy(studentId, strategy));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/planner/drafts")
    @ResponseBody
    public ResponseEntity<?> listPlanDrafts(HttpSession session) {
        String studentId = resolveStudentId(session);
        if (studentId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Session expired. Please log in again."));
        }
        return ResponseEntity.ok(planDraftService.getDrafts(studentId));
    }

    @PostMapping("/planner/drafts/save")
    @ResponseBody
    public ResponseEntity<?> savePlanDraft(
            @RequestParam("name") String name,
            @RequestParam(value = "pacingStrategy", required = false) String pacingStrategy,
            HttpSession session) {
        String studentId = resolveStudentId(session);
        if (studentId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Session expired. Please log in again."));
        }
        try {
            PlanDraftDto saved = planDraftService.saveDraft(studentId, name, pacingStrategy);
            List<PlanDraftDto> drafts = planDraftService.getDrafts(studentId);
            return ResponseEntity.ok(Map.of("draft", saved, "drafts", drafts));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/planner/drafts/{draftId}/load")
    public Object loadPlanDraft(
            @PathVariable("draftId") Long draftId,
            @RequestParam(value = "redirect", required = false) String redirect,
            HttpSession session) {
        String studentId = resolveStudentId(session);
        if (studentId == null) {
            if (redirect != null && !redirect.isBlank()) {
                return "redirect:/login";
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Session expired. Please log in again."));
        }
        try {
            LoadedPlanDraftDto loaded = planDraftService.loadDraft(studentId, draftId);
            if (redirect != null && !redirect.isBlank()) {
                return "redirect:" + (redirect.startsWith("/") ? redirect : "/" + redirect);
            }
            return ResponseEntity.ok(loaded);
        } catch (IllegalArgumentException ex) {
            if (redirect != null && !redirect.isBlank()) {
                return "redirect:" + (redirect.startsWith("/") ? redirect : "/" + redirect);
            }
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/planner/drafts/{draftId}/delete")
    public Object deletePlanDraft(
            @PathVariable("draftId") Long draftId,
            @RequestParam(value = "redirect", required = false) String redirect,
            HttpSession session) {
        String studentId = resolveStudentId(session);
        if (studentId == null) {
            if (redirect != null && !redirect.isBlank()) {
                return "redirect:/login";
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Session expired. Please log in again."));
        }
        try {
            planDraftService.deleteDraft(studentId, draftId);
            if (redirect != null && !redirect.isBlank()) {
                return "redirect:" + (redirect.startsWith("/") ? redirect : "/" + redirect);
            }
            return ResponseEntity.ok(Map.of("drafts", planDraftService.getDrafts(studentId)));
        } catch (IllegalArgumentException ex) {
            if (redirect != null && !redirect.isBlank()) {
                return "redirect:" + (redirect.startsWith("/") ? redirect : "/" + redirect);
            }
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/planner/catalog")
    @ResponseBody
    public ResponseEntity<?> searchCatalog(
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "season", required = false) String season,
            @RequestParam(value = "difficulty", required = false) Integer difficulty,
            HttpSession session) {
        Student student = authSessionHelper.resolveAuthenticatedStudent(session).orElse(null);
        if (student == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Session expired. Please log in again."));
        }

        CartDto cart = cartService.getCart(student.getStudentId());
        List<AvailableCourseDto> courses = buildAvailableCourses(student, cart);

        if (query != null && !query.isBlank()) {
            String normalized = query.trim().toLowerCase(Locale.ROOT);
            courses = courses.stream()
                    .filter(c -> (c.code() != null && c.code().toLowerCase(Locale.ROOT).contains(normalized))
                            || (c.name() != null && c.name().toLowerCase(Locale.ROOT).contains(normalized)))
                    .toList();
        }
        if (type != null && !type.isBlank() && !"ALL".equalsIgnoreCase(type.trim())) {
            String filterType = type.trim().toUpperCase(Locale.ROOT);
            courses = courses.stream()
                    .filter(c -> switch (filterType) {
                        case "CORE", "ELECTIVE", "GENERAL" -> filterType.equalsIgnoreCase(c.category());
                        case "READY" -> c.prerequisitesMet() && !c.completed();
                        case "UNCOMPLETED" -> !c.completed();
                        default -> true;
                    })
                    .toList();
        }
        if (season != null && !season.isBlank() && !"ALL".equalsIgnoreCase(season.trim())) {
            String targetSeason = season.trim().toUpperCase(Locale.ROOT);
            courses = courses.stream()
                    .filter(c -> "BOTH".equalsIgnoreCase(c.season()) || targetSeason.equalsIgnoreCase(c.season()))
                    .toList();
        }
        if (difficulty != null) {
            courses = courses.stream()
                    .filter(c -> c.difficulty() != null && c.difficulty().equals(difficulty))
                    .toList();
        }
        return ResponseEntity.ok(courses);
    }

    @GetMapping("/planner/courses/{courseId}")
    @ResponseBody
    public ResponseEntity<?> getCourseDetail(
            @PathVariable("courseId") Long courseId,
            HttpSession session) {
        Student student = authSessionHelper.resolveAuthenticatedStudent(session).orElse(null);
        if (student == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Session expired. Please log in again."));
        }

        CartDto cart = cartService.getCart(student.getStudentId());
        AvailableCourseDto course = buildAvailableCourses(student, cart).stream()
                .filter(c -> c.id().equals(courseId))
                .findFirst()
                .orElse(null);

        if (course == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Course not found: " + courseId));
        }

        List<Prerequisite> allPrerequisites = prerequisiteRepository.findAllWithCourses();
        Map<Long, List<Course>> prereqMap = allPrerequisites.stream()
                .filter(p -> p.getCourse() != null && p.getPrerequisiteCourse() != null)
                .collect(Collectors.groupingBy(
                        p -> p.getCourse().getId(),
                        Collectors.mapping(Prerequisite::getPrerequisiteCourse, Collectors.toList())
                ));

        List<String> chain = new ArrayList<>();
        collectPrerequisiteChain(courseId, prereqMap, new HashSet<>(), new LinkedHashSet<>(), chain);

        List<String> unlocks = allPrerequisites.stream()
                .filter(p -> p.getPrerequisiteCourse() != null
                        && courseId.equals(p.getPrerequisiteCourse().getId())
                        && p.getCourse() != null)
                .map(p -> p.getCourse().getCode())
                .distinct()
                .sorted()
                .toList();

        int diff = course.difficulty() != null ? course.difficulty() : 3;
        String difficultyLabel = formatDifficultyLabel(diff);
        String seasonLabel = formatSeasonLabel(course.season());
        String description = (course.description() != null && !course.description().isBlank())
                ? course.description()
                : "SDU curriculum course (" + course.code() + ").";

        CourseDetailDto detail = new CourseDetailDto(
                course.id(),
                course.code(),
                course.name(),
                course.credits(),
                diff,
                difficultyLabel,
                description,
                course.courseType(),
                course.category(),
                course.recommendedSemester(),
                seasonLabel,
                course.prerequisiteCodes(),
                chain,
                unlocks,
                course.completed(),
                course.enrolled(),
                course.inCart(),
                course.prerequisitesMet(),
                course.missingSummary()
        );
        return ResponseEntity.ok(detail);
    }

    private void collectPrerequisiteChain(
            Long courseId,
            Map<Long, List<Course>> prereqMap,
            Set<Long> visiting,
            Set<String> visitedCodes,
            List<String> orderedChain) {
        if (!visiting.add(courseId)) {
            return;
        }
        List<Course> direct = prereqMap.getOrDefault(courseId, List.of());
        for (Course req : direct) {
            if (req.getId() != null) {
                collectPrerequisiteChain(req.getId(), prereqMap, visiting, visitedCodes, orderedChain);
            }
            if (req.getCode() != null && visitedCodes.add(req.getCode())) {
                orderedChain.add(req.getCode());
            }
        }
        visiting.remove(courseId);
    }

    private String formatDifficultyLabel(int difficulty) {
        return switch (difficulty) {
            case 1 -> "1/5 — Very Light";
            case 2 -> "2/5 — Light";
            case 3 -> "3/5 — Moderate";
            case 4 -> "4/5 — Challenging";
            case 5 -> "5/5 — Very Challenging";
            default -> difficulty + "/5";
        };
    }

    private String formatSeasonLabel(String season) {
        if ("FALL".equalsIgnoreCase(season)) {
            return "Fall";
        }
        if ("SPRING".equalsIgnoreCase(season)) {
            return "Spring";
        }
        return "Fall & Spring";
    }

    protected List<AvailableCourseDto> buildAvailableCourses(Student student, CartDto cart) {
        List<Course> allCourses = courseRepository.findAll(Sort.by(Sort.Direction.ASC, "code"));

        Set<Long> inCartCourseIds = cart.items().stream()
                .map(item -> item.courseId())
                .collect(Collectors.toSet());

        Map<Long, CurriculumCourse> curriculumByCourseId = new HashMap<>();
        if (student.getMajor() != null && student.getCatalogYear() != null) {
            List<CurriculumCourse> curriculum = curriculumCourseRepository
                    .findByMajor_CodeAndCatalogYearOrderBySemesterAsc(student.getMajor().getCode(), student.getCatalogYear());
            for (CurriculumCourse cc : curriculum) {
                if (cc.getCourse() != null && cc.getCourse().getId() != null) {
                    curriculumByCourseId.put(cc.getCourse().getId(), cc);
                }
            }
        }

        List<Prerequisite> allPrerequisites = prerequisiteRepository.findAllWithCourses();
        Map<Long, List<Course>> prereqMap = allPrerequisites.stream()
                .filter(p -> p.getCourse() != null && p.getPrerequisiteCourse() != null)
                .collect(Collectors.groupingBy(
                        p -> p.getCourse().getId(),
                        Collectors.mapping(Prerequisite::getPrerequisiteCourse, Collectors.toList())
                ));

        Set<Long> completedIds = resolveCompletedCourses(student).keySet();

        Set<Long> enrolledIds = student.getEnrolledCourses() == null
                ? Set.of()
                : student.getEnrolledCourses().stream()
                        .map(Course::getId)
                        .collect(Collectors.toSet());

        Set<Long> satisfiedIds = new HashSet<>(completedIds);
        satisfiedIds.addAll(inCartCourseIds);

        return allCourses.stream()
                .map(c -> {
                    List<Course> prereqs = prereqMap.getOrDefault(c.getId(), List.of());
                    List<Course> missing = prereqs.stream()
                            .filter(p -> !satisfiedIds.contains(p.getId()))
                            .toList();
                    List<String> prereqCodes = prereqs.stream().map(Course::getCode).toList();
                    List<String> missingCodes = missing.stream().map(Course::getCode).toList();
                    boolean hasPrereqs = !prereqs.isEmpty();
                    boolean met = missing.isEmpty();
                    String summary = missing.stream()
                            .map(m -> m.getCode() + " (" + m.getName() + ")")
                            .collect(Collectors.joining(", "));
                    CurriculumCourse cc = curriculumByCourseId.get(c.getId());
                    String courseType = cc != null && cc.getCourseType() != null ? cc.getCourseType() : "ELECTIVE";
                    String category = resolveCourseCategory(c.getCode(), courseType);
                    String season = c.getSeason() != null && !c.getSeason().isBlank()
                            ? c.getSeason().toUpperCase(Locale.ROOT)
                            : "BOTH";
                    Integer recommendedSemester = cc != null ? cc.getSemester() : null;

                    return new AvailableCourseDto(
                            c.getId(),
                            c.getCode(),
                            c.getName(),
                            c.getCredits(),
                            c.getDifficulty() != null ? c.getDifficulty() : 3,
                            c.getDescription(),
                            courseType,
                            category,
                            season,
                            recommendedSemester,
                            completedIds.contains(c.getId()),
                            enrolledIds.contains(c.getId()),
                            inCartCourseIds.contains(c.getId()),
                            prereqCodes,
                            hasPrereqs,
                            met,
                            missingCodes,
                            summary
                    );
                })
                .toList();
    }

    private Map<Long, Course> resolveCompletedCourses(Student student) {
        Map<Long, Course> merged = new LinkedHashMap<>();
        if (student == null) {
            return merged;
        }
        if (student.getCompletedCourses() != null) {
            for (Course course : student.getCompletedCourses()) {
                if (course != null && course.getId() != null) {
                    merged.put(course.getId(), course);
                }
            }
        }
        if (student.getId() != null) {
            for (CourseCompletion completion : courseCompletionRepository.findForStudent(student.getId())) {
                Course course = completion.getCourse();
                if (course != null && course.getId() != null) {
                    merged.putIfAbsent(course.getId(), course);
                }
            }
        }
        return merged;
    }

    private String resolveCourseCategory(String code, String courseType) {
        if ("GENERAL".equalsIgnoreCase(courseType)) {
            return "GENERAL";
        }
        if (code != null && code.toUpperCase(Locale.ROOT).startsWith("MDE")) {
            return "GENERAL";
        }
        if ("CORE".equalsIgnoreCase(courseType)) {
            return "CORE";
        }
        return "ELECTIVE";
    }

    @PostMapping("/planner/cart/add")
    @ResponseBody
    public ResponseEntity<?> addCourseToCart(
            @RequestParam("courseId") Long courseId,
            HttpSession session) {

        String studentId = resolveStudentId(session);
        if (studentId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Session expired. Please log in again."));
        }

        try {
            CartDto updatedCart = cartService.addCourse(studentId, courseId);
            return ResponseEntity.ok(updatedCart);
        } catch (PrerequisiteNotMetException ex) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", ex.getMessage(),
                    "missingPrerequisites", ex.getMissingPrerequisites().stream().map(Course::getCode).toList()
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            log.error("Error adding course {} to cart", courseId, ex);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to add course to cart."));
        }
    }

    @RequestMapping(value = "/planner/cart/remove", method = {RequestMethod.DELETE, RequestMethod.POST})
    @ResponseBody
    public ResponseEntity<?> removeCourseFromCart(
            @RequestParam("courseId") Long courseId,
            HttpSession session) {

        String studentId = resolveStudentId(session);
        if (studentId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Session expired. Please log in again."));
        }

        try {
            CartDto updatedCart = cartService.removeCourse(studentId, courseId);
            return ResponseEntity.ok(updatedCart);
        } catch (Exception ex) {
            log.error("Error removing course {} from cart", courseId, ex);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to remove course from cart."));
        }
    }

    @GetMapping("/planner/cart")
    @ResponseBody
    public ResponseEntity<?> getCartData(HttpSession session) {
        String studentId = resolveStudentId(session);
        if (studentId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Session expired. Please log in again."));
        }

        return ResponseEntity.ok(cartService.getCart(studentId));
    }

    @PostMapping("/planner/cart/enroll")
    @ResponseBody
    public ResponseEntity<?> enrollCartCourses(HttpSession session) {
        String studentId = resolveStudentId(session);
        if (studentId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Session expired. Please log in again."));
        }

        try {
            List<EnrolledCourseDto> enrolledCourses = cartService.enrollCartCourses(studentId);
            int enrolledCredits = enrolledCourses.stream().mapToInt(EnrolledCourseDto::credits).sum();
            CartDto updatedCart = cartService.getCart(studentId);
            return ResponseEntity.ok(Map.of(
                    "cart", updatedCart,
                    "enrolledCourses", enrolledCourses,
                    "enrolledCredits", enrolledCredits,
                    "message", "Courses accepted! They are now listed as your Currently Enrolled Courses."
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            log.error("Error enrolling cart courses for student {}", studentId, ex);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to confirm course enrollment."));
        }
    }

    @PostMapping("/planner/enrolled/cancel")
    public Object cancelEnrolledCourse(
            @RequestParam("courseId") Long courseId,
            @RequestParam(value = "redirect", required = false) String redirect,
            HttpSession session) {

        String studentId = resolveStudentId(session);
        if (studentId == null) {
            if (redirect != null && !redirect.isBlank()) {
                return "redirect:/login";
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Session expired. Please log in again."));
        }

        try {
            List<EnrolledCourseDto> enrolledCourses = cartService.cancelEnrolledCourse(studentId, courseId);
            if (redirect != null && !redirect.isBlank()) {
                return "redirect:" + (redirect.startsWith("/") ? redirect : "/" + redirect);
            }
            int enrolledCredits = enrolledCourses.stream().mapToInt(EnrolledCourseDto::credits).sum();
            CartDto updatedCart = cartService.getCart(studentId);
            return ResponseEntity.ok(Map.of(
                    "cart", updatedCart,
                    "enrolledCourses", enrolledCourses,
                    "enrolledCredits", enrolledCredits
            ));
        } catch (Exception ex) {
            log.error("Error cancelling enrolled course {} for student {}", courseId, studentId, ex);
            if (redirect != null && !redirect.isBlank()) {
                return "redirect:" + (redirect.startsWith("/") ? redirect : "/" + redirect);
            }
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to cancel enrolled course."));
        }
    }

    @PostMapping("/planner/enrolled/return-to-cart")
    public Object returnEnrolledToCart(
            @RequestParam(value = "redirect", required = false) String redirect,
            HttpSession session) {

        String studentId = resolveStudentId(session);
        if (studentId == null) {
            if (redirect != null && !redirect.isBlank()) {
                return "redirect:/login";
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Session expired. Please log in again."));
        }

        try {
            CartDto updatedCart = cartService.returnEnrolledToCart(studentId);
            if (redirect != null && !redirect.isBlank()) {
                return "redirect:" + (redirect.startsWith("/") ? redirect : "/" + redirect);
            }
            List<EnrolledCourseDto> enrolledCourses = cartService.getEnrolledCourses(studentId);
            int enrolledCredits = enrolledCourses.stream().mapToInt(EnrolledCourseDto::credits).sum();
            return ResponseEntity.ok(Map.of(
                    "cart", updatedCart,
                    "enrolledCourses", enrolledCourses,
                    "enrolledCredits", enrolledCredits,
                    "message", "Enrolled courses returned to your Semester Cart for editing."
            ));
        } catch (Exception ex) {
            log.error("Error returning enrolled courses to cart for student {}", studentId, ex);
            if (redirect != null && !redirect.isBlank()) {
                return "redirect:" + (redirect.startsWith("/") ? redirect : "/" + redirect);
            }
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to return enrolled courses to cart."));
        }
    }

    private String resolveStudentId(HttpSession session) {
        return authSessionHelper.resolveStudentId(session);
    }
}
