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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FaqControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("US-34 & US-35: GET /faq renders categories accordion and real-time search input")
    void getFaq_shouldRenderCategoriesAndSearchInput() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("authenticatedStudentId", "240103000");

        mockMvc.perform(get("/faq").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("faq"))
                .andExpect(content().string(containsString("Academic FAQ &amp; Regulations")))
                .andExpect(content().string(containsString("id=\"faqSearchInput\"")))
                .andExpect(content().string(containsString("GPA &amp; Grading")))
                .andExpect(content().string(containsString("Registration")))
                .andExpect(content().string(containsString("Retakes")))
                .andExpect(content().string(containsString("Summer School")))
                .andExpect(content().string(containsString("Scholarships")))
                .andExpect(content().string(containsString("btn-ask-ai-faq")))
                .andExpect(content().string(containsString("id=\"copilot-fab\"")))
                .andExpect(content().string(containsString("id=\"copilot-drawer\"")));
    }

    @Test
    @DisplayName("US-40: Floating AI Copilot widget is rendered on Dashboard, Planner, Profile, and FAQ")
    void floatingCopilotWidget_shouldRenderAcrossAllPages() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("authenticatedStudentId", "240103000");

        for (String path : new String[]{"/dashboard", "/planner", "/profile", "/faq"}) {
            mockMvc.perform(get(path).session(session))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("id=\"copilot-fab\"")))
                    .andExpect(content().string(containsString("id=\"copilot-drawer\"")))
                    .andExpect(content().string(containsString("id=\"quick-prompts\"")));
        }
    }
}

