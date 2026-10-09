package kz.edu.sdu.advisor.controller;

import kz.edu.sdu.advisor.model.Faculty;
import kz.edu.sdu.advisor.model.Major;
import kz.edu.sdu.advisor.model.Student;
import kz.edu.sdu.advisor.model.StudentAccount;
import kz.edu.sdu.advisor.repository.FacultyRepository;
import kz.edu.sdu.advisor.repository.MajorRepository;
import kz.edu.sdu.advisor.repository.StudentAccountRepository;
import kz.edu.sdu.advisor.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RegistrationControllerTest {

    private static final String EMAIL = "us01-registration@sdu.edu.kz";
    private static final String PASSWORD = "SecurePass123!";
    private static final String FACULTY_CODE = "FE&NS";
    private static final String MAJOR_CODE = "CS";
    private static final String CATALOG_YEAR = "2024";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private StudentAccountRepository studentAccountRepository;

    @Autowired
    private FacultyRepository facultyRepository;

    @Autowired
    private MajorRepository majorRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanUpTestAccount() {
        studentRepository.findByEmailIgnoreCase(EMAIL).ifPresent(studentRepository::delete);
        studentAccountRepository.findByEmailIgnoreCase(EMAIL).ifPresent(studentAccountRepository::delete);
        studentRepository.findByStudentId("us01-registration").ifPresent(studentRepository::delete);
        studentAccountRepository.findByStudentId("us01-registration").ifPresent(studentAccountRepository::delete);
    }

    @Test
    void registerWithValidDetails_shouldPersistSelectionAndLoadCurriculum() throws Exception {
        MvcResult result = mockMvc.perform(post("/register")
                        .param("email", EMAIL)
                        .param("password", PASSWORD)
                        .param("confirmPassword", PASSWORD)
                        .param("facultyCode", FACULTY_CODE)
                        .param("majorCode", MAJOR_CODE)
                        .param("catalogYear", CATALOG_YEAR))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/register/success"))
                .andReturn();

        StudentAccount savedAccount = studentAccountRepository.findByEmailIgnoreCase(EMAIL).orElseThrow();
        assertThat(savedAccount.getPasswordHash()).isNotEqualTo(PASSWORD);
        assertThat(passwordEncoder.matches(PASSWORD, savedAccount.getPasswordHash())).isTrue();
        assertThat(savedAccount.getMajor().getCode()).isEqualTo(MAJOR_CODE);
        assertThat(savedAccount.getCatalogYear()).isEqualTo(2024);
        assertThat(savedAccount.getStudentId()).isEqualTo("us01-registration");

        Student savedStudent = studentRepository.findByStudentId("us01-registration").orElseThrow();
        assertThat(savedStudent.getEmail()).isEqualTo(EMAIL.toLowerCase());
        assertThat(savedStudent.getAccount().getId()).isEqualTo(savedAccount.getId());

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        mockMvc.perform(get("/register/success").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("registration-success"))
                .andExpect(model().attribute("curriculum", hasSize(32)))
                .andExpect(content().string(containsString("Your Student ID is")))
                .andExpect(content().string(containsString("us01-registration")))
                .andExpect(content().string(containsString("Semester")));
    }

    @Test
    void registerWithValidDetails_shouldAllowImmediateLoginWithExtractedStudentId() throws Exception {
        mockMvc.perform(post("/register")
                        .param("email", EMAIL)
                        .param("password", PASSWORD)
                        .param("confirmPassword", PASSWORD)
                        .param("facultyCode", FACULTY_CODE)
                        .param("majorCode", MAJOR_CODE)
                        .param("catalogYear", CATALOG_YEAR))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/register/success"));

        mockMvc.perform(post("/login")
                        .param("studentId", "us01-registration")
                        .param("password", PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/planner"))
                .andExpect(request().sessionAttribute("authenticatedStudentId", "us01-registration"));
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
    void registerWithMajorFromAnotherFaculty_shouldShowSelectionError() throws Exception {
        Faculty otherFaculty = facultyRepository.save(new Faculty("OTHER", "Other Faculty"));
        majorRepository.save(new Major("OTHER-CS", "Other Computer Science", otherFaculty, 240));

        mockMvc.perform(post("/register")
                        .param("email", EMAIL)
                        .param("password", PASSWORD)
                        .param("confirmPassword", PASSWORD)
                        .param("facultyCode", FACULTY_CODE)
                        .param("majorCode", "OTHER-CS")
                        .param("catalogYear", CATALOG_YEAR))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors("registrationForm", "majorCode"));

        assertThat(studentAccountRepository.findByEmailIgnoreCase(EMAIL)).isEmpty();
    }

    @Test
    void registerWithUnsupportedCatalogYear_shouldShowSelectionError() throws Exception {
        mockMvc.perform(post("/register")
                        .param("email", EMAIL)
                        .param("password", PASSWORD)
                        .param("confirmPassword", PASSWORD)
                        .param("facultyCode", FACULTY_CODE)
                        .param("majorCode", MAJOR_CODE)
                        .param("catalogYear", "2023"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors("registrationForm", "catalogYear"));

        assertThat(studentAccountRepository.findByEmailIgnoreCase(EMAIL)).isEmpty();
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