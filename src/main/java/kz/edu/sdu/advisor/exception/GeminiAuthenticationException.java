package kz.edu.sdu.advisor.exception;

/**
 * Thrown when the Gemini API rejects the request because the API key is
 * missing, invalid, or lacks permission (HTTP 401 / 403).
 */
public class GeminiAuthenticationException extends GeminiApiException {

    public GeminiAuthenticationException(String message) {
        super(message);
    }
}
