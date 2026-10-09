package kz.edu.sdu.advisor.controller;

import jakarta.servlet.http.HttpSession;
import kz.edu.sdu.advisor.exception.PrerequisiteNotMetException;
import kz.edu.sdu.advisor.model.Course;
import kz.edu.sdu.advisor.model.Prerequisite;
import kz.edu.sdu.advisor.model.Student;
import kz.edu.sdu.advisor.model.dto.AvailableCourseDto;
import kz.edu.sdu.advisor.model.dto.CartDto;
import kz.edu.sdu.advisor.repository.CourseRepository;
import kz.edu.sdu.advisor.repository.PrerequisiteRepository;
import kz.edu.sdu.advisor.repository.StudentRepository;
import kz.edu.sdu.advisor.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequiredArgsConstructor
public class PlannerController {

    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final PrerequisiteRepository prerequisiteRepository;
    private final CartService cartService;

    @GetMapping("/planner")
    public String showPlanner(HttpSession session, Model model) {
        String studentId = resolveStudentId(session);
        if (studentId == null) {
            return "redirect:/login";
        }

        Student student = studentRepository.findByStudentId(studentId).orElse(null);
        if (student == null) {
            session.invalidate();
            return "redirect:/login";
        }

        CartDto cart = cartService.getCart(studentId);
        List<Course> allCourses = courseRepository.findAll(Sort.by(Sort.Direction.ASC, "code"));

        Set<Long> inCartCourseIds = cart.items().stream()
                .map(item -> item.courseId())
                .collect(Collectors.toSet());

        List<Course> availableCourses = allCourses.stream()
                .filter(c -> !inCartCourseIds.contains(c.getId()))
                .toList();

        List<Prerequisite> allPrerequisites = prerequisiteRepository.findAllWithCourses();
        Map<Long, List<Course>> prereqMap = allPrerequisites.stream()
                .filter(p -> p.getCourse() != null && p.getPrerequisiteCourse() != null)
                .collect(Collectors.groupingBy(
                        p -> p.getCourse().getId(),
                        Collectors.mapping(Prerequisite::getPrerequisiteCourse, Collectors.toList())
                ));

        Set<Long> completedIds = student.getCompletedCourses() == null
                ? Set.of()
                : student.getCompletedCourses().stream()
                        .map(Course::getId)
                        .collect(Collectors.toSet());

        Set<Long> satisfiedIds = new java.util.HashSet<>(completedIds);
        satisfiedIds.addAll(inCartCourseIds);

        List<String> completedCodes = student.getCompletedCourses() == null
                ? List.of()
                : student.getCompletedCourses().stream().map(Course::getCode).toList();

        List<AvailableCourseDto> availableCourseDtos = availableCourses.stream()
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
                    return new AvailableCourseDto(
                            c.getId(),
                            c.getCode(),
                            c.getName(),
                            c.getCredits(),
                            prereqCodes,
                            hasPrereqs,
                            met,
                            missingCodes,
                            summary
                    );
                })
                .toList();

        model.addAttribute("student", student);
        model.addAttribute("cart", cart);
        model.addAttribute("availableCourses", availableCourseDtos);
        model.addAttribute("completedCourseCodes", completedCodes);

        return "planner";
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

    private String resolveStudentId(HttpSession session) {
        String studentId = (String) session.getAttribute("authenticatedStudentId");
        if (studentId == null && session.getAttribute("authenticatedStudent") instanceof Student s) {
            studentId = s.getStudentId();
        }
        return studentId;
    }
}

