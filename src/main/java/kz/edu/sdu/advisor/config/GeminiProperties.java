package kz.edu.sdu.advisor.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * Configuration for the connection to the Gemini AI API.
 *
 * <p>The API key is intentionally the only property with no hardcoded default:
 * it must be supplied through the {@code GEMINI_API_KEY} environment variable
 * (see {@code application.properties}). Never commit a real key to source control.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "gemini.api")
public class GeminiProperties {

    /** Secret API key, injected from the GEMINI_API_KEY environment variable. */
    private String key;

    /** Base URL of the Gemini REST API (no trailing slash). */
    private String baseUrl = "https://generativelanguage.googleapis.com/v1beta";

    /** Gemini model to call, e.g. "gemini-3.6-flash". */
    private String model = "gemini-3.6-flash";

    /** Max time to establish a connection before failing. */
    private Duration connectTimeout = Duration.ofSeconds(5);

    /** Max time to wait for a response before failing. */
    private Duration readTimeout = Duration.ofSeconds(20);
}
