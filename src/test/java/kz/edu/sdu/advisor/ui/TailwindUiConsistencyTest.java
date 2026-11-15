package kz.edu.sdu.advisor.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TailwindUiConsistencyTest {

    private static final Path TEMPLATES_DIR = Path.of("src/main/resources/templates");

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Local Tailwind utility stylesheet and theme config exist, avoid remote CDN, and load in all pages")
    void allPageTemplatesIncludeOfflineTailwindAssets() throws Exception {
        mockMvc.perform(get("/css/tailwind-utilities.css")).andExpect(status().isOk());
        mockMvc.perform(get("/js/tailwind-config.js")).andExpect(status().isOk());

        for (String page : List.of("login.html", "register.html", "registration-success.html",
                "dashboard.html", "planner.html", "profile.html", "faq.html", "advisor.html")) {
            String html = Files.readString(TEMPLATES_DIR.resolve(page));
            assertThat(html).contains("/css/tailwind-utilities.css");
            assertThat(html).contains("/js/tailwind-config.js");
            assertThat(html).doesNotContain("cdn.tailwindcss.com");
        }
    }

    @Test
    @DisplayName("Rendered HTML responses expose Tailwind utility classes and stylesheet link")
    void renderedPagesContainTailwindUtilities() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/css/tailwind-utilities.css")))
                .andExpect(content().string(containsString("antialiased")));

        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/css/tailwind-utilities.css")));

        for (String route : List.of("/dashboard", "/planner", "/profile", "/faq")) {
            mockMvc.perform(get(route).with(user("240103000").roles("STUDENT")))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("/css/tailwind-utilities.css")))
                    .andExpect(content().string(containsString("font-sans")));
        }

        mockMvc.perform(get("/advisor").with(user("advisor-1").roles("ADVISOR")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/css/tailwind-utilities.css")));
    }
}
