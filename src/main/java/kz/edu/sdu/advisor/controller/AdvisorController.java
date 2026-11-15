package kz.edu.sdu.advisor.controller;

import kz.edu.sdu.advisor.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Role-protected advisor workspace endpoint (T-06 RBAC verification).
 */
@Controller
@RequestMapping("/advisor")
@PreAuthorize("hasAnyRole('ADVISOR', 'ADMIN')")
@RequiredArgsConstructor
public class AdvisorController {

    private final StudentRepository studentRepository;

    @GetMapping
    public String showAdvisorPortal(Model model) {
        var students = studentRepository.findAll();
        model.addAttribute("studentCount", students.size());
        model.addAttribute("students", students);
        return "advisor";
    }
}
