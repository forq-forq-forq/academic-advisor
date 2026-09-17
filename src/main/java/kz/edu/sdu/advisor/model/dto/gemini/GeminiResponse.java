package kz.edu.sdu.advisor.model.dto.gemini;

import java.util.List;

/**
 * Response body returned by the Gemini {@code generateContent} endpoint.
 */
public record GeminiResponse(List<GeminiCandidate> candidates) {
}
