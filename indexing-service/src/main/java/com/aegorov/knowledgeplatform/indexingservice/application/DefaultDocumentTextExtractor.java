package com.aegorov.knowledgeplatform.indexingservice.application;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Component
public class DefaultDocumentTextExtractor implements DocumentTextExtractor {

    @Override
    public String extract(byte[] content, String contentType, String fileName) {
        String normalizedType = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        if ("application/pdf".equals(normalizedType) || fileName.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            return extractPdf(content);
        }
        return new String(content, StandardCharsets.UTF_8);
    }

    private String extractPdf(byte[] content) {
        try (PDDocument document = Loader.loadPDF(content)) {
            return new PDFTextStripper().getText(document);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to parse PDF document", ex);
        }
    }
}
