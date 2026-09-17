package kz.edu.sdu.advisor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import kz.edu.sdu.advisor.config.GeminiProperties;
import kz.edu.sdu.advisor.exception.GeminiAuthenticationException;
import kz.edu.sdu.advisor.exception.GeminiTimeoutException;

/**
 * Unit tests for {@link AiService}, covering the three test scripts from the
 * "Gemini AI API Connection" user story:
 * <ul>
 *   <li>a successful HTTP 200 call returns the generated text;</li>
 *   <li>an unreachable/slow API results in a caught timeout exception;</li>
 *   <li>a missing or invalid API key (401) results in a caught authentication exception.</li>
 * </ul>
 *
 * <p>No real network call is made - HTTP responses are mocked, so this suite
 * runs in CI without needing a real {@code GEMINI_API_KEY}.
 */
class AiServiceTest {

    private static final String BASE_URL = "https://fake-gemini.test/v1beta";
    private static final String MODEL = "gemini-3.6-flash";

    private GeminiProperties properties;
    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        properties = new GeminiProperties();
        properties.setKey("test-api-key");
        properties.setBaseUrl(BASE_URL);
        properties.setModel(MODEL);
        properties.setConnectTimeout(Duration.ofMillis(200));
        properties.setReadTimeout(Duration.ofMillis(200));

        restClientBuilder = RestClient.builder().baseUrl(BASE_URL);
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
    }

    private AiService newService() {
        return new AiService(restClientBuilder.build(), properties);
    }

    @Test
    void sendPrompt_returnsGeneratedText_onHttp200() {
        String responseBody = """
                {
                  "candidates": [
                    { "content": { "parts": [ { "text": "Take CS201 next semester." } ] } }
                  ]
                }
                """;

        mockServer.expect(requestTo(BASE_URL + "/models/" + MODEL + ":generateContent"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "test-api-key"))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        String result = newService().sendPrompt("What should I take next semester?");

        assertThat(result).isEqualTo("Take CS201 next semester.");
        mockServer.verify();
    }

    @Test
    void sendPrompt_throwsAuthenticationException_whenApiKeyIsMissing() {
        properties.setKey("");

        assertThatThrownBy(() -> newService().sendPrompt("test"))
                .isInstanceOf(GeminiAuthenticationException.class);

        // No HTTP call should have been made at all.
        mockServer.verify();
    }

    @Test
    void sendPrompt_throwsAuthenticationException_onHttp401() {
        mockServer.expect(requestTo(BASE_URL + "/models/" + MODEL + ":generateContent"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .body("{\"error\":{\"code\":401,\"message\":\"invalid api key\"}}")
                        .contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> newService().sendPrompt("test"))
                .isInstanceOf(GeminiAuthenticationException.class);

        mockServer.verify();
    }

    @Test
    void sendPrompt_throwsTimeoutException_whenApiIsUnreachable() {
        // Point at a non-routable address, with a short connect timeout wired
        // the same way GeminiClientConfig wires it, so the real request
        // factory fires a genuine ResourceAccessException quickly.
        properties.setBaseUrl("http://10.255.255.1");
        properties.setConnectTimeout(Duration.ofMillis(300));

        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
                .withConnectTimeout(properties.getConnectTimeout())
                .withReadTimeout(properties.getReadTimeout());
        ClientHttpRequestFactory requestFactory = ClientHttpRequestFactoryBuilder.detect().build(settings);

        RestClient timingOutClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                .build();
        AiService service = new AiService(timingOutClient, properties);

        assertThatThrownBy(() -> service.sendPrompt("test"))
                .isInstanceOf(GeminiTimeoutException.class)
                .satisfies(e -> {
                    assertThat(e.getCause()).isInstanceOfAny(ResourceAccessException.class, java.util.concurrent.CancellationException.class);
                });
    }
}
