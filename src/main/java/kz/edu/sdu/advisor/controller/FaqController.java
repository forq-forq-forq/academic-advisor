package kz.edu.sdu.advisor.controller;

import jakarta.servlet.http.HttpSession;
import kz.edu.sdu.advisor.service.AuthSessionHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class FaqController {

    private final AuthSessionHelper authSessionHelper;

    @GetMapping("/faq")
    public String faqPage(HttpSession session, Model model) {
        authSessionHelper.resolveAuthenticatedStudent(session)
                .ifPresent(student -> model.addAttribute("student", student));
        model.addAttribute("pageTitle", "Academic FAQ & Regulations");
        return "faq";
    }
}
