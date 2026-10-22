package com.example.pdfmetaviewerandroid;

import android.content.Context;
import android.net.Uri;
import android.widget.TextView;
import androidx.lifecycle.ViewModelProvider;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.pdfmetaviewerandroid.repository.DocumentRecord;
import com.example.pdfmetaviewerandroid.ui.ViewerViewModel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

/** Exercises the actual Android parser, widgets, rotation and export stream, not framework shadows. */
@RunWith(AndroidJUnit4.class)
public class ParityWorkflowTest {
    private File sample(String name) throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File file = new File(context.getCacheDir(), name);
        try (InputStream input = context.getAssets().open("Amazon-S3-Tutorial.pdf");
             FileOutputStream output = new FileOutputStream(file)) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
        }
        return file;
    }
    private ViewerViewModel.State awaitReady(ActivityScenario<MainActivity> scenario) throws Exception {
        AtomicReference<ViewerViewModel.State> latest = new AtomicReference<>();
        long deadline = System.currentTimeMillis() + 90_000;
        do {
            scenario.onActivity(activity -> latest.set(new ViewModelProvider(activity).get(ViewerViewModel.class).state().getValue()));
            if (!latest.get().loading && !latest.get().exporting) return latest.get();
            Thread.sleep(100);
        } while (System.currentTimeMillis() < deadline);
        throw new AssertionError("Background work did not finish");
    }
    @Test public void realDocumentSearchSelectionRotationThemeAndJsonExport() throws Exception {
        File file = sample("Parity Sample.pdf");
        File exported = new File(file.getParentFile(), "parity-export.json");
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> new ViewModelProvider(activity).get(ViewerViewModel.class).open(Uri.fromFile(file)));
            ViewerViewModel.State loaded = awaitReady(scenario);
            assertFalse(loaded.message, loaded.error);
            assertNotNull(loaded.record);
            DocumentRecord record = loaded.record;
            assertEquals("Parity Sample.pdf", record.getSourceName());
            assertEquals("43", record.getMetadata().get("xmpTPg:NPages"));
            assertFalse(record.getContent().trim().isEmpty());
            scenario.onActivity(activity -> {
                assertEquals("Parity Sample.pdf", ((TextView) activity.findViewById(R.id.file_name)).getText().toString());
                ((TextInputEditText) activity.findViewById(R.id.search)).setText("producer");
                ((MaterialCheckBox) activity.findViewById(R.id.select_all)).performClick();
                ViewerViewModel model = new ViewModelProvider(activity).get(ViewerViewModel.class);
                assertTrue(model.presentation.copySelected().contains("pdfTeX-1.40.16"));
            });
            scenario.recreate();
            scenario.onActivity(activity -> {
                ViewerViewModel model = new ViewModelProvider(activity).get(ViewerViewModel.class);
                assertEquals(record.getId(), model.state().getValue().record.getId());
                assertEquals("producer", model.presentation.getQuery());
                assertTrue(model.presentation.selectedCount() > 0);
                activity.findViewById(R.id.theme_button).performClick();
            });
            // Appearance changes recreate the activity asynchronously.
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(activity -> {
                ViewerViewModel model = new ViewModelProvider(activity).get(ViewerViewModel.class);
                assertEquals(record.getId(), model.state().getValue().record.getId());
                assertNotNull(model.prepareExport());
                model.exportTo(Uri.fromFile(exported));
            });
            ViewerViewModel.State saved = awaitReady(scenario);
            assertFalse(saved.message, saved.error);
            JsonNode json = new ObjectMapper().readTree(exported);
            assertEquals("Parity Sample.pdf", json.get("source").asText());
            assertEquals("Amazon S3 Tutorial", json.get("metadata").get("dc:title").asText());
            assertFalse(json.has("content"));
        } finally { file.delete(); exported.delete(); }
    }
    @Test public void badPdfReportsFailureAndDisablesExport() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File broken = new File(context.getCacheDir(), "broken.pdf");
        try (FileOutputStream output = new FileOutputStream(broken)) { output.write(new byte[]{1, 2, 3}); }
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> new ViewModelProvider(activity).get(ViewerViewModel.class).open(Uri.fromFile(broken)));
            ViewerViewModel.State state = awaitReady(scenario);
            assertTrue(state.error);
            assertNull(state.record);
            scenario.onActivity(activity -> assertFalse(activity.findViewById(R.id.export_button).isEnabled()));
        } finally { broken.delete(); }
    }
}
