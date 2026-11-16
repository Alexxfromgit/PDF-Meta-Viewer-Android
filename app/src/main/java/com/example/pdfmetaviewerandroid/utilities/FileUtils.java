package com.example.pdfmetaviewerandroid.utilities;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.Locale;

public final class FileUtils {
    private FileUtils() {}

    public static final class ImportedPdf implements AutoCloseable {
        public final File file;
        public final String displayName;
        ImportedPdf(File file, String displayName) { this.file = file; this.displayName = displayName; }
        @Override public void close() { if (!file.delete()) file.deleteOnExit(); }
    }

    /** Copy once, off the UI thread, and parse the local snapshot rather than reopening the provider. */
    public static ImportedPdf importPdf(Context context, Uri uri) throws IOException {
        String name = null;
        try (Cursor cursor = context.getContentResolver().query(uri,
                new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) name = cursor.getString(index);
            }
        }
        if (name == null || name.trim().isEmpty()) name = uri.getLastPathSegment();
        if (name == null || name.trim().isEmpty()) name = "document.pdf";
        // A provider's display name is a label, never a path.
        name = name.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1);
        if (!name.toLowerCase(Locale.ROOT).endsWith(".pdf")) throw new IOException("Only PDF files can be analyzed.");
        File temporary = File.createTempFile("pdf-inspect-", ".pdf", context.getCacheDir());
        boolean copied = false;
        try {
            try (InputStream input = context.getContentResolver().openInputStream(uri);
                 FileOutputStream output = new FileOutputStream(temporary)) {
                if (input == null) throw new IOException("The selected PDF could not be opened.");
                byte[] buffer = new byte[32 * 1024];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("PDF analysis was interrupted.");
                    output.write(buffer, 0, count);
                }
            }
            copied = true;
            return new ImportedPdf(temporary, name);
        } finally {
            if (!copied && !temporary.delete()) temporary.deleteOnExit();
        }
    }
    @Deprecated public static File getFileFromUri(Context context, Uri uri) throws IOException {
        return importPdf(context, uri).file;
    }
    public static InputStream getInputStreamFromUri(Context context, Uri uri) throws IOException {
        return context.getContentResolver().openInputStream(uri);
    }
}
