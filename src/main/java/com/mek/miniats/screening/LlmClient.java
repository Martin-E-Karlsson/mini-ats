package com.mek.miniats.screening;

/** Minimal abstraction over a chat LLM, so the screening logic is testable and provider-agnostic. */
public interface LlmClient {

    String complete(String systemPrompt, String userPrompt);
}
