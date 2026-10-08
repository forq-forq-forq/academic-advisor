package kz.edu.sdu.advisor.controller;

import kz.edu.sdu.advisor.model.Student;
import kz.edu.sdu.advisor.model.StudentAccount;
import kz.edu.sdu.advisor.repository.StudentAccountRepository;
import kz.edu.sdu.advisor.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RegistrationControllerTest {

    private static final String EMAIL = "us01-registration@sdu.edu.kz";
    private static final String PASSWORD = "SecurePass123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private StudentAccountRepository studentAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanUpTestAccount() {
        studentAccountRepository.findByEmailIgnoreCase(EMAIL).ifPresent(studentAccountRepository::delete);
        studentRepository.findByEmailIgnoreCase(EMAIL).ifPresent(studentRepository::delete);
    }

    @Test
    void registerWithValidDetails_shouldPersistHashedPasswordAndRedirectToLogin() throws Exception {
        mockMvc.perform(post("/register")
                        .param("email", EMAIL)
                        .param("password", PASSWORD)
                        .param("confirmPassword", PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));

        StudentAccount savedAccount = studentAccountRepository.findByEmailIgnoreCase(EMAIL).orElseThrow();
        assertThat(savedAccount.getPasswordHash()).isNotEqualTo(PASSWORD);
        assertThat(passwordEncoder.matches(PASSWORD, savedAccount.getPasswordHash())).isTrue();
    }

    @Test
    void registerWithNonUniversityEmail_shouldShowValidationError() throws Exception {
        mockMvc.perform(post("/register")
                        .param("email", "student@example.com")
                        .param("password", PASSWORD)
                        .param("confirmPassword", PASSWORD))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors("registrationForm", "email"));

        assertThat(studentAccountRepository.findByEmailIgnoreCase("student@example.com")).isEmpty();
    }

    @Test
    void registerWithMismatchedPasswords_shouldShowValidationError() throws Exception {
        mockMvc.perform(post("/register")
                        .param("email", EMAIL)
                        .param("password", PASSWORD)
                        .param("confirmPassword", "DifferentPass123!"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors("registrationForm", "confirmPassword"));
    }

    @Test
    void registerWithWeakPassword_shouldShowValidationError() throws Exception {
        mockMvc.perform(post("/register")
                        .param("email", EMAIL)
                        .param("password", "weakpassword")
                        .param("confirmPassword", "weakpassword"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors("registrationForm", "password"));
    }

    @Test
    void registerWithAlreadyRegisteredEmail_shouldNotCreateAnotherAccount() throws Exception {
        StudentAccount existingAccount = new StudentAccount();
        existingAccount.setEmail(EMAIL);
        existingAccount.setPasswordHash(passwordEncoder.encode(PASSWORD));
        studentAccountRepository.save(existingAccount);

        mockMvc.perform(post("/register")
                        .param("email", EMAIL.toUpperCase())
                        .param("password", PASSWORD)
                        .param("confirmPassword", PASSWORD))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors("registrationForm", "email"));

        assertThat(studentAccountRepository.findAll().stream()
            .filter(account -> EMAIL.equalsIgnoreCase(account.getEmail())))
                .hasSize(1);
    }

        @Test
        void registerWithEmailOnExistingStudentRecord_shouldNotCreateAccount() throws Exception {
        Student existingStudent = new Student();
        existingStudent.setStudentId("existing-us01-student");
        existingStudent.setName("Existing Student");
        existingStudent.setEmail(EMAIL);
        studentRepository.save(existingStudent);

        mockMvc.perform(post("/register")
                .param("email", EMAIL)
                .param("password", PASSWORD)
                .param("confirmPassword", PASSWORD))
            .andExpect(status().isOk())
            .andExpect(view().name("register"))
            .andExpect(model().attributeHasFieldErrors("registrationForm", "email"));

        assertThat(studentAccountRepository.findByEmailIgnoreCase(EMAIL)).isEmpty();
        }
}