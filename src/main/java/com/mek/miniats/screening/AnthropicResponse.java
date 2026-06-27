package com.mek.miniats.screening;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/** Minimal mapping of the Anthropic Messages API response. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AnthropicResponse(List<Content> content) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Content(String type, String text) {
    }

    /** The assistant's first text block, or empty if none. */
    public String firstText() {
        if (content == null) {
            return "";
        }
        return content.stream()
                .filter(c -> "text".equals(c.type()))
                .map(Content::text)
                .findFirst()
                .orElse("");
    }
}
