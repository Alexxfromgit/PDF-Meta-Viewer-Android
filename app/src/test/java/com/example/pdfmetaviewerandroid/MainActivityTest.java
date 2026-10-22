package com.example.pdfmetaviewerandroid;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.lifecycle.ViewModelProvider;
import com.example.pdfmetaviewerandroid.ui.ViewerViewModel;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowContentResolver;
import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class MainActivityTest {
    private ActivityController<MainActivity> controller;
    private File input;
    private ViewerViewModel model;
    public static final class Provider extends ContentProvider {
        static File input;
        static final AtomicBoolean readOnMainThread = new AtomicBoolean();
        @Override public boolean onCreate() { return true; }
        @Override public String getType(Uri uri) { return "application/pdf"; }
        @Override public Cursor query(Uri uri, String[] projection, String selection, String[] args, String order) {
            readOnMainThread.set(readOnMainThread.get() || Looper.myLooper() == Looper.getMainLooper());
            return new MatrixCursor(new String[]{OpenableColumns.DISPLAY_NAME}) {{
                addRow(new Object[]{"Original document.pdf"});
            }};
        }
        @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
            return ParcelFileDescriptor.open(input, ParcelFileDescriptor.MODE_READ_ONLY);
        }
        @Override public Uri insert(Uri uri, ContentValues values) { return null; }
        @Override public int delete(Uri uri, String selection, String[] args) { return 0; }
        @Override public int update(Uri uri, ContentValues values, String selection, String[] args) { return 0; }
    }
    @Before public void setup() throws Exception {
        input = File.createTempFile("activity-", ".pdf", RuntimeEnvironment.getApplication().getCacheDir());
        try (java.io.InputStream bytes = getClass().getResourceAsStream("/Amazon-S3-Tutorial.pdf")) {
            Files.copy(bytes, input.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        Provider.input = input;
        Provider.readOnMainThread.set(false);
        Provider provider = new Provider();
        android.content.pm.ProviderInfo info = new android.content.pm.ProviderInfo();
        info.authority = "parity.test";
        provider.attachInfo(RuntimeEnvironment.getApplication(), info);
        ShadowContentResolver.registerProviderInternal("parity.test", provider);
        controller = Robolectric.buildActivity(MainActivity.class).setup();
        model = new ViewModelProvider(controller.get()).get(ViewerViewModel.class);
    }
    @After public void cleanup() {
        controller.pause().stop().destroy();
        input.delete();
    }
    private void awaitAnalysis() throws Exception {
        long until = System.currentTimeMillis() + 20_000;
        while (model.state().getValue().loading && System.currentTimeMillis() < until) {
            Shadows.shadowOf(Looper.getMainLooper()).idle();
            Thread.sleep(10);
        }
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertFalse("Analysis did not complete", model.state().getValue().loading);
        if (model.state().getValue().error) {
            for (org.robolectric.shadows.ShadowLog.LogItem item : org.robolectric.shadows.ShadowLog.getLogsForTag("PDFMetaViewer"))
                if (item.throwable != null) item.throwable.printStackTrace();
        }
    }
    @Test public void activeScreenLoadsTheOriginalNameSearchesCopiesAndStartsJsonExport() throws Exception {
        model.open(Uri.parse("content://parity.test/report"));
        awaitAnalysis();
        assertNotNull(model.state().getValue().message, model.state().getValue().record);
        MainActivity activity = controller.get();
        assertEquals("Original document.pdf", ((TextView) activity.findViewById(R.id.file_name)).getText().toString());
        assertFalse("Provider IO must stay off the UI thread", Provider.readOnMainThread.get());
        assertTrue(activity.findViewById(R.id.export_button).isEnabled());
        ((TextInputEditText) activity.findViewById(R.id.search)).setText("producer");
        ((MaterialCheckBox) activity.findViewById(R.id.select_all)).performClick();
        assertTrue(model.presentation.selectedCount() > 0);
        ((Spinner) activity.findViewById(R.id.sort)).setSelection(3);
        activity.findViewById(R.id.copy_button).performClick();
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager)
                activity.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
        assertTrue(clipboard.getPrimaryClip().getItemAt(0).getText().toString().contains("pdfTeX-1.40.16"));
        activity.findViewById(R.id.export_button).performClick();
        Intent save = Shadows.shadowOf(activity).getNextStartedActivityForResult().intent;
        assertEquals(Intent.ACTION_CREATE_DOCUMENT, save.getAction());
        assertEquals("application/json", save.getType());
        assertEquals("Original document-metadata.json", save.getStringExtra(Intent.EXTRA_TITLE));
    }
    @Test public void rotationKeepsTheDocumentSearchSelectionAndTextTab() throws Exception {
        model.open(Uri.parse("content://parity.test/report"));
        awaitAnalysis();
        String documentId = model.state().getValue().record.getId();
        ((TextInputEditText) controller.get().findViewById(R.id.search)).setText("producer");
        model.presentation.selectVisible(true);
        ((TabLayout) controller.get().findViewById(R.id.result_tabs)).selectTab(
                ((TabLayout) controller.get().findViewById(R.id.result_tabs)).getTabAt(1));
        Configuration landscape = new Configuration(controller.get().getResources().getConfiguration());
        landscape.orientation = Configuration.ORIENTATION_LANDSCAPE;
        controller.configurationChange(landscape);
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        ViewerViewModel retained = new ViewModelProvider(controller.get()).get(ViewerViewModel.class);
        assertEquals(documentId, retained.state().getValue().record.getId());
        assertEquals("producer", retained.presentation.getQuery());
        assertTrue(retained.presentation.selectedCount() > 0);
        assertEquals(1, retained.selectedTab);
        assertEquals(android.view.View.VISIBLE, controller.get().findViewById(R.id.text_panel).getVisibility());
    }
    @Test public void aFailedReadCannotLeaveAnOldExportEnabledOrReportSuccess() throws Exception {
        model.open(Uri.parse("content://parity.test/report"));
        awaitAnalysis();
        Files.write(input.toPath(), "not a PDF".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        model.open(Uri.parse("content://parity.test/broken"));
        awaitAnalysis();
        assertTrue(model.state().getValue().error);
        assertNull(model.state().getValue().record);
        assertFalse(controller.get().findViewById(R.id.export_button).isEnabled());
    }
}
