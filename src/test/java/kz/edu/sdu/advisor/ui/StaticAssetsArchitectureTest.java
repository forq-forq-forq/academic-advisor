package kz.edu.sdu.advisor.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StaticAssetsArchitectureTest {

    private static final Path TEMPLATES_DIR = Path.of("src/main/resources/templates");
    private static final Path CSS_DIR = Path.of("src/main/resources/static/css");
    private static final Path JS_DIR = Path.of("src/main/resources/static/js");
    private static final Pattern SCRIPT_PATTERN = Pattern.compile("<script([^>]*)>(.*?)</script>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("All Thymeleaf templates contain no embedded <style> or inline <script> blocks")
    void templatesContainNoInlineStyleOrScriptBlocks() throws IOException {
        List<Path> templates;
        try (Stream<Path> walk = Files.walk(TEMPLATES_DIR)) {
            templates = walk.filter(p -> p.toString().endsWith(".html")).toList();
        }
        assertThat(templates).isNotEmpty();

        for (Path template : templates) {
            String html = Files.readString(template);
            assertThat(html)
                    .as("Template %s should not contain inline <style> tags", template)
                    .doesNotContainIgnoringCase("<style");

            Matcher matcher = SCRIPT_PATTERN.matcher(html);
            while (matcher.find()) {
                String attrs = matcher.group(1);
                String body = matcher.group(2);
                assertThat(attrs)
                        .as("Script tag in %s must reference external src/th:src", template)
                        .containsPattern("src=");
                assertThat(body.trim())
                        .as("External script tag in %s must have empty body", template)
                        .isEmpty();
            }
        }
    }

    @Test
    @DisplayName("Extracted CSS and JS static files exist on disk and are served without auth")
    void staticAssetsExistAndArePubliclyServed() throws Exception {
        for (String css : List.of("common.css", "auth.css", "registration-success.css",
                "dashboard.css", "planner.css", "profile.css", "faq.css", "copilot.css", "advisor.css")) {
            assertThat(Files.exists(CSS_DIR.resolve(css))).isTrue();
            mockMvc.perform(get("/css/" + css))
                    .andExpect(status().isOk());
        }

        for (String js : List.of("common.js", "login.js", "register.js",
                "planner.js", "profile.js", "faq.js", "copilot.js")) {
            assertThat(Files.exists(JS_DIR.resolve(js))).isTrue();
            mockMvc.perform(get("/js/" + js))
                    .andExpect(status().isOk());
        }
    }
}
