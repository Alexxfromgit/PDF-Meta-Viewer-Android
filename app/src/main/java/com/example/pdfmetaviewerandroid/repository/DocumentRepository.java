package com.example.pdfmetaviewerandroid.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import java.util.LinkedHashMap;
import java.util.Map;

/** Desktop-compatible JSON. SAF streams are owned and closed by the caller. */
public final class DocumentRepository {
    private static final ObjectMapper JSON = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private DocumentRepository() {}

    public static String toJson(DocumentRecord record, boolean includeContent) throws IOException {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("source", record.getSourceName());
        data.put("metadata", record.getMetadata());
        if (includeContent) {
            data.put("content", record.getContent());
            data.put("contentTruncated", record.isContentTruncated());
        }
        return JSON.writeValueAsString(data) + "\n";
    }
    public static void write(DocumentRecord record, OutputStream output, boolean includeContent) throws IOException {
        output.write(toJson(record, includeContent).getBytes(StandardCharsets.UTF_8));
        output.flush();
    }
    public static String suggestedExportName(String sourceName) {
        int dot = sourceName.lastIndexOf('.');
        return (dot > 0 ? sourceName.substring(0, dot) : sourceName) + "-metadata.json";
    }
    public static void storeMetadataRecord(DocumentRecord record, String filePath, boolean includeContent) throws IOException {
        Path target = Paths.get(filePath).toAbsolutePath();
        Path temporary = Files.createTempFile(target.getParent(), ".pdf-meta-viewer-", ".tmp");
        try {
            Files.write(temporary, toJson(record, includeContent).getBytes(StandardCharsets.UTF_8));
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
    /** The old hardcoded source-tree destination is not writable on Android. */
    @Deprecated public static void storeMetadataRecord(DocumentRecord record, boolean includeContent) {
        throw new IllegalArgumentException("Choose an export destination first.");
    }
}
