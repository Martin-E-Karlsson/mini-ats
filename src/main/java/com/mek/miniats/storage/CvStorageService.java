package com.mek.miniats.storage;

import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Stores candidate CVs in the "cvs" bucket. Keys are flat (no folders) so URI
 * encoding stays trivial: {candidateId}_{random}_{safeFilename}.
 */
@Service
public class CvStorageService {

    static final String BUCKET = "cvs";

    private final SupabaseStorageClient storage;

    public CvStorageService(SupabaseStorageClient storage) {
        this.storage = storage;
    }

    /** Uploads the CV and returns the storage key to persist on the candidate. */
    public String store(UUID candidateId, String filename, byte[] bytes, String contentType) {
        String safe = filename == null ? "cv" : filename.replaceAll("[^A-Za-z0-9._-]", "_");
        String key = candidateId + "_" + UUID.randomUUID() + "_" + safe;
        storage.upload(BUCKET, key, bytes, contentType);
        return key;
    }

    public byte[] download(String key) {
        return storage.download(BUCKET, key);
    }
}
