package com.mek.miniats.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TokenResponse(
        @JsonProperty("access_token")  String accessToken,
        @JsonProperty("refresh_token") String refreshToken,
        User user
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record User(
            String id,
            String email
    ) {}
}