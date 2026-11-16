package com.example.pdfmetaviewerandroid.repository;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;

/** Immutable result, safe to hand from the parsing thread to the UI and exporter. */
public final class DocumentRecord {
    private final String id = UUID.randomUUID().toString();
    private final String sourceName;
    private final long fileSize;
    private final Map<String, String> metadata;
    private final String content;
    private final boolean contentTruncated;

    public DocumentRecord(String sourceName, long fileSize, Map<String, String> metadata,
                          String content, boolean contentTruncated) {
        this.sourceName = Objects.requireNonNull(sourceName);
        this.fileSize = fileSize;
        this.metadata = Collections.unmodifiableMap(new TreeMap<>(Objects.requireNonNull(metadata)));
        this.content = content == null ? "" : content;
        this.contentTruncated = contentTruncated;
    }
    public DocumentRecord(Map<String, String> metadata, String content) {
        this("unknown.pdf", 0, metadata, content, false);
    }
    public String getId() { return id; }
    public String getSourceName() { return sourceName; }
    public long getFileSize() { return fileSize; }
    public Map<String, String> getMetadata() { return metadata; }
    public String getContent() { return content; }
    public boolean isContentTruncated() { return contentTruncated; }
}
