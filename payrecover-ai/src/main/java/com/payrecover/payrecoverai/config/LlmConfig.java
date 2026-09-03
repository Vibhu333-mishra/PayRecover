package com.payrecover.payrecoverai.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * ALL LLM SETTINGS LIVE HERE. Nothing about the AI provider is hardcoded
 * anywhere else in the codebase.
 *
 * WHAT AN "API" IS (short version, because you asked for beginner framing)
 * An API is just a URL you can send data to and get data back from, over HTTP,
 * the same way your browser talks to a website -- except the answer is JSON
 * instead of a web page. Groq's chat API is one such URL.
 *
 * WHY GROQ'S URL LOOKS LIKE ".../openai/v1"
 * Groq deliberately copies OpenAI's request/response format. That means this
 * one client class also works with OpenAI, OpenRouter, Together, or a local
 * Ollama server -- you would only change base-url, model and the API key. No
 * Java code changes. That is why we did not pull in any vendor SDK.
 *
 * WHERE THE VALUES COME FROM
 * @Value("${payrecover.llm.model}") reads a key from application.properties.
 * The API key line there is written as ${GROQ_API_KEY:} which means "read the
 * GROQ_API_KEY environment variable, and if it is missing, use empty string".
 * That is how the secret stays OUT of the repository. Never paste the key here.
 */
@Configuration
public class LlmConfig {

    private static final Logger log = LoggerFactory.getLogger(LlmConfig.class);

    /** Master switch. Set false to force fallback-rules mode for a demo. */
    @Value("${payrecover.llm.enabled:true}")
    private boolean enabled;

    @Value("${payrecover.llm.base-url}")
    private String baseUrl;

    @Value("${payrecover.llm.model}")
    private String model;

    @Value("${payrecover.llm.api-key:}")
    private String apiKey;

    /**
     * Temperature = how much randomness the model is allowed. 0.0 is as
     * deterministic as the model gets; 1.0+ is creative. For classification we
     * want boring and repeatable, so we keep it very low.
     */
    @Value("${payrecover.llm.temperature:0.2}")
    private double temperature;

    /** Upper bound on the length of the answer. Protects against runaway cost. */
    @Value("${payrecover.llm.max-tokens:1200}")
    private int maxTokens;

    /**
     * Asks the provider to guarantee valid JSON. Supported by Groq and OpenAI.
     * If you ever switch to a provider that rejects it, set this to false --
     * our parser can still handle plain text containing a JSON object.
     */
    @Value("${payrecover.llm.json-mode:true}")
    private boolean jsonMode;

    @Value("${payrecover.llm.connect-timeout-ms:5000}")
    private int connectTimeoutMs;

    @Value("${payrecover.llm.read-timeout-ms:25000}")
    private int readTimeoutMs;

    /**
     * RestClient is Spring's modern HTTP client (Spring 6.1+, so it ships with
     * Boot 3.2). It replaces the older RestTemplate and needs no extra
     * dependency.
     *
     * TIMEOUTS ARE THE IMPORTANT PART. Without them, a hung Groq request would
     * leave your /analyze endpoint spinning forever and the demo would look
     * broken. With them, the call gives up after 25 seconds, we catch the
     * error, and the fallback classifier answers instead.
     */
    @Bean
    public RestClient llmRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs); // time to open the socket
        factory.setReadTimeout(readTimeoutMs);       // time to wait for the answer

        return RestClient.builder()
                .requestFactory(factory)
                .baseUrl(baseUrl)
                .build();
    }

    /**
     * True only if the AI is switched on AND a key is actually present.
     * LlmClient checks this first so we never waste a network round-trip
     * discovering that the key is missing.
     */
    public boolean isUsable() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }

    /**
     * Runs once at startup. Printing this saves you a lot of confusion later --
     * you will know immediately whether the demo is running on real AI or on
     * fallback rules, instead of guessing from the UI.
     */
    @PostConstruct
    public void logStartupState() {
        if (!enabled) {
            log.warn("[LLM] Disabled by configuration. All diagnoses will use fallback rules.");
        } else if (apiKey == null || apiKey.isBlank()) {
            log.warn("[LLM] No API key found (env var GROQ_API_KEY is not set). "
                    + "Diagnoses will use fallback rules. This is a supported mode.");
        } else {
            log.info("[LLM] Ready. provider={} model={} jsonMode={}", baseUrl, model, jsonMode);
        }
    }

    // ===== Getters (no setters: config is read-only at runtime) =====

    public boolean isEnabled() {
        return enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getModel() {
        return model;
    }

    public String getApiKey() {
        return apiKey;
    }

    public double getTemperature() {
        return temperature;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public boolean isJsonMode() {
        return jsonMode;
    }
}
