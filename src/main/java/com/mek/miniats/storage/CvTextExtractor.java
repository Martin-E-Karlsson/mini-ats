package com.mek.miniats.storage;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/** Extracts plain text from an uploaded CV (PDF via PDFBox, otherwise UTF-8 text). */
@Component
public class CvTextExtractor {

    public String extract(byte[] bytes, String filename) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        if (filename != null && filename.toLowerCase().endsWith(".pdf")) {
            try (PDDocument document = Loader.loadPDF(bytes)) {
                PDFTextStripper stripper = new PDFTextStripper();
                // Order text by on-page position instead of PDF content-stream order;
                // this fixes most of the jumbling on multi-column CV layouts.
                stripper.setSortByPosition(true);
                return stripper.getText(document);
            } catch (Exception e) {
                return ""; // unreadable PDF — fall back to empty, screening can still be pasted
            }
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
