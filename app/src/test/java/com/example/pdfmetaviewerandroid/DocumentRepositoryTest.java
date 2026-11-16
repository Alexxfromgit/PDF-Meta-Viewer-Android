package com.example.pdfmetaviewerandroid;

import com.example.pdfmetaviewerandroid.repository.DocumentRecord;
import com.example.pdfmetaviewerandroid.repository.DocumentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.junit.Assert.*;

public class DocumentRepositoryTest {
    private final ObjectMapper json = new ObjectMapper();
    @Test public void exportsTheDesktopEnvelopeWithOriginalNameAndEscapedValues() throws Exception {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("z-key", "quote \" and newline\n");
        values.put("dc:title", "Résumé 日本語");
        DocumentRecord record = new DocumentRecord("Original document.PDF", 42, values, "private body", false);
        String result = DocumentRepository.toJson(record, false);
        JsonNode actual = json.readTree(result);
        assertEquals(2, actual.size());
        assertEquals("Original document.PDF", actual.get("source").asText());
        assertEquals("quote \" and newline\n", actual.get("metadata").get("z-key").asText());
        assertEquals("Résumé 日本語", actual.get("metadata").get("dc:title").asText());
        assertFalse(actual.has("content"));
        assertTrue(result.indexOf("dc:title") < result.indexOf("z-key"));
        assertTrue(result.contains("\n"));
    }
    @Test public void contentIsAJsonFieldRatherThanAppendedRawText() throws Exception {
        DocumentRecord record = new DocumentRecord("sample.pdf", 1, new LinkedHashMap<>(), "body\n\"quoted\"", true);
        JsonNode actual = json.readTree(DocumentRepository.toJson(record, true));
        assertEquals("body\n\"quoted\"", actual.get("content").asText());
        assertTrue(actual.get("contentTruncated").asBoolean());
    }
    @Test public void exportFailuresPropagate() {
        OutputStream broken = new OutputStream() {
            @Override public void write(int value) throws IOException { throw new IOException("Disk full"); }
        };
        DocumentRecord record = new DocumentRecord(new LinkedHashMap<>(), "");
        assertThrows(IOException.class, () -> DocumentRepository.write(record, broken, false));
    }
    @Test public void recordCannotBeChangedByItsOriginalMapOrAConsumer() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("title", "first");
        DocumentRecord record = new DocumentRecord(values, "");
        values.put("title", "changed");
        assertEquals("first", record.getMetadata().get("title"));
        assertThrows(UnsupportedOperationException.class, () -> record.getMetadata().clear());
    }
    @Test public void exportNameMatchesDesktopConvention() {
        assertEquals("report.final-metadata.json", DocumentRepository.suggestedExportName("report.final.PDF"));
    }
}
