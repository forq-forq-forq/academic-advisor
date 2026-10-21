package kz.edu.sdu.advisor.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FaqController {

    @GetMapping("/faq")
    public String faqPage(Model model) {
        model.addAttribute("pageTitle", "Academic FAQ & Regulations");
        return "faq";
    }
}
