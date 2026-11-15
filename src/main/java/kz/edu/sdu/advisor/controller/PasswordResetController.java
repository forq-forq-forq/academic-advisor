package kz.edu.sdu.advisor.controller;

import kz.edu.sdu.advisor.model.PasswordResetToken;
import kz.edu.sdu.advisor.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

/**
 * Self-service password recovery flow (/forgot-password and /reset-password) (US-05).
 */
@Controller
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    @Value("${advisor.auth.expose-reset-link:true}")
    private boolean exposeResetLink;

    @GetMapping("/forgot-password")
    public String showForgotPasswordPage() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String handleForgotPassword(
            @RequestParam(name = "identifier", required = false) String identifier,
            Model model) {
        if (!StringUtils.hasText(identifier)) {
            model.addAttribute("error", "Enter your Student ID or @sdu.edu.kz email.");
            return "forgot-password";
        }

        String trimmed = identifier.trim();
        Optional<String> tokenOpt = passwordResetService.requestPasswordReset(trimmed);
        model.addAttribute("identifier", trimmed);
        model.addAttribute("successMessage",
                "If an account matches \"" + trimmed + "\", password reset instructions are ready.");
        if (exposeResetLink && tokenOpt.isPresent()) {
            model.addAttribute("demoResetUrl", "/reset-password?token=" + tokenOpt.get());
        }
        return "forgot-password";
    }

    @GetMapping("/reset-password")
    public String showResetPasswordPage(
            @RequestParam(name = "token", required = false) String token,
            Model model) {
        Optional<PasswordResetToken> validToken = passwordResetService.validateResetToken(token);
        if (validToken.isEmpty()) {
            model.addAttribute("tokenInvalid", true);
            model.addAttribute("error", "This password reset link is invalid or has expired.");
        } else {
            model.addAttribute("tokenInvalid", false);
            model.addAttribute("token", validToken.get().getToken());
        }
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String handleResetPassword(
            @RequestParam(name = "token", required = false) String token,
            @RequestParam(name = "newPassword", required = false) String newPassword,
            @RequestParam(name = "confirmPassword", required = false) String confirmPassword,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            passwordResetService.resetPassword(token, newPassword, confirmPassword);
            redirectAttributes.addFlashAttribute("resetSuccessMessage",
                    "Your password has been reset successfully. You can now log in.");
            return "redirect:/login?resetSuccess=true";
        } catch (IllegalArgumentException ex) {
            boolean stillValid = passwordResetService.validateResetToken(token).isPresent();
            model.addAttribute("token", token);
            model.addAttribute("tokenInvalid", !stillValid);
            model.addAttribute("error", ex.getMessage());
            return "reset-password";
        }
    }
}
