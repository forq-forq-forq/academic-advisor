package kz.edu.sdu.advisor.exception;

/**
 * Base unchecked exception for any failure while talking to the Gemini AI API
 * (unexpected status codes, malformed/empty responses, etc.).
 */
public class GeminiApiException extends RuntimeException {

    public GeminiApiException(String message) {
        super(message);
    }

    public GeminiApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
