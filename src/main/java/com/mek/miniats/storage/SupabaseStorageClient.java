package com.mek.miniats.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Thin client over the Supabase Storage REST API, authenticated with the
 * service-role key (server-side; storage RLS is bypassed, just like our DB
 * access). Object keys are single path segments (no '/') to keep URI encoding
 * simple.
 */
@Component
public class SupabaseStorageClient {

    private final RestClient client;

    public SupabaseStorageClient(
            @Value("${supabase.url}") String url,
            @Value("${supabase.service-role-key}") String serviceKey) {
        this.client = RestClient.builder()
                .baseUrl(url + "/storage/v1")
                .defaultHeader("apikey", serviceKey)
                .defaultHeader("Authorization", "Bearer " + serviceKey)
                .build();
    }

    public void upload(String bucket, String key, byte[] bytes, String contentType) {
        client.post()
                .uri("/object/{bucket}/{key}", bucket, key)
                .header("x-upsert", "true")
                .contentType(MediaType.parseMediaType(
                        contentType == null ? "application/octet-stream" : contentType))
                .body(bytes)
                .retrieve()
                .toBodilessEntity();
    }

    public byte[] download(String bucket, String key) {
        return client.get()
                .uri("/object/{bucket}/{key}", bucket, key)
                .retrieve()
                .body(byte[].class);
    }
}
