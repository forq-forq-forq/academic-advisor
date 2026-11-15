package kz.edu.sdu.advisor.security;

import kz.edu.sdu.advisor.model.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Test
    @DisplayName("Unauthenticated browser request redirects to /login")
    void unauthenticatedDashboardRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("Unauthenticated JSON/API request returns 401 JSON error")
    void unauthenticatedApiReturns401() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .contentType("application/json")
                        .content("{\"message\":\"hello\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    @Test
    @DisplayName("STUDENT can access /dashboard but receives 403 Forbidden on /advisor")
    void studentRbacBlocksAdvisorRoute() throws Exception {
        mockMvc.perform(get("/dashboard")
                        .with(user("240103000").roles("STUDENT")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/advisor")
                        .with(user("240103000").roles("STUDENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADVISOR and ADMIN roles can access /advisor portal")
    void advisorAndAdminCanAccessAdvisorRoute() throws Exception {
        mockMvc.perform(get("/advisor")
                        .with(user("advisor-1").roles("ADVISOR")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/advisor")
                        .with(user("admin-1").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("CSRF enforcement blocks mutating requests without CSRF token and allows valid token")
    void csrfBlocksMissingTokenAndAllowsValidToken() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("authenticatedStudentId", "240103000");

        mockMvc.perform(post("/profile/update")
                        .session(session)
                        .header("X-Enforce-Csrf", "true")
                        .param("currentSemester", "3")
                        .param("careerGoal", "Security Engineer"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/profile/update")
                        .session(session)
                        .with(csrf())
                        .header("X-Enforce-Csrf", "true")
                        .param("currentSemester", "3")
                        .param("careerGoal", "Backend Software Engineer"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"));
    }

    @Test
    @DisplayName("CustomUserDetailsService loads student by ID or email with ROLE_STUDENT")
    void userDetailsServiceLoadsStudentByIdAndEmail() {
        UserDetails byId = userDetailsService.loadUserByUsername("240103000");
        assertThat(byId.getUsername()).isEqualTo("240103000");
        assertThat(byId.getAuthorities()).extracting("authority")
                .containsExactly(Role.STUDENT.asAuthority());

        UserDetails byEmail = userDetailsService.loadUserByUsername("240103000@sdu.edu.kz");
        assertThat(byEmail.getUsername()).isEqualTo("240103000");

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("nonexistent-id"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    @DisplayName("Security headers are applied and login establishes Spring Security session cleared on logout")
    void securityHeadersAndLoginLogoutFlow() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "SAMEORIGIN"));

        MvcResult loginResult = mockMvc.perform(post("/login")
                        .param("studentId", "240103000")
                        .param("password", "Student123!@#"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/planner"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertThat(session).isNotNull();

        mockMvc.perform(get("/dashboard").session(session))
                .andExpect(status().isOk());

        mockMvc.perform(get("/logout").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        mockMvc.perform(get("/dashboard").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }
}
