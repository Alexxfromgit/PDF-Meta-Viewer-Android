package com.example.pdfmetaviewerandroid.extractor;

import com.example.pdfmetaviewerandroid.repository.DocumentRecord;
import com.example.pdfmetaviewerandroid.repository.DocumentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission;
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class PDFExtractorTest {
    @Before public void initializePdfBox() { PDFBoxResourceLoader.init(RuntimeEnvironment.getApplication()); }

    private File sample() throws Exception {
        File file = File.createTempFile("sample-", ".pdf", RuntimeEnvironment.getApplication().getCacheDir());
        try (InputStream input = getClass().getResourceAsStream("/Amazon-S3-Tutorial.pdf")) {
            Files.copy(input, file.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        return file;
    }
    @Test public void realPdfMatchesTheCurrentDesktopMetadataContract() throws Exception {
        File file = sample();
        try {
            DocumentRecord record = new PDFExtractor().extract(file, "Amazon-S3-Tutorial.pdf");
            assertFalse(record.getContent().trim().isEmpty());
            assertEquals(file.length(), record.getFileSize());
            assertFalse(record.isContentTruncated());
            ObjectMapper json = new ObjectMapper();
            JsonNode actual = json.readTree(DocumentRepository.toJson(record, false));
            JsonNode reference;
            try (InputStream input = getClass().getResourceAsStream("/desktop-sample.json")) {
                reference = json.readTree(input);
            }
            assertEquals(reference.get("source"), actual.get("source"));
            // Parser-version-specific glyph diagnostics and external OCR are not byte-for-byte contracts.
            Set<String> parserSpecific = new HashSet<>(Arrays.asList("pdf:ocrPageCount", "pdf:charsPerPage",
                    "pdf:unmappedUnicodeCharsPerPage", "pdf:totalUnmappedUnicodeChars",
                    "pdf:overallPercentageUnmappedUnicodeChars"));
            Iterator<String> fields = reference.get("metadata").fieldNames();
            while (fields.hasNext()) {
                String key = fields.next();
                if (key.equals("xmp:MetadataDate") || key.equals("xmp:ModifyDate")) {
                    // The packet says 2017-03-16T15:02+02:00. Desktop JempBox
                    // misparses this minutes-only form as 2017-03-17T11:02Z.
                    // Keep the authored instant instead of reproducing that bug.
                    assertEquals(key, "2017-03-16T13:02:00Z", actual.get("metadata").get(key).asText());
                } else if (!parserSpecific.contains(key)) {
                    assertEquals(key, reference.get("metadata").get(key), actual.get("metadata").get(key));
                }
            }
            assertEquals(43, record.getMetadata().get("pdf:charsPerPage").split(", ").length);
        } finally { file.delete(); }
    }
    @Test public void rejectsRenamedNonPdfAndMissingFilesWithFriendlyErrors() throws Exception {
        File file = File.createTempFile("invalid-", ".pdf", RuntimeEnvironment.getApplication().getCacheDir());
        try {
            Files.write(file.toPath(), "this is not a PDF".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            PdfExtractionException failure = assertThrows(PdfExtractionException.class,
                    () -> new PDFExtractor().extract(file, "renamed.pdf"));
            assertTrue(failure.getMessage().contains("valid PDF"));
            assertThrows(PdfExtractionException.class, () -> new PDFExtractor().extract(file, "report.txt"));
        } finally { file.delete(); }
        assertThrows(PdfExtractionException.class, () -> new PDFExtractor().extract(file, "missing.pdf"));
    }
    @Test public void repeatedExtractionsHaveNoLeakedMetadataOrText() throws Exception {
        File first = File.createTempFile("first-", ".pdf", RuntimeEnvironment.getApplication().getCacheDir());
        File second = File.createTempFile("second-", ".pdf", RuntimeEnvironment.getApplication().getCacheDir());
        try {
            try (PDDocument document = new PDDocument()) {
                document.addPage(new PDPage());
                document.getDocumentInformation().setTitle("Only first");
                document.save(first);
            }
            try (PDDocument document = new PDDocument()) { document.addPage(new PDPage()); document.save(second); }
            PDFExtractor extractor = new PDFExtractor();
            assertEquals("Only first", extractor.extract(first, "first.pdf").getMetadata().get("dc:title"));
            assertFalse(extractor.extract(second, "second.pdf").getMetadata().containsKey("dc:title"));
        } finally { first.delete(); second.delete(); }
    }
    @Test public void encryptedPdfReturnsARealFailure() throws Exception {
        File file = File.createTempFile("encrypted-", ".pdf", RuntimeEnvironment.getApplication().getCacheDir());
        try {
            try (PDDocument document = new PDDocument()) {
                document.addPage(new PDPage());
                document.protect(new StandardProtectionPolicy("owner-secret", "user-secret", new AccessPermission()));
                document.save(file);
            }
            PdfExtractionException failure = assertThrows(PdfExtractionException.class,
                    () -> new PDFExtractor().extract(file, "locked.pdf"));
            assertTrue(failure.getMessage().contains("password-protected"));
        } finally { file.delete(); }
    }
    @Test public void boundedPreviewNeverRetainsTextPastTheDesktopLimit() throws Exception {
        PDFExtractor.BoundedPreview preview = new PDFExtractor.BoundedPreview(8);
        preview.write("12345678");
        assertFalse(preview.truncated);
        preview.write("overflow");
        assertEquals("12345678", preview.toString());
        assertTrue(preview.truncated);
    }
}
