package com.example.pdfmetaviewerandroid.ui;

import android.app.Application;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.pdfmetaviewerandroid.extractor.PDFExtractor;
import com.example.pdfmetaviewerandroid.extractor.PdfExtractionException;
import com.example.pdfmetaviewerandroid.repository.DocumentRecord;
import com.example.pdfmetaviewerandroid.repository.DocumentRepository;
import com.example.pdfmetaviewerandroid.utilities.FileUtils;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Parsing survives rotation; generation checks prevent an old result replacing a newer PDF. */
public final class ViewerViewModel extends AndroidViewModel {
    public static final class State {
        public final DocumentRecord record;
        public final boolean loading;
        public final boolean exporting;
        public final String message;
        public final boolean error;
        public final Uri exportedUri;
        State(DocumentRecord record, boolean loading, boolean exporting, String message, boolean error, Uri exportedUri) {
            this.record = record;
            this.loading = loading;
            this.exporting = exporting;
            this.message = message;
            this.error = error;
            this.exportedUri = exportedUri;
        }
    }
    private final MutableLiveData<State> state = new MutableLiveData<>(
            new State(null, false, false, "Open a PDF to begin.", false, null));
    private final ExecutorService parsing = Executors.newFixedThreadPool(2);
    private final ExecutorService writing = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    public final MetadataPresentation presentation = new MetadataPresentation();
    private Future<?> extraction;
    private long generation;
    private Uri sourceUri;
    private DocumentRecord pendingExport;
    public int selectedTab;
    private boolean cleared;

    public ViewerViewModel(@NonNull Application application) {
        super(application);
        PDFBoxResourceLoader.init(application);
    }
    public LiveData<State> state() { return state; }
    public Uri sourceUri() { return sourceUri; }

    public void open(Uri uri) { open(uri, null); }

    public void open(Uri uri, android.view.DragAndDropPermissions dragPermission) {
        if (uri == null) {
            if (dragPermission != null) dragPermission.release();
            return;
        }
        sourceUri = uri;
        long request = ++generation;
        if (extraction != null) extraction.cancel(true);
        presentation.setMetadata(Collections.emptyMap());
        selectedTab = 0;
        State before = state.getValue();
        state.setValue(new State(null, true, before.exporting, "Analyzing PDF…", false, null));
        java.util.concurrent.FutureTask<Void> task = new java.util.concurrent.FutureTask<Void>(() -> {
            try (FileUtils.ImportedPdf imported = FileUtils.importPdf(getApplication(), uri)) {
                DocumentRecord result = new PDFExtractor().extract(imported.file, imported.displayName);
                main.post(() -> {
                    if (cleared || request != generation) return;
                    presentation.setMetadata(result.getMetadata());
                    State current = state.getValue();
                    state.setValue(new State(result, false, current.exporting,
                            result.getMetadata().size() + " metadata fields found.", false, null));
                });
            } catch (Exception exception) {
                Log.e("PDFMetaViewer", "PDF analysis failed", exception);
                main.post(() -> {
                    if (cleared || request != generation) return;
                    String message = exception instanceof PdfExtractionException || exception instanceof IOException
                            ? exception.getMessage() : "The PDF could not be opened. Check its permissions and try again.";
                    state.setValue(new State(null, false, state.getValue().exporting, message, true, null));
                });
            }
            return null;
        }) {
            @Override protected void done() {
                // done also runs when a queued parse is cancelled before it starts.
                if (dragPermission != null) dragPermission.release();
            }
        };
        extraction = task;
        parsing.execute(task);
    }

    /** Snapshot before opening the system dialog, so a later selection cannot change the export. */
    public String prepareExport() {
        State current = state.getValue();
        if (current.record == null || current.loading || current.exporting) return null;
        pendingExport = current.record;
        return DocumentRepository.suggestedExportName(pendingExport.getSourceName());
    }
    public void exportTo(Uri destination) {
        DocumentRecord record = pendingExport;
        pendingExport = null;
        if (destination == null) return; // User cancelled the save dialog.
        if (record == null) { reportError("Reopen the PDF and choose Export JSON again."); return; }
        State before = state.getValue();
        state.setValue(new State(before.record, before.loading, true, "Saving metadata…", false, null));
        writing.submit(() -> {
            try {
                // Serialize before opening a provider stream: format failures never truncate a destination.
                byte[] json = DocumentRepository.toJson(record, false).getBytes(java.nio.charset.StandardCharsets.UTF_8);
                try (OutputStream output = getApplication().getContentResolver().openOutputStream(destination, "wt")) {
                    if (output == null) throw new IOException("The destination could not be opened.");
                    output.write(json);
                    output.flush();
                }
                main.post(() -> {
                    if (cleared) return;
                    State current = state.getValue();
                    state.setValue(new State(current.record, current.loading, false,
                            "Exported metadata for " + record.getSourceName(), false, destination));
                });
            } catch (Exception exception) {
                Log.e("PDFMetaViewer", "JSON export failed", exception);
                main.post(() -> {
                    if (cleared) return;
                    State current = state.getValue();
                    state.setValue(new State(current.record, current.loading, false,
                            "The JSON file could not be saved. Check the destination and try again.", true, null));
                });
            }
        });
    }
    public void reportError(String message) {
        State current = state.getValue();
        state.setValue(new State(current.record, current.loading, current.exporting, message, true, current.exportedUri));
    }
    @Override protected void onCleared() {
        cleared = true;
        generation++;
        if (extraction != null) extraction.cancel(true);
        parsing.shutdownNow();
        // An export already writing to a provider is allowed to finish.
        writing.shutdown();
    }
}
