package kz.edu.sdu.advisor.controller;

import jakarta.servlet.http.HttpSession;
import kz.edu.sdu.advisor.model.Course;
import kz.edu.sdu.advisor.model.Student;
import kz.edu.sdu.advisor.model.dto.AcademicStandingDto;
import kz.edu.sdu.advisor.model.dto.CompletedCourseDto;
import kz.edu.sdu.advisor.repository.CourseRepository;
import kz.edu.sdu.advisor.service.AuthSessionHelper;
import kz.edu.sdu.advisor.service.TranscriptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Serves the Profile & Academic Record page (/profile) and handles manual
 * transcript management (US-29, US-30).
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ProfileController {

    private static final List<String> SEMESTER_OPTIONS = List.of(
            "Fall 2023", "Spring 2024", "Fall 2024", "Spring 2025", "Fall 2025", "Spring 2026"
    );

    private final AuthSessionHelper authSessionHelper;
    private final TranscriptService transcriptService;
    private final CourseRepository courseRepository;

    @GetMapping("/profile")
    public String showProfile(HttpSession session, Model model) {
        Student student = authSessionHelper.resolveAuthenticatedStudent(session).orElse(null);
        if (student == null) {
            return "redirect:/login";
        }

        List<CompletedCourseDto> completedCourses = transcriptService.getCompletedCourses(student.getId());
        List<Course> allCourses = courseRepository.findAll(Sort.by(Sort.Direction.ASC, "code"));
        Map<String, Long> courseIdByCode = allCourses.stream()
                .collect(Collectors.toMap(Course::getCode, Course::getId, (a, b) -> a));

        AcademicStandingDto standing = AcademicStandingDto.of(student);

        model.addAttribute("student", student);
        model.addAttribute("standing", standing);
        model.addAttribute("completedCourses", completedCourses);
        model.addAttribute("allCourses", allCourses);
        model.addAttribute("courseIdByCode", courseIdByCode);
        model.addAttribute("supportedGrades", TranscriptService.SUPPORTED_GRADES);
        model.addAttribute("semesterOptions", SEMESTER_OPTIONS);
        return "profile";
    }

    @PostMapping("/profile/transcript/save")
    public String saveTranscriptCourse(
            @RequestParam("courseId") Long courseId,
            @RequestParam("letterGrade") String letterGrade,
            @RequestParam(value = "semesterTaken", required = false) String semesterTaken,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        String studentId = authSessionHelper.resolveStudentId(session);
        if (studentId == null) {
            return "redirect:/login";
        }

        try {
            CompletedCourseDto saved = transcriptService.saveCompletion(studentId, courseId, letterGrade, semesterTaken);
            redirectAttributes.addFlashAttribute("transcriptSuccess",
                    "Saved " + saved.code() + " (" + saved.letterGrade() + ") to your transcript.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("transcriptError", ex.getMessage());
        } catch (Exception ex) {
            log.error("Failed to save transcript entry for student {}", studentId, ex);
            redirectAttributes.addFlashAttribute("transcriptError", "Could not save transcript record.");
        }
        return "redirect:/profile";
    }

    @PostMapping("/profile/transcript/remove")
    public String removeTranscriptCourse(
            @RequestParam("courseId") Long courseId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        String studentId = authSessionHelper.resolveStudentId(session);
        if (studentId == null) {
            return "redirect:/login";
        }

        try {
            transcriptService.removeCompletion(studentId, courseId);
            redirectAttributes.addFlashAttribute("transcriptSuccess", "Course removed from your completed history.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("transcriptError", ex.getMessage());
        }
        return "redirect:/profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(
            @RequestParam(value = "currentSemester", required = false) Integer currentSemester,
            @RequestParam(value = "careerGoal", required = false) String careerGoal,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        String studentId = authSessionHelper.resolveStudentId(session);
        if (studentId == null) {
            return "redirect:/login";
        }

        try {
            transcriptService.updateAcademicProfile(studentId, currentSemester, careerGoal);
            redirectAttributes.addFlashAttribute("profileSuccess", "Academic profile updated.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("transcriptError", ex.getMessage());
        }
        return "redirect:/profile";
    }
}

