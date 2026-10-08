package kz.edu.sdu.advisor.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import kz.edu.sdu.advisor.model.Student;
import kz.edu.sdu.advisor.model.dto.RegistrationForm;
import kz.edu.sdu.advisor.repository.StudentAccountRepository;
import kz.edu.sdu.advisor.repository.StudentRepository;
import kz.edu.sdu.advisor.service.StudentRegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

/**
 * Handles student authentication via Student ID.
 * On success, stores the authenticated student ID in the HTTP session.
 */
@Controller
@RequiredArgsConstructor
public class AuthController {

    private final StudentRepository studentRepository;
    private final StudentAccountRepository studentAccountRepository;
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
        return "register";
    }

    @PostMapping("/register")
    public String processRegistration(@Valid @ModelAttribute("registrationForm") RegistrationForm form,
                                      BindingResult result,
                                      HttpSession session) {
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
        if (result.hasErrors()) {
            return "register";
        }

        studentRegistrationService.register(form);
        return "redirect:/login?registered";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}
