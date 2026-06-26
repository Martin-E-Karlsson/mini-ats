package com.mek.miniats.screening;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

/**
 * Anthropic-backed implementation of {@link LlmClient}. If no API key is set the
 * feature degrades gracefully: it returns an explanatory message instead of
 * calling the API, so the app (and the demo) work without credentials.
 */
@Component
public class AnthropicLlmClient implements LlmClient {

    private final RestClient client;
    private final String model;
    private final boolean enabled;

    public AnthropicLlmClient(
            @Value("${anthropic.api-key:}") String apiKey,
            @Value("${anthropic.model:claude-haiku-4-5-20251001}") String model) {
        this.enabled = apiKey != null && !apiKey.isBlank();
        this.model = model;
        this.client = RestClient.builder()
                .baseUrl("https://api.anthropic.com")
                .defaultHeader("x-api-key", apiKey)
                .defaultHeader("anthropic-version", "2023-06-01")
                .defaultHeader("content-type", "application/json")
                .build();
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        if (!enabled) {
            return "AI screening is not configured (no ANTHROPIC_API_KEY set).\n\n"
                    + "How it would work: the candidate's CV text and the job description are sent to an "
                    + "LLM with a prompt asking for a concise, structured fit assessment — key strengths, "
                    + "gaps against the role, and a recommendation. Set ANTHROPIC_API_KEY to enable it.";
        }
        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "max_tokens", 600,
                    "system", systemPrompt,
                    "messages", List.of(Map.of("role", "user", "content", userPrompt)));

            AnthropicResponse response = client.post()
                    .uri("/v1/messages")
                    .body(body)
                    .retrieve()
                    .body(AnthropicResponse.class);

            return response == null ? "" : response.firstText();
        } catch (RestClientException e) {
            return "AI screening request failed: " + e.getMessage()
                    + " (check the API key, model name, and account credits).";
        }
    }
}
