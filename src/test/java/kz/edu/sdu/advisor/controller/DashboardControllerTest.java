package kz.edu.sdu.advisor.controller;

import kz.edu.sdu.advisor.service.AiService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DashboardControllerTest {

    private static final String VALID_STUDENT_ID = "240103000";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AiService aiService;

    @Test
    @DisplayName("Unauthenticated GET /dashboard should redirect to /login")
    void getDashboard_unauthenticated_shouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("US-07 QA-1: Authenticated GET /dashboard renders GPA card and Academic Standing badge")
    void getDashboard_authenticated_shouldRenderGpaAndStandingBadge() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("authenticatedStudentId", VALID_STUDENT_ID);

        mockMvc.perform(get("/dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("student", "standing"))
                .andExpect(content().string(containsString("Cumulative GPA")))
                .andExpect(content().string(containsString("3.80")))
                .andExpect(content().string(containsString("Honor Standing")));
    }

    @Test
    @DisplayName("US-07 QA-2 & QA-3: Authenticated GET /dashboard renders Credit Progress Ring and stats")
    void getDashboard_authenticated_shouldRenderCreditProgressRing() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("authenticatedStudentId", VALID_STUDENT_ID);

        mockMvc.perform(get("/dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Academic Standing")))
                .andExpect(content().string(containsString("27 / 240 ECTS completed")))
                .andExpect(content().string(containsString("11.2%")))
                .andExpect(content().string(containsString("213 ECTS")))
                .andExpect(content().string(containsString("14 ECTS")));
    }

    @Test
    @DisplayName("US-07 QA-4: Authenticated GET /dashboard renders major and faculty program details")
    void getDashboard_authenticated_shouldRenderProgramDetails() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("authenticatedStudentId", VALID_STUDENT_ID);

        mockMvc.perform(get("/dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Computer Science")))
                .andExpect(content().string(containsString("Engineering and Natural Sciences")));
    }

    @Test
    @DisplayName("POST /dashboard/chat handles AI assistant messages")
    void postChat_authenticated_shouldReturnAiReply() throws Exception {
        when(aiService.sendPrompt(anyString())).thenReturn("Here is your degree advice.");

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("authenticatedStudentId", VALID_STUDENT_ID);

        mockMvc.perform(post("/dashboard/chat")
                        .session(session)
                        .param("message", "What courses should I take next?"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply", is("Here is your degree advice.")));
    }
}

