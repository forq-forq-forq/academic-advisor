package kz.edu.sdu.advisor.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Manual smoke check for the Gemini AI API connection.
 *
 * <p>Not part of the automated test suite - it makes a real network call and
 * costs quota, so it is not run automatically. To use it:
 * <ol>
 *   <li>Put a real key in the project's {@code .env} file: {@code GEMINI_API_KEY=...}</li>
 *   <li>Run: {@code mvnw.cmd test -Dtest=AiServiceManualCheck}</li>
 *   <li>Look at the console output.</li>
 * </ol>
 */
@SpringBootTest
class AiServiceManualCheck {

    @Autowired
    private AiService aiService;

    @Test
    void callGeminiAndPrintResponse() {
        String response = aiService.sendPrompt("Reply with exactly one short sentence confirming you received this message.");
        System.out.println("=== GEMINI RESPONSE ===");
        System.out.println(response);
        System.out.println("=======================");
    }
}
