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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link AuthController}.
 *
 * <p>Covers all three QA scenarios defined in the Student Authentication (Mock) use case:
 * <ul>
 *   <li>Scenario 1 (Pass)  — valid Student ID → redirect to /dashboard</li>
 *   <li>Scenario 2 (Fail)  — unregistered ID  → "User not found" error on login page</li>
 *   <li>Scenario 3 (Fail)  — direct /dashboard access without session → redirect to /login</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    /** Valid Student ID seeded by {@link kz.edu.sdu.advisor.config.DataSeeder}. */
    private static final String VALID_STUDENT_ID = "240103000";

    /** An ID that does not exist in the database. */
    private static final String UNKNOWN_STUDENT_ID = "000000000";

    @Autowired
    private MockMvc mockMvc;

    // ──────────────────────────────────────────────────────────────────────────
    // Scenario 1 (Pass): Valid student ID login
    // Given:  The student is on the login page.
    // When:   The student enters a valid, existing Student ID and clicks Log In.
    // Then:   The system authenticates the user and redirects to /dashboard.
    // ──────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("Scenario 1 (Pass): Valid student ID → redirects to /dashboard and stores session")
    void loginWithValidId_shouldRedirectToDashboard() throws Exception {
        mockMvc.perform(post("/login")
                        .param("studentId", VALID_STUDENT_ID))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"))
                .andExpect(request().sessionAttribute("authenticatedStudentId", VALID_STUDENT_ID));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Scenario 2 (Fail): Unregistered ID error
    // Given:  The student is on the login page.
    // When:   The student enters an unregistered Student ID and submits.
    // Then:   The system displays "User not found" and remains on the login page.
    // ──────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("Scenario 2 (Fail): Unknown student ID → stays on login page with 'User not found'")
    void loginWithInvalidId_shouldShowUserNotFoundError() throws Exception {
        mockMvc.perform(post("/login")
                        .param("studentId", UNKNOWN_STUDENT_ID))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attributeExists("error"))
                .andExpect(model().attribute("error", "User not found"))
                .andExpect(content().string(containsString("User not found")));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Scenario 3 (Fail): Unauthorized direct URL access
    // Given:  The student is NOT authenticated.
    // When:   The student navigates directly to /dashboard.
    // Then:   The system blocks access and redirects to /login.
    // ──────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("Scenario 3 (Fail): GET /dashboard without session → redirects to /login")
    void accessDashboardWithoutSession_shouldRedirectToLogin() throws Exception {
        // Use a fresh empty session to guarantee no authenticated student attribute
        MockHttpSession unauthenticatedSession = new MockHttpSession();

        mockMvc.perform(get("/dashboard").session(unauthenticatedSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }
}
