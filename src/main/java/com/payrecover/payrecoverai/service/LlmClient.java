package com.payrecover.payrecoverai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payrecover.payrecoverai.config.LlmConfig;
import com.payrecover.payrecoverai.exception.LlmUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * THE ONLY CLASS THAT TALKS TO THE INTERNET.
 *
 * 
 *
 * HOW IT CONNECTS
 *   AiDiagnosisService --> LlmClient --> (Groq) --> AiResponseParser
 * Every failure path here throws LlmUnavailableException, which is the signal
 * AiDiagnosisService uses to switch to FallbackClassifier.
 */
@Component
public class LlmClient {

    private static final Logger log = LoggerFactory.getLogger(LlmClient.class);

    private final LlmConfig config;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

     
    public LlmClient(LlmConfig config, RestClient llmRestClient, ObjectMapper objectMapper) {
        this.config = config;
        this.restClient = llmRestClient;
        this.objectMapper = objectMapper;
    }

    /**
     * @return the model's raw reply text (expected to be a JSON object)
     * @throws LlmUnavailableException on ANY problem, so the caller has exactly
     *         one thing to catch
     */
    public String complete(String systemPrompt, String userPrompt) {
        // Cheapest possible check first: do not open a socket if we have no key.
        if (!config.isUsable()) {
            throw new LlmUnavailableException(
                    "LLM not usable: either payrecover.llm.enabled=false or no API key is set.");
        }

        Map<String, Object> requestBody = buildRequestBody(systemPrompt, userPrompt);

        String rawResponse;
        try {
            rawResponse = restClient.post()
                    .uri("/chat/completions")
                    // "Bearer <key>" is the standard way APIs accept a token.
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .header("Content-Type", "application/json")
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);
        } catch (Exception ex) {
            // Covers timeouts, DNS failures, 401 bad key, 429 rate limit, 5xx.
            // We log the message but NOT the API key, and rethrow as our own type.
            log.warn("[LLM] Call to {} failed: {}", config.getBaseUrl(), ex.getMessage());
            throw new LlmUnavailableException("LLM request failed: " + ex.getMessage(), ex);
        }

        return extractContent(rawResponse);
    }

    private Map<String, Object> buildRequestBody(String systemPrompt, String userPrompt) {
        // LinkedHashMap keeps insertion order, which makes the body readable if
        // you ever print it while debugging.
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", config.getModel());
        body.put("temperature", config.getTemperature());
        body.put("max_tokens", config.getMaxTokens());

        if (config.isJsonMode()) {
            // Tells the provider to guarantee syntactically valid JSON.
            body.put("response_format", Map.of("type", "json_object"));
        }

        body.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        ));
        return body;
    }

    /**
     * Digs the answer out of the provider's envelope.
     *
     * The response looks like:
     *   { "choices": [ { "message": { "content": "{...our JSON...}" } } ], ... }
     *
     * We walk that path defensively -- path() returns a "missing node" instead
     * of null, so this cannot throw a NullPointerException even if the provider
     * changes its shape.
     */
    private String extractContent(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            throw new LlmUnavailableException("LLM returned an empty HTTP body.");
        }

        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            JsonNode choices = root.path("choices");

            if (!choices.isArray() || choices.isEmpty()) {
                throw new LlmUnavailableException(
                        "LLM response had no 'choices' array. Body: " + truncate(rawResponse));
            }

            String content = choices.get(0).path("message").path("content").asText("");
            if (content.isBlank()) {
                throw new LlmUnavailableException(
                        "LLM returned an empty message. This usually means max_tokens is too low.");
            }
            return content;

        } catch (LlmUnavailableException ex) {
            throw ex; // already the right type, do not double-wrap
        } catch (Exception ex) {
            throw new LlmUnavailableException("Could not read LLM response JSON.", ex);
        }
    }

    /** Keeps error logs readable when a provider returns a huge HTML error page. */
    private String truncate(String text) {
        return text.length() <= 300 ? text : text.substring(0, 300) + "...";
    }
}
