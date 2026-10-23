package kz.edu.sdu.advisor.controller;

import jakarta.servlet.http.HttpSession;
import kz.edu.sdu.advisor.model.Student;
import kz.edu.sdu.advisor.model.dto.AcademicStandingDto;
import kz.edu.sdu.advisor.model.dto.CompletedCourseDto;
import kz.edu.sdu.advisor.model.dto.CourseRecommendationDto;
import kz.edu.sdu.advisor.repository.CourseCompletionRepository;
import kz.edu.sdu.advisor.service.AcademicContextService;
import kz.edu.sdu.advisor.service.AiService;
import kz.edu.sdu.advisor.service.AuthSessionHelper;
import kz.edu.sdu.advisor.service.CumulativeGpaService;
import kz.edu.sdu.advisor.service.SemesterPlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

/**
 * Serves the student dashboard and handles chat message processing.
 *
 * <p>GET  /dashboard      — renders the dashboard page (requires session).
 * <p>POST /dashboard/chat — accepts a student message, calls AiService,
 *                           and returns the AI reply as JSON for the frontend.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final AuthSessionHelper authSessionHelper;
    private final AiService aiService;
    private final CourseCompletionRepository courseCompletionRepository;
    private final CumulativeGpaService cumulativeGpaService;
    private final AcademicContextService academicContextService;
    private final SemesterPlanService semesterPlanService;

    @GetMapping("/dashboard")
    public String showDashboard(HttpSession session, Model model) {
        Student student = authSessionHelper.resolveAuthenticatedStudent(session).orElse(null);
        if (student == null) {
            return "redirect:/login";
        }

        var completedCourses = courseCompletionRepository.findForStudent(student.getId())
                .stream().map(CompletedCourseDto::of).toList();
        AcademicStandingDto academicStanding = AcademicStandingDto.of(student,
                cumulativeGpaService.calculate(completedCourses));
        model.addAttribute("student", student);
        model.addAttribute("standing", academicStanding);
        model.addAttribute("enrolledCourses", academicStanding.enrolledCourses());
        model.addAttribute("completedCourses", completedCourses);
        return "dashboard";
    }

    /**
     * Processes a chat message submitted via AJAX.
     *
     * @param message the student's query text
     * @param session the current HTTP session (must be authenticated)
     * @return JSON object with either {@code reply} (AI text) or {@code error}
     */
    @PostMapping({"/dashboard/chat", "/api/chat"})
    @ResponseBody
    public ResponseEntity<Map<String, Object>> chat(
            @RequestParam("message") String message,
            @RequestParam(value = "page", required = false) String page,
            HttpSession session) {

        Student student = authSessionHelper.resolveAuthenticatedStudent(session).orElse(null);
        if (student == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Session expired. Please log in again."));
        }

        String trimmed = message == null ? "" : message.trim();
        if (trimmed.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Message cannot be empty."));
        }

        boolean includePlan = semesterPlanService.shouldIncludeRecommendations(trimmed);
        List<CourseRecommendationDto> recommendations = includePlan
                ? semesterPlanService.buildRecommendedPlan(student, trimmed)
                : List.of();

        try {
            String contextualPrompt = academicContextService.buildContextualPrompt(student, trimmed, page);
            String reply = aiService.sendPrompt(contextualPrompt);
            if (reply == null || reply.isBlank()) {
                reply = semesterPlanService.buildSmartFallbackReply(student, trimmed, page, recommendations);
            }
            return ResponseEntity.ok(Map.of(
                    "reply", reply,
                    "recommendations", recommendations
            ));
        } catch (Exception ex) {
            log.warn("Gemini API unavailable ({}), serving context-aware local advisor response", ex.getMessage());
            String fallbackReply = semesterPlanService.buildSmartFallbackReply(student, trimmed, page, recommendations);
            return ResponseEntity.ok(Map.of(
                    "reply", fallbackReply,
                    "recommendations", recommendations
            ));
        }
    }

    @PostMapping("/api/chat/plan")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> buildPlan(
            @RequestParam(value = "focus", required = false) String focus,
            HttpSession session) {

        Student student = authSessionHelper.resolveAuthenticatedStudent(session).orElse(null);
        if (student == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Session expired. Please log in again."));
        }

        String query = (focus == null || focus.isBlank()) ? "Build my next semester" : focus.trim();
        List<CourseRecommendationDto> recommendations = semesterPlanService.buildRecommendedPlan(student, query);
        String reply = semesterPlanService.buildFallbackAdvisorReply(student, recommendations);
        return ResponseEntity.ok(Map.of(
                "reply", reply,
                "recommendations", recommendations
        ));
    }
}
