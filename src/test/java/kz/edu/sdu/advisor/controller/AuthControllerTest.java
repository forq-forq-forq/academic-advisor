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
 * <p>Covers authentication and session management scenarios:
 * <ul>
 *   <li>Scenario 1 (Pass) — valid Student ID → redirect to /dashboard</li>
 *   <li>Scenario 2 (Fail) — unregistered ID → "User not found" error on login page</li>
 *   <li>Scenario 3 (Fail) — direct /dashboard access without session → redirect to /login</li>
 *   <li>Scenario 4 (Pass) — logout invalidates session and blocks protected pages</li>
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
        MockHttpSession unauthenticatedSession = new MockHttpSession();

        mockMvc.perform(get("/dashboard").session(unauthenticatedSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Scenario 4 (Pass): Logout invalidates session
    // Given:  The student is authenticated.
    // When:   The student logs out.
    // Then:   The session is invalidated and protected pages are inaccessible.
    // ──────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("Scenario 4 (Pass): Logout invalidates session and blocks dashboard access")
    void logout_shouldInvalidateSessionAndBlockProtectedPage() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("authenticatedStudentId", VALID_STUDENT_ID);

        mockMvc.perform(get("/logout").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        mockMvc.perform(get("/dashboard").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }
}