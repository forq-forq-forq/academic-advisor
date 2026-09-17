package kz.edu.sdu.advisor.service;

import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import kz.edu.sdu.advisor.config.GeminiProperties;
import kz.edu.sdu.advisor.exception.GeminiApiException;
import kz.edu.sdu.advisor.exception.GeminiAuthenticationException;
import kz.edu.sdu.advisor.exception.GeminiTimeoutException;
import kz.edu.sdu.advisor.model.dto.gemini.GeminiContent;
import kz.edu.sdu.advisor.model.dto.gemini.GeminiPart;
import kz.edu.sdu.advisor.model.dto.gemini.GeminiRequest;
import kz.edu.sdu.advisor.model.dto.gemini.GeminiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Dedicated service for external communication with the Gemini AI API.
 *
 * <p>Responsible for sending prompts as properly serialized JSON requests,
 * deserializing the response, and extracting the relevant generated text.
 * The API key is never hardcoded here — it is read from {@link GeminiProperties},
 * which in turn is populated from the {@code GEMINI_API_KEY} environment variable.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private static final String GENERATE_CONTENT_PATH = "/models/{model}:generateContent";
    private static final String API_KEY_HEADER = "x-goog-api-key";

    private final RestClient geminiRestClient;
    private final GeminiProperties geminiProperties;

    /**
     * Sends a single text prompt to the Gemini API and returns the generated
     * text content.
     *
     * @param prompt the context/prompt to send
     * @return the text extracted from the model's response
     * @throws GeminiAuthenticationException if the API key is missing or the API rejects it (401/403)
     * @throws GeminiTimeoutException        if the API does not respond in time
     * @throws GeminiApiException            for any other failure (bad status, empty/malformed response)
     */
    public String sendPrompt(String prompt) {
        if (!StringUtils.hasText(geminiProperties.getKey())) {
            log.error("Gemini API key is not configured. Set the GEMINI_API_KEY environment variable.");
            throw new GeminiAuthenticationException("Missing Gemini API key.");
        }

        GeminiRequest requestBody = GeminiRequest.ofPrompt(prompt);

        try {
            GeminiResponse response = geminiRestClient.post()
                    .uri(GENERATE_CONTENT_PATH, geminiProperties.getModel())
                    .header(API_KEY_HEADER, geminiProperties.getKey())
                    .body(requestBody)
                    .retrieve()
                    .onStatus(status -> status.value() == 401 || status.value() == 403, (req, res) -> {
                        log.error("Gemini API authentication failed: HTTP {} - {}",
                                res.getStatusCode().value(), readBody(res));
                        throw new GeminiAuthenticationException(
                                "Gemini API authentication failed (HTTP " + res.getStatusCode().value()
                                        + "). Check that GEMINI_API_KEY is set and valid.");
                    })
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        log.error("Gemini API request failed: HTTP {} - {}",
                                res.getStatusCode().value(), readBody(res));
                        throw new GeminiApiException("Gemini API request failed with HTTP " + res.getStatusCode().value());
                    })
                    .body(GeminiResponse.class);

            log.info("Gemini API request succeeded with HTTP 200.");
            return extractText(response);
        } catch (ResourceAccessException | java.util.concurrent.CancellationException ex) {
            log.error("Gemini API did not respond in time.", ex);
            throw new GeminiTimeoutException("Gemini API did not respond in time.", ex);
        }
    }

    /**
     * Reads the raw error body for logging, without letting a read failure
     * hide the original error status.
     */
    private String readBody(ClientHttpResponse res) {
        try {
            return StreamUtils.copyToString(res.getBody(), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            return "<unable to read response body: " + ex.getMessage() + ">";
        }
    }

    /**
     * Pulls out just the generated text from a Gemini response, ignoring
     * everything else (safety ratings, finish reason, usage metadata, etc.).
     */
    private String extractText(GeminiResponse response) {
        if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
            throw new GeminiApiException("Gemini API returned no candidates.");
        }

        GeminiContent content = response.candidates().get(0).content();
        if (content == null || content.parts() == null || content.parts().isEmpty()) {
            throw new GeminiApiException("Gemini API response did not contain any text content.");
        }

        return content.parts().stream()
                .map(GeminiPart::text)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining());
    }
}
