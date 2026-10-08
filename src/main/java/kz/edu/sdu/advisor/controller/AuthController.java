package kz.edu.sdu.advisor.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import kz.edu.sdu.advisor.model.CurriculumCourse;
import kz.edu.sdu.advisor.model.Faculty;
import kz.edu.sdu.advisor.model.Major;
import kz.edu.sdu.advisor.model.Student;
import kz.edu.sdu.advisor.model.StudentAccount;
import kz.edu.sdu.advisor.model.dto.RegistrationForm;
import kz.edu.sdu.advisor.repository.CurriculumCourseRepository;
import kz.edu.sdu.advisor.repository.FacultyRepository;
import kz.edu.sdu.advisor.repository.MajorRepository;
import kz.edu.sdu.advisor.repository.StudentAccountRepository;
import kz.edu.sdu.advisor.repository.StudentRepository;
import kz.edu.sdu.advisor.service.StudentRegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Handles student authentication via Student ID.
 * On success, stores the authenticated student ID in the HTTP session.
 */
@Controller
@RequiredArgsConstructor
public class AuthController {

    private final StudentRepository studentRepository;
    private final StudentAccountRepository studentAccountRepository;
    private final FacultyRepository facultyRepository;
    private final MajorRepository majorRepository;
    private final CurriculumCourseRepository curriculumCourseRepository;
    private final StudentRegistrationService studentRegistrationService;

    @GetMapping("/login")
    public String showLoginPage(HttpSession session) {
        // If already logged in, redirect to dashboard
        if (session.getAttribute("authenticatedStudentId") != null) {
            return "redirect:/dashboard";
        }
        return "login";
    }

    @PostMapping("/login")
    public String processLogin(@RequestParam("studentId") String studentId,
                               HttpSession session,
                               Model model) {
        Optional<Student> studentOpt = studentRepository.findByStudentId(studentId.trim());

        if (studentOpt.isPresent()) {
            session.setAttribute("authenticatedStudentId", studentOpt.get().getStudentId());
            return "redirect:/dashboard";
        }

        model.addAttribute("error", "User not found");
        return "login";
    }

    @GetMapping("/register")
    public String showRegistrationPage(HttpSession session, Model model) {
        if (session.getAttribute("authenticatedStudentId") != null) {
            return "redirect:/dashboard";
        }
        model.addAttribute("registrationForm", new RegistrationForm());
        populateRegistrationOptions(model);
        return "register";
    }

    @PostMapping("/register")
    public String processRegistration(@Valid @ModelAttribute("registrationForm") RegistrationForm form,
                                      BindingResult result,
                                      HttpSession session,
                                      Model model) {
        if (session.getAttribute("authenticatedStudentId") != null) {
            return "redirect:/dashboard";
        }

        if (form.getPassword() != null && !form.getPassword().equals(form.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "password.mismatch", "Passwords do not match.");
        }
        if (!result.hasFieldErrors("email")) {
            String email = form.getEmail().trim();
            if (studentRepository.existsByEmailIgnoreCase(email)
                    || studentAccountRepository.existsByEmailIgnoreCase(email)) {
                result.rejectValue("email", "email.duplicate", "This email is already registered.");
            }
        }

        Faculty faculty = null;
        Major major = null;
        if (!result.hasFieldErrors("facultyCode")) {
            faculty = facultyRepository.findByCode(form.getFacultyCode()).orElse(null);
            if (faculty == null) {
                result.rejectValue("facultyCode", "faculty.invalid", "Select a valid faculty.");
            }
        }
        if (!result.hasFieldErrors("majorCode")) {
            major = majorRepository.findByCode(form.getMajorCode()).orElse(null);
            if (major == null) {
                result.rejectValue("majorCode", "major.invalid", "Select a valid major.");
            } else if (faculty != null && !major.getFaculty().getId().equals(faculty.getId())) {
                result.rejectValue("majorCode", "major.faculty.mismatch", "The selected major does not belong to this faculty.");
            }
        }
        if (major != null && !result.hasFieldErrors("catalogYear")) {
            List<CurriculumCourse> curriculum = curriculumCourseRepository
                    .findByMajor_CodeAndCatalogYearOrderBySemesterAsc(major.getCode(), form.getCatalogYear());
            Set<Integer> availableSemesters = curriculum.stream()
                    .map(CurriculumCourse::getSemester)
                    .collect(Collectors.toSet());
            Set<Integer> requiredSemesters = IntStream.rangeClosed(1, 8).boxed().collect(Collectors.toSet());
            if (curriculum.isEmpty()) {
                result.rejectValue("catalogYear", "curriculum.missing", "No curriculum is available for this major and catalog year.");
            } else if (!availableSemesters.containsAll(requiredSemesters)) {
                result.rejectValue("catalogYear", "curriculum.incomplete", "The curriculum for this selection is incomplete.");
            }
        }
        if (result.hasErrors()) {
            populateRegistrationOptions(model);
            return "register";
        }

        StudentAccount account = studentRegistrationService.register(form, major);
        session.setAttribute("registeredAccountId", account.getId());
        return "redirect:/register/success";
    }

    @GetMapping("/register/success")
    public String showRegistrationSuccess(HttpSession session, Model model) {
        Object accountId = session.getAttribute("registeredAccountId");
        if (!(accountId instanceof Long id)) {
            return "redirect:/login";
        }
        session.removeAttribute("registeredAccountId");

        StudentAccount account = studentAccountRepository.findById(id).orElse(null);
        if (account == null || account.getMajor() == null || account.getCatalogYear() == null) {
            return "redirect:/login";
        }
        List<CurriculumCourse> curriculum = curriculumCourseRepository
                .findByMajor_CodeAndCatalogYearOrderBySemesterAsc(account.getMajor().getCode(), account.getCatalogYear());
        model.addAttribute("account", account);
        model.addAttribute("curriculum", curriculum);
        return "registration-success";
    }

    private void populateRegistrationOptions(Model model) {
        model.addAttribute("faculties", facultyRepository.findAll(Sort.by("name")));
        model.addAttribute("majors", majorRepository.findAll(Sort.by("name")));
        model.addAttribute("catalogYears", curriculumCourseRepository.findDistinctCatalogYears());
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}
