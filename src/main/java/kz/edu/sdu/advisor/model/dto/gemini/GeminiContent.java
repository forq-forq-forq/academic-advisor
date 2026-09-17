package kz.edu.sdu.advisor.model.dto.gemini;

import java.util.List;

/**
 * A block of content made up of one or more {@link GeminiPart}s.
 */
public record GeminiContent(List<GeminiPart> parts) {
}
