package kz.edu.sdu.advisor.exception;

/**
 * Thrown when the Gemini API does not respond within the configured
 * connect/read timeout.
 */
public class GeminiTimeoutException extends GeminiApiException {

    public GeminiTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
