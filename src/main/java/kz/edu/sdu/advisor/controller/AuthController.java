package kz.edu.sdu.advisor.controller;

import jakarta.servlet.http.HttpSession;
import kz.edu.sdu.advisor.model.Student;
import kz.edu.sdu.advisor.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
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

        model.addAttribute("error", "Student not found. Please check your Student ID.");
        return "login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}
