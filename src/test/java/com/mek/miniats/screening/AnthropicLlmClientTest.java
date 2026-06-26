package com.mek.miniats.screening;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AnthropicLlmClientTest {

    @Test
    void withoutApiKey_returnsNotConfiguredMessageInsteadOfCallingTheApi() {
        // Blank key => the feature must degrade gracefully (no network call, no crash).
        AnthropicLlmClient client = new AnthropicLlmClient("", "claude-haiku-4-5-20251001");

        String result = client.complete("system", "user");

        assertThat(result).containsIgnoringCase("not configured");
    }
}
