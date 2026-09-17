package kz.edu.sdu.advisor.model.dto.gemini;

import java.util.List;

/**
 * Request body for the Gemini {@code generateContent} endpoint.
 */
public record GeminiRequest(List<GeminiContent> contents) {

    /**
     * Builds a single-turn request carrying a single text prompt.
     */
    public static GeminiRequest ofPrompt(String prompt) {
        return new GeminiRequest(List.of(new GeminiContent(List.of(new GeminiPart(prompt)))));
    }
}
