package kz.edu.sdu.advisor.controller;

import kz.edu.sdu.advisor.model.PasswordResetToken;
import kz.edu.sdu.advisor.repository.PasswordResetTokenRepository;
import kz.edu.sdu.advisor.repository.StudentAccountRepository;
import kz.edu.sdu.advisor.repository.StudentRepository;
import kz.edu.sdu.advisor.service.PasswordResetService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PasswordResetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private PasswordResetService passwordResetService;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private StudentAccountRepository studentAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("GET /login links to /forgot-password and /forgot-password renders publicly")
    void loginShowsForgotPasswordAndPageRenders() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Forgot your password?")))
                .andExpect(content().string(containsString("/forgot-password")));

        mockMvc.perform(get("/forgot-password"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Reset Password")));
    }

    @Test
    @DisplayName("POST /forgot-password creates token for existing student without revealing nonexistent accounts")
    void forgotPasswordAntiEnumerationAndSingleActiveToken() throws Exception {
        long countBefore = tokenRepository.count();

        mockMvc.perform(post("/forgot-password")
                        .param("identifier", "nonexistent-999999@sdu.edu.kz"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"forgot-success-msg\"")))
                .andExpect(content().string(not(containsString("id=\"dev-reset-link-box\""))));

        assertThat(tokenRepository.count()).isEqualTo(countBefore);

        mockMvc.perform(post("/forgot-password")
                        .param("identifier", "240103000"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"forgot-success-msg\"")))
                .andExpect(content().string(containsString("id=\"dev-reset-link-box\"")));

        var tokensFirst = tokenRepository.findByStudentIdAndUsedFalse("240103000");
        assertThat(tokensFirst).hasSize(1);
        String firstTokenValue = tokensFirst.get(0).getToken();

        mockMvc.perform(post("/forgot-password")
                        .param("identifier", "240103000@sdu.edu.kz"))
                .andExpect(status().isOk());

        assertThat(tokenRepository.findByToken(firstTokenValue).orElseThrow().isUsed()).isTrue();
        assertThat(tokenRepository.findByStudentIdAndUsedFalse("240103000")).hasSize(1);
    }

    @Test
    @DisplayName("Expired or unknown reset tokens are rejected on GET and POST /reset-password")
    void expiredOrInvalidTokenRejected() throws Exception {
        mockMvc.perform(get("/reset-password").param("token", "does-not-exist"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("invalid or has expired")))
                .andExpect(content().string(not(containsString("Update Password"))));

        String expiredTokenValue = passwordResetService.requestPasswordReset("240103000").orElseThrow();
        PasswordResetToken expired = tokenRepository.findByToken(expiredTokenValue).orElseThrow();
        expired.setExpiresAt(LocalDateTime.now().minusMinutes(5));
        tokenRepository.save(expired);

        mockMvc.perform(get("/reset-password").param("token", expiredTokenValue))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("invalid or has expired")));

        mockMvc.perform(post("/reset-password")
                        .param("token", expiredTokenValue)
                        .param("newPassword", "ValidNewPass99!@")
                        .param("confirmPassword", "ValidNewPass99!@"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("invalid or has expired")));
    }

    @Test
    @DisplayName("Valid reset updates password across Student and StudentAccount, invalidates token, and allows new login")
    void fullResetFlowUpdatesPasswordAndDisablesReuse() throws Exception {
        String token = passwordResetService.requestPasswordReset("240103000").orElseThrow();

        mockMvc.perform(get("/reset-password").param("token", token))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Update Password")));

        mockMvc.perform(post("/reset-password")
                        .param("token", token)
                        .param("newPassword", "weak")
                        .param("confirmPassword", "weak"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("12-64 characters")));

        mockMvc.perform(post("/reset-password")
                        .param("token", token)
                        .param("newPassword", "NewStrongPass99!@")
                        .param("confirmPassword", "Mismatch99!@"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("do not match")));

        mockMvc.perform(post("/reset-password")
                        .param("token", token)
                        .param("newPassword", "NewStrongPass99!@")
                        .param("confirmPassword", "NewStrongPass99!@"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?resetSuccess=true"));

        PasswordResetToken usedToken = tokenRepository.findByToken(token).orElseThrow();
        assertThat(usedToken.isUsed()).isTrue();

        var student = studentRepository.findByStudentId("240103000").orElseThrow();
        var account = studentAccountRepository.findByStudentId("240103000").orElseThrow();
        assertThat(passwordEncoder.matches("NewStrongPass99!@", student.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches("NewStrongPass99!@", account.getPasswordHash())).isTrue();

        mockMvc.perform(post("/reset-password")
                        .param("token", token)
                        .param("newPassword", "AnotherPass99!@")
                        .param("confirmPassword", "AnotherPass99!@"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("invalid or has expired")));

        mockMvc.perform(post("/login")
                        .param("studentId", "240103000")
                        .param("password", "NewStrongPass99!@"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/planner"));
    }
}
