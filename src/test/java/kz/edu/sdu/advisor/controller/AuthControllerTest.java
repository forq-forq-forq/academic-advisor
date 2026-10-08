package kz.edu.sdu.advisor.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link AuthController}.
 *
 * <p>Covers User Story 4 (US-04: Student Authentication) QA scenarios:
 * <ul>
 *   <li>Scenario 1 (Pass) — valid Student ID and password → session created, redirect to /planner</li>
 *   <li>Scenario 2 (Fail) — incorrect password for existing Student ID → "Invalid Student ID or password", studentId retained</li>
 *   <li>Scenario 3 (Fail) — non-existent Student ID → "Invalid Student ID or password"</li>
 *   <li>Scenario 4 (Fail) — empty Student ID or password → client-side validation / rejection</li>
 *   <li>Scenario 5 (Fail) — unauthenticated access to /dashboard directly → redirect to /login</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    /** Valid Student ID seeded by {@link kz.edu.sdu.advisor.config.DataSeeder}. */
    private static final String VALID_STUDENT_ID = "240103000";

    /** Valid password corresponding to seeded demo student. */
    private static final String VALID_PASSWORD = "Student123!@#";

    /** An ID that does not exist in the database. */
    private static final String UNKNOWN_STUDENT_ID = "000000000";

    @Autowired
    private MockMvc mockMvc;

    // ──────────────────────────────────────────────────────────────────────────
    // Scenario 1: Given a valid Student ID and password, when the login form is
    // submitted, then a session is created and the user is redirected to the /planner page.
    // ──────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("Scenario 1 (Pass): Valid Student ID and password → session created, redirects to /planner")
    void loginWithValidCredentials_shouldRedirectToPlannerAndCreateSession() throws Exception {
        mockMvc.perform(post("/login")
                        .param("studentId", VALID_STUDENT_ID)
                        .param("password", VALID_PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/planner"))
                .andExpect(request().sessionAttribute("authenticatedStudentId", VALID_STUDENT_ID))
                .andExpect(request().sessionAttribute("authenticatedStudent", notNullValue()));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Scenario 2: Given an incorrect password for an existing Student ID, when login
    // is submitted, then an error message "Invalid Student ID or password" is shown
    // and no session is created (entered Student ID is retained).
    // ──────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("Scenario 2 (Fail): Incorrect password for existing Student ID → shows error and retains studentId")
    void loginWithWrongPassword_shouldShowInvalidCredentialsErrorAndRetainStudentId() throws Exception {
        mockMvc.perform(post("/login")
                        .param("studentId", VALID_STUDENT_ID)
                        .param("password", "WrongPassword!"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attribute("error", "Invalid Student ID or password"))
                .andExpect(model().attribute("studentId", VALID_STUDENT_ID))
                .andExpect(content().string(containsString("Invalid Student ID or password")))
                .andExpect(content().string(containsString(VALID_STUDENT_ID)))
                .andExpect(request().sessionAttributeDoesNotExist("authenticatedStudent"))
                .andExpect(request().sessionAttributeDoesNotExist("authenticatedStudentId"));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Scenario 3: Given a non-existent Student ID, when login is submitted, then an
    // error message "Invalid Student ID or password" is shown.
    // ──────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("Scenario 3 (Fail): Non-existent Student ID → shows error")
    void loginWithNonExistentId_shouldShowInvalidCredentialsError() throws Exception {
        mockMvc.perform(post("/login")
                        .param("studentId", UNKNOWN_STUDENT_ID)
                        .param("password", VALID_PASSWORD))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attribute("error", "Invalid Student ID or password"))
                .andExpect(model().attribute("studentId", UNKNOWN_STUDENT_ID))
                .andExpect(content().string(containsString("Invalid Student ID or password")))
                .andExpect(request().sessionAttributeDoesNotExist("authenticatedStudent"))
                .andExpect(request().sessionAttributeDoesNotExist("authenticatedStudentId"));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Scenario 4: Given empty Student ID or password fields, when login is submitted,
    // then client-side validation prevents submission and highlights required fields.
    // ──────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("Scenario 4 (Fail): Empty Student ID or password → rejected and login page contains client-side validation")
    void loginWithEmptyFields_shouldBeRejectedAndFormHasRequiredAttributes() throws Exception {
        // Form page verification for client-side validation (required attributes & IDs)
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"studentId\"")))
                .andExpect(content().string(containsString("id=\"password\"")))
                .andExpect(content().string(containsString("required")));

        // Server-side submission with empty studentId
        mockMvc.perform(post("/login")
                        .param("studentId", "")
                        .param("password", VALID_PASSWORD))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attribute("error", "Invalid Student ID or password"))
                .andExpect(request().sessionAttributeDoesNotExist("authenticatedStudent"));

        // Server-side submission with empty password
        mockMvc.perform(post("/login")
                        .param("studentId", VALID_STUDENT_ID)
                        .param("password", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attribute("error", "Invalid Student ID or password"))
                .andExpect(model().attribute("studentId", VALID_STUDENT_ID))
                .andExpect(request().sessionAttributeDoesNotExist("authenticatedStudent"));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Scenario 5: Given an unauthenticated user attempting to access /dashboard directly,
    // when navigating to the URL, then the system redirects them to the /login page.
    // ──────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("Scenario 5 (Fail): GET /dashboard without session → redirects to /login")
    void accessDashboardWithoutSession_shouldRedirectToLogin() throws Exception {
        MockHttpSession unauthenticatedSession = new MockHttpSession();

        mockMvc.perform(get("/dashboard").session(unauthenticatedSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("Unauthenticated user navigating directly to /planner → redirects to /login")
    void accessPlannerWithoutSession_shouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/planner"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("Already authenticated user navigating to /login → redirects to /planner")
    void alreadyAuthenticatedUser_shouldRedirectToPlanner() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("authenticatedStudentId", VALID_STUDENT_ID);

        mockMvc.perform(get("/login").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/planner"));
    }

    @Test
    @DisplayName("Logout invalidates session and blocks protected pages")
    void logout_shouldInvalidateSessionAndBlockProtectedPages() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("authenticatedStudentId", VALID_STUDENT_ID);

        mockMvc.perform(get("/logout").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        mockMvc.perform(get("/dashboard").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        mockMvc.perform(get("/planner").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }
}