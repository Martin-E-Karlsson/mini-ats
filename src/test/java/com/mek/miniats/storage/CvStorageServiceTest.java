package com.mek.miniats.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CvStorageServiceTest {

    @Mock
    SupabaseStorageClient storage;

    @InjectMocks
    CvStorageService service;

    @Test
    void store_uploadsToCvsBucketAndReturnsKey() {
        UUID candidateId = UUID.randomUUID();
        byte[] bytes = "pdf-bytes".getBytes();

        String key = service.store(candidateId, "Ada Lovelace CV.pdf", bytes, "application/pdf");

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(storage).upload(eq("cvs"), keyCaptor.capture(), eq(bytes), eq("application/pdf"));
        assertThat(key).isEqualTo(keyCaptor.getValue());
        assertThat(key).startsWith(candidateId.toString());
        // filename is sanitised: spaces become underscores, extension preserved
        assertThat(key).endsWith("Ada_Lovelace_CV.pdf");
    }

    @Test
    void store_sanitisesUnsafeFilenameCharacters() {
        UUID candidateId = UUID.randomUUID();

        String key = service.store(candidateId, "../weird name!.pdf", "x".getBytes(), "application/pdf");

        assertThat(key).doesNotContain("/");
        assertThat(key).doesNotContain(" ");
        assertThat(key).doesNotContain("!");
    }

    @Test
    void download_delegatesToClientWithBucket() {
        when(storage.download("cvs", "key123")).thenReturn("data".getBytes());
        assertThat(service.download("key123")).isEqualTo("data".getBytes());
    }
}
