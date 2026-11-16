package com.example.pdfmetaviewerandroid.extractor;

import com.example.pdfmetaviewerandroid.repository.DocumentRecord;
import java.io.IOException;

/** Compatibility wrapper; the active screen uses the full immutable extraction result. */
@Deprecated
public final class ReadPdfMetadata {
    private final String path;
    public ReadPdfMetadata(String path) { this.path = path; }
    public String[] getPdfMetadata() throws IOException {
        try {
            DocumentRecord record = new PDFExtractor().getDocumentRecordFromImportedPDF(path);
            String[] keys = {"pdf:producer", "dc:title", "pdf:keywords", "dc:creator", "dc:description",
                    "dcterms:created", "dcterms:modified"};
            String[] result = new String[keys.length];
            for (int i = 0; i < keys.length; i++) result[i] = record.getMetadata().getOrDefault(keys[i], "");
            return result;
        } catch (IllegalArgumentException exception) {
            throw new IOException(exception.getMessage(), exception);
        }
    }
}
