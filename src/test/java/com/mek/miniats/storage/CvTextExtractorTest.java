package com.mek.miniats.storage;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class CvTextExtractorTest {

    private final CvTextExtractor extractor = new CvTextExtractor();

    @Test
    void plainTextFile_isReturnedAsString() {
        byte[] bytes = "10 years of Java".getBytes(StandardCharsets.UTF_8);
        assertThat(extractor.extract(bytes, "cv.txt")).isEqualTo("10 years of Java");
    }

    @Test
    void emptyBytes_returnEmptyString() {
        assertThat(extractor.extract(new byte[0], "cv.pdf")).isEmpty();
    }

    @Test
    void unreadablePdf_returnsEmptyStringInsteadOfThrowing() {
        // Not a real PDF; PDFBox should fail and we fall back to empty.
        assertThat(extractor.extract("not a pdf".getBytes(StandardCharsets.UTF_8), "cv.pdf")).isEmpty();
    }
}
