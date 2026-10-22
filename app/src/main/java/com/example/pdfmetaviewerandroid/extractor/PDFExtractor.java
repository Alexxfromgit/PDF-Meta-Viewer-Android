package com.example.pdfmetaviewerandroid.extractor;

import com.example.pdfmetaviewerandroid.repository.DocumentRecord;
import com.example.pdfmetaviewerandroid.repository.DocumentRepository;
import com.tom_roush.pdfbox.cos.COSName;
import com.tom_roush.pdfbox.cos.COSBase;
import com.tom_roush.pdfbox.cos.COSString;
import com.tom_roush.pdfbox.cos.COSArray;
import com.tom_roush.pdfbox.io.MemoryUsageSetting;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDDocumentInformation;
import com.tom_roush.pdfbox.pdmodel.PDDocumentCatalog;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.common.PDMetadata;
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission;
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException;
import com.tom_roush.pdfbox.pdmodel.font.PDFont;
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import com.tom_roush.pdfbox.text.PDFTextStripper;
import com.tom_roush.pdfbox.util.Matrix;
import com.tom_roush.pdfbox.util.Vector;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Android-native PDF inspection, projected onto desktop's metadata keys and export schema. */
public final class PDFExtractor {
    public static final int TEXT_PREVIEW_LIMIT = 2_000_000;
    private static final int XMP_LIMIT = 2_000_000;

    public DocumentRecord extract(File file, String sourceName) throws PdfExtractionException {
        validate(file, sourceName);
        try (PDDocument document = PDDocument.load(file,
                MemoryUsageSetting.setupMixed(16 * 1024 * 1024).setTempDir(file.getParentFile()))) {
            checkInterrupted();
            Map<String, String> metadata = new TreeMap<>();
            PDDocumentCatalog catalog = document.getDocumentCatalog();
            PDDocumentInformation info = document.getDocumentInformation();
            metadata.put("Content-Type", "application/pdf");
            String version = String.format(Locale.ROOT, "%.1f", document.getVersion());
            metadata.put("pdf:PDFVersion", version);
            metadata.put("dc:format", "application/pdf; version=" + version);
            metadata.put("xmpTPg:NPages", Integer.toString(document.getNumberOfPages()));
            put(metadata, info.getTitle(), "pdf:docinfo:title", "dc:title");
            put(metadata, info.getAuthor(), "pdf:docinfo:creator", "dc:creator");
            put(metadata, info.getSubject(), "pdf:docinfo:subject", "dc:description");
            put(metadata, info.getKeywords(), "pdf:docinfo:keywords", "pdf:keywords");
            put(metadata, info.getCreator(), "pdf:docinfo:creator_tool", "xmp:CreatorTool");
            put(metadata, info.getProducer(), "pdf:docinfo:producer", "pdf:producer");
            put(metadata, date(info.getCreationDate()), "pdf:docinfo:created", "dcterms:created");
            put(metadata, date(info.getModificationDate()), "pdf:docinfo:modified", "dcterms:modified");
            put(metadata, info.getTrapped(), "pdf:docinfo:trapped");
            Set<String> standard = new java.util.HashSet<>(java.util.Arrays.asList("Title", "Author", "Subject", "Keywords", "Creator",
                    "Producer", "CreationDate", "ModDate", "Trapped"));
            for (COSName name : info.getCOSObject().keySet()) {
                if (standard.contains(name.getName())) continue;
                String value = customValue(info.getCOSObject().getDictionaryObject(name));
                put(metadata, value, name.getName(), "pdf:docinfo:custom:" + name.getName());
            }
            readXmp(catalog.getMetadata(), metadata);
            metadata.put("pdf:encrypted", Boolean.toString(document.isEncrypted()));
            AccessPermission permissions = document.getCurrentAccessPermission();
            metadata.put("access_permission:can_print", Boolean.toString(permissions.canPrint()));
            metadata.put("access_permission:can_print_faithful", Boolean.toString(permissions.canPrintFaithful()));
            metadata.put("access_permission:can_modify", Boolean.toString(permissions.canModify()));
            metadata.put("access_permission:modify_annotations", Boolean.toString(permissions.canModifyAnnotations()));
            metadata.put("access_permission:fill_in_form", Boolean.toString(permissions.canFillInForm()));
            metadata.put("access_permission:extract_content", Boolean.toString(permissions.canExtractContent()));
            metadata.put("access_permission:extract_for_accessibility", Boolean.toString(permissions.canExtractForAccessibility()));
            metadata.put("access_permission:assemble_document", Boolean.toString(permissions.canAssembleDocument()));
            metadata.put("pdf:hasXFA", Boolean.toString(catalog.getAcroForm() != null && catalog.getAcroForm().hasXFA()));
            metadata.put("pdf:hasCollection", Boolean.toString(catalog.getCOSObject().containsKey(COSName.getPDFName("Collection"))));
            metadata.put("pdf:hasMarkedContent", Boolean.toString(catalog.getStructureTreeRoot() != null
                    && catalog.getStructureTreeRoot().getCOSObject().containsKey(COSName.K)));
            Set<String> subtypes = new java.util.TreeSet<>();
            Set<String> types = new java.util.TreeSet<>();
            int threeDimensional = 0;
            for (PDPage page : document.getPages()) {
                checkInterrupted();
                for (PDAnnotation annotation : page.getAnnotations()) {
                    subtypes.add(String.valueOf(annotation.getSubtype()));
                    // Tika's annotationTypes key contains /NM annotation names, not /Type.
                    types.add(String.valueOf(annotation.getAnnotationName()));
                    if ("3D".equals(annotation.getSubtype())) threeDimensional++;
                }
            }
            if (!subtypes.isEmpty()) metadata.put("pdf:annotationSubtypes", String.join(", ", subtypes));
            if (!types.isEmpty()) metadata.put("pdf:annotationTypes", String.join(", ", types));
            metadata.put("pdf:num3DAnnotations", Integer.toString(threeDimensional));

            BoundedPreview preview = new BoundedPreview(TEXT_PREVIEW_LIMIT);
            DiagnosticsStripper stripper = new DiagnosticsStripper();
            stripper.writeText(document, preview);
            stripper.addMetadata(metadata);
            // Android does not run an external OCR process. Image-only pages have no text.
            metadata.put("pdf:ocrPageCount", "0");
            return new DocumentRecord(sourceName, file.length(), metadata, preview.toString(), preview.truncated);
        } catch (InvalidPasswordException exception) {
            throw new PdfExtractionException("The PDF is password-protected and could not be opened.", exception);
        } catch (InterruptedIOException exception) {
            throw new PdfExtractionException("PDF analysis was interrupted.", exception);
        } catch (IOException | RuntimeException exception) {
            throw new PdfExtractionException("The file is damaged, unreadable, or not a supported PDF.", exception);
        }
    }

    private static void validate(File file, String sourceName) throws PdfExtractionException {
        if (file == null || !file.isFile()) throw new PdfExtractionException("The selected file no longer exists.");
        if (!file.canRead()) throw new PdfExtractionException("The selected PDF is not readable.");
        if (sourceName == null || !sourceName.toLowerCase(Locale.ROOT).endsWith(".pdf"))
            throw new PdfExtractionException("Only PDF files can be analyzed.");
        try (InputStream input = new FileInputStream(file)) {
            byte[] header = new byte[1024];
            int count = input.read(header);
            if (count <= 0 || !new String(header, 0, count, StandardCharsets.ISO_8859_1).contains("%PDF-"))
                throw new PdfExtractionException("The selected file is not a valid PDF.");
        } catch (IOException exception) {
            throw new PdfExtractionException("The selected PDF could not be opened.", exception);
        }
    }
    private static void readXmp(PDMetadata xmp, Map<String, String> values) {
        values.put("pdf:hasXMP", Boolean.toString(xmp != null));
        if (xmp == null) return;
        try (InputStream input = xmp.createInputStream()) {
            java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer, 0, Math.min(buffer.length, XMP_LIMIT + 1 - bytes.size()))) > 0) {
                checkInterrupted();
                bytes.write(buffer, 0, count);
                if (bytes.size() > XMP_LIMIT) break;
            }
            byte[] packet = bytes.toByteArray();
            if (packet.length > XMP_LIMIT) throw new IOException("XMP metadata exceeds the display limit.");
            // Reject all encodings of document types before parsing; the DOM check and
            // refusing entity resolver provide additional protection.
            String text = new String(packet, StandardCharsets.UTF_8).replace("\u0000", "");
            if (text.toUpperCase(Locale.ROOT).contains("<!DOCTYPE"))
                throw new IOException("XMP document types are not allowed.");
            XmpMetadata.read(packet, values);
        } catch (Exception exception) {
            values.put("pdf:metadataWarning", "XMP metadata could not be read.");
        }
    }
    private static String date(Calendar value) {
        return value == null ? null : DateTimeFormatter.ISO_INSTANT.format(value.toInstant().truncatedTo(ChronoUnit.SECONDS));
    }
    private static void put(Map<String, String> metadata, String value, String... keys) {
        if (value != null && !value.trim().isEmpty()) for (String key : keys) metadata.put(key, value);
    }
    private static String customValue(COSBase value) {
        if (value instanceof COSString) return ((COSString) value).getString();
        if (value instanceof COSName) return ((COSName) value).getName();
        if (value instanceof com.tom_roush.pdfbox.cos.COSNumber
                || value instanceof com.tom_roush.pdfbox.cos.COSBoolean) return value.toString().replaceAll("^COS\\w+\\{|}$", "");
        if (value instanceof COSArray) {
            List<String> entries = new ArrayList<>();
            for (COSBase item : (COSArray) value) {
                if (item instanceof COSString) entries.add(((COSString) item).getString());
            }
            return String.join(", ", entries);
        }
        return null;
    }
    private static void checkInterrupted() throws InterruptedIOException {
        if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("PDF analysis was interrupted.");
    }

    /** Bounds the result while allowing metadata diagnostics to finish for every page. */
    static final class BoundedPreview extends Writer {
        private final StringBuilder text = new StringBuilder();
        private final int limit;
        boolean truncated;
        BoundedPreview(int limit) { this.limit = limit; }
        @Override public void write(char[] buffer, int offset, int count) throws IOException {
            checkInterrupted();
            int remaining = limit - text.length();
            int keep = Math.min(remaining, count);
            if (keep > 0) text.append(buffer, offset, keep);
            if (count > keep) truncated = true;
        }
        @Override public void flush() {}
        @Override public void close() {}
        @Override public String toString() { return text.toString(); }
    }

    private static final class DiagnosticsStripper extends PDFTextStripper {
        private final List<String> characters = new ArrayList<>();
        private final List<String> unmapped = new ArrayList<>();
        private long totalCharacters;
        private long totalUnmapped;
        private int pageCharacters;
        private int pageUnmapped;
        private boolean damagedFont;
        private boolean nonEmbeddedFont;
        DiagnosticsStripper() throws IOException {}
        @Override protected void startPage(PDPage page) throws IOException {
            checkInterrupted();
            pageCharacters = 0;
            pageUnmapped = 0;
            super.startPage(page);
        }
        @Override protected void endPage(PDPage page) throws IOException {
            super.endPage(page);
            characters.add(Integer.toString(pageCharacters));
            unmapped.add(Integer.toString(pageUnmapped));
        }
        @Override protected void showGlyph(Matrix matrix, PDFont font, int code, String unicode, Vector displacement) throws IOException {
            checkInterrupted();
            pageCharacters++;
            totalCharacters++;
            if (unicode == null || unicode.isEmpty()) { pageUnmapped++; totalUnmapped++; }
            damagedFont |= font.isDamaged();
            nonEmbeddedFont |= !font.isEmbedded();
            super.showGlyph(matrix, font, code, unicode, displacement);
        }
        void addMetadata(Map<String, String> values) {
            values.put("pdf:charsPerPage", String.join(", ", characters));
            values.put("pdf:unmappedUnicodeCharsPerPage", String.join(", ", unmapped));
            values.put("pdf:totalUnmappedUnicodeChars", Long.toString(totalUnmapped));
            if (totalCharacters > 0) values.put("pdf:overallPercentageUnmappedUnicodeChars",
                    Double.toString((double) totalUnmapped / totalCharacters));
            values.put("pdf:containsDamagedFont", Boolean.toString(damagedFont));
            values.put("pdf:containsNonEmbeddedFont", Boolean.toString(nonEmbeddedFont));
        }
    }

    /** Compatibility entry points for callers of the original prototype. */
    public DocumentRecord getDocumentRecordFromImportedPDF(String path) {
        File file = new File(path);
        try { return extract(file, file.getName()); }
        catch (PdfExtractionException exception) { throw new IllegalArgumentException(exception.getMessage(), exception); }
    }
    public void storeDocumentRecordAs(DocumentRecord record, String path, boolean includeContent) {
        try { DocumentRepository.storeMetadataRecord(record, path, includeContent); }
        catch (IOException exception) { throw new IllegalStateException("The JSON file could not be saved.", exception); }
    }
    @Deprecated public void storeDocumentRecordAs(DocumentRecord record, boolean includeContent) {
        DocumentRepository.storeMetadataRecord(record, includeContent);
    }
}
