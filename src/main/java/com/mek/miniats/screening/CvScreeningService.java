package com.mek.miniats.screening;

import org.springframework.stereotype.Service;

/**
 * Builds a screening prompt from the candidate's CV and the role, and asks the
 * LLM for a concise fit assessment. Stripped-down on purpose — no CV parsing,
 * just pasted text — but the prompt and abstraction are production-shaped.
 */
@Service
public class CvScreeningService {

    private static final String SYSTEM_PROMPT = """
            You are a recruiting assistant helping screen candidates.
            Given a candidate's CV and a job, produce a short, structured assessment:
            - Fit score from 1 to 10
            - Key strengths for this role
            - Gaps or missing requirements
            - A one-line recommendation
            Be concise and objective. Do not invent facts not present in the CV.""";

    private final LlmClient llm;

    public CvScreeningService(LlmClient llm) {
        this.llm = llm;
    }

    public String screen(String candidateName, String jobTitle, String jobDescription, String cvText) {
        if (cvText == null || cvText.isBlank()) {
            throw new IllegalArgumentException("Paste the candidate's CV text to run a screening.");
        }
        String userPrompt = """
                Candidate: %s
                Role: %s
                Job description: %s

                CV:
                %s
                """.formatted(
                candidateName,
                jobTitle,
                jobDescription == null || jobDescription.isBlank() ? "(none provided)" : jobDescription,
                cvText);

        return llm.complete(SYSTEM_PROMPT, userPrompt);
    }
}
