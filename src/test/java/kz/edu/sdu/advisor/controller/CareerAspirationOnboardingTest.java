package kz.edu.sdu.advisor.controller;

import kz.edu.sdu.advisor.model.Major;
import kz.edu.sdu.advisor.model.Student;
import kz.edu.sdu.advisor.model.StudentAccount;
import kz.edu.sdu.advisor.repository.CurriculumCourseRepository;
import kz.edu.sdu.advisor.repository.MajorRepository;
import kz.edu.sdu.advisor.repository.StudentAccountRepository;
import kz.edu.sdu.advisor.repository.StudentRepository;
import kz.edu.sdu.advisor.service.SemesterPlanService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CareerAspirationOnboardingTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MajorRepository majorRepository;

    @Autowired
    private CurriculumCourseRepository curriculumCourseRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private StudentAccountRepository studentAccountRepository;

    @Autowired
    private SemesterPlanService semesterPlanService;

    @Test
    @DisplayName("GET /register renders career aspiration quick-select preset chips")
    void registerFormRendersAspirationPresetChips() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"aspiration-presets\"")))
                .andExpect(content().string(containsString("AI / Machine Learning Engineer")))
                .andExpect(content().string(containsString("Cybersecurity Specialist")))
                .andExpect(content().string(containsString("Product &amp; Systems Analyst")));
    }

    @Test
    @DisplayName("Registration persists career goal on Student and StudentAccount and displays it on onboarding summary")
    void registrationPersistsCareerGoalAcrossEntitiesAndOnboardingSummary() throws Exception {
        Major major = majorRepository.findAll().get(0);
        Integer year = curriculumCourseRepository.findDistinctCatalogYears().get(0);

        mockMvc.perform(post("/register")
                        .param("email", "us03career@sdu.edu.kz")
                        .param("studentId", "240103777")
                        .param("fullName", "Amina Serik")
                        .param("facultyCode", major.getFaculty().getCode())
                        .param("majorCode", major.getCode())
                        .param("catalogYear", String.valueOf(year))
                        .param("careerGoal", "  Cybersecurity Specialist  ")
                        .param("password", "SecurePass123!@")
                        .param("confirmPassword", "SecurePass123!@"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/register/success"));

        StudentAccount account = studentAccountRepository.findByStudentId("240103777").orElseThrow();
        Student student = studentRepository.findByStudentId("240103777").orElseThrow();
        assertThat(account.getCareerGoal()).isEqualTo("Cybersecurity Specialist");
        assertThat(student.getCareerGoal()).isEqualTo("Cybersecurity Specialist");

        mockMvc.perform(get("/register/success")
                        .sessionAttr("registeredAccountId", account.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"onboarding-career-goal\"")))
                .andExpect(content().string(containsString("Cybersecurity Specialist")));
    }

    @Test
    @DisplayName("SemesterPlanService aligns career-matched electives without matching stopword-only overlaps")
    void semesterPlanMatchesExpandedOnboardingCareerGoals() {
        Student student = studentRepository.findByStudentId("240103000").orElseThrow();
        student.setCareerGoal("Cybersecurity Specialist");
        studentRepository.save(student);

        var plan = semesterPlanService.buildRecommendedPlan(student, "suggest elective courses for my career");
        assertThat(plan).isNotEmpty();
        assertThat(plan).anySatisfy(rec ->
                assertThat(rec.reason()).contains("career goal"));
    }
}
