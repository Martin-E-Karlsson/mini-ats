package com.mek.miniats.screening;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CvScreeningServiceTest {

    @Mock
    LlmClient llm;

    @InjectMocks
    CvScreeningService service;

    @Test
    void blankCvText_throws() {
        assertThatThrownBy(() -> service.screen("Ada", "Backend Engineer", "Java role", "   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void returnsModelOutput() {
        when(llm.complete(anyString(), anyString())).thenReturn("Strong match: 8/10");

        String result = service.screen("Ada", "Backend Engineer", "Java role", "10 years of Java");

        assertThat(result).isEqualTo("Strong match: 8/10");
    }

    @Test
    void promptIncludesCandidateJobAndCv() {
        when(llm.complete(anyString(), anyString())).thenReturn("ok");

        service.screen("Ada Lovelace", "Backend Engineer", "Build APIs in Java", "Spring Boot expert");

        ArgumentCaptor<String> userPrompt = ArgumentCaptor.forClass(String.class);
        verify(llm).complete(anyString(), userPrompt.capture());
        assertThat(userPrompt.getValue())
                .contains("Ada Lovelace")
                .contains("Backend Engineer")
                .contains("Build APIs in Java")
                .contains("Spring Boot expert");
    }
}
