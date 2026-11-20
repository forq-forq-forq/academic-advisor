package kz.edu.sdu.advisor.controller;

import jakarta.servlet.http.HttpSession;
import kz.edu.sdu.advisor.model.Student;
import kz.edu.sdu.advisor.model.dto.AcademicStandingDto;
import kz.edu.sdu.advisor.model.dto.CompletedCourseDto;
import kz.edu.sdu.advisor.model.dto.GpaSimulationResultDto;
import kz.edu.sdu.advisor.service.AuthSessionHelper;
import kz.edu.sdu.advisor.service.CumulativeGpaService;
import kz.edu.sdu.advisor.service.GpaSimulationService;
import kz.edu.sdu.advisor.service.TranscriptService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
 * Serves the GPA Planner simulation workspace (/gpa) and live simulation API (US-25).
 */
@Controller
@RequiredArgsConstructor
public class GpaController {

    private final AuthSessionHelper authSessionHelper;
    private final GpaSimulationService gpaSimulationService;
    private final TranscriptService transcriptService;
    private final CumulativeGpaService cumulativeGpaService;

    @GetMapping("/gpa")
    public String showGpaPlanner(HttpSession session, Model model) {
        Student student = authSessionHelper.resolveAuthenticatedStudent(session).orElse(null);
        if (student == null) {
            return "redirect:/login";
        }

        List<CompletedCourseDto> completed = transcriptService.getCompletedCourses(student.getId());
        Double calculated = cumulativeGpaService.calculate(completed);
        Double effectiveGpa = calculated != null ? calculated : student.getGpa();
        AcademicStandingDto standing = AcademicStandingDto.of(student, effectiveGpa);

        GpaSimulationResultDto simulation = gpaSimulationService.simulate(student.getStudentId(), Map.of());

        model.addAttribute("student", student);
        model.addAttribute("standing", standing);
        model.addAttribute("simulation", simulation);
        model.addAttribute("supportedGrades", GpaSimulationService.GPA_LETTER_GRADES);
        return "gpa";
    }

    @PostMapping("/gpa/simulate")
    @ResponseBody
    public ResponseEntity<?> simulateGpa(
            @RequestParam Map<String, String> gradeParams,
            HttpSession session) {
        String studentId = authSessionHelper.resolveStudentId(session);
        if (studentId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Session expired. Please log in again."));
        }
        try {
            return ResponseEntity.ok(gpaSimulationService.simulate(studentId, gradeParams));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }
}
