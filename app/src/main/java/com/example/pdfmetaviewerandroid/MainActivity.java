package com.example.pdfmetaviewerandroid;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.Formatter;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.pdfmetaviewerandroid.repository.DocumentRecord;
import com.example.pdfmetaviewerandroid.ui.MetadataAdapter;
import com.example.pdfmetaviewerandroid.ui.MetadataPresentation;
import com.example.pdfmetaviewerandroid.ui.ViewerViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;

/** Native Android counterpart of the desktop inspection workflow. All file work lives in the ViewModel. */
public final class MainActivity extends AppCompatActivity {
    private ViewerViewModel viewModel;
    private MetadataAdapter adapter;
    private TextInputEditText search;
    private Spinner sort;
    private MaterialCheckBox selectAll;
    private MaterialButton copyButton;
    private MaterialButton exportButton;
    private MaterialButton themeButton;
    private TextView status;
    private ProgressBar progress;
    private TabLayout tabs;
    private String renderedDocumentId;
    private SharedPreferences preferences;
    private int neutralStatusColor;

    private final ActivityResultLauncher<String[]> openPdf = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri == null) return;
                try {
                    getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (SecurityException ignored) { /* Some providers only grant access for this session. */ }
                viewModel.open(uri);
            });
    private final ActivityResultLauncher<String> saveJson = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/json"), uri -> viewModel.exportTo(uri));

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        preferences = getSharedPreferences("viewer", MODE_PRIVATE);
        AppCompatDelegate.setDefaultNightMode(preferences.getBoolean("darkMode", false)
                ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);
        View root = findViewById(R.id.viewer_root);
        int padding = Math.round(12 * getResources().getDisplayMetrics().density);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            view.setPadding(bars.left + padding, bars.top + padding, bars.right + padding, bars.bottom + padding);
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
        root.setOnDragListener((view, event) -> {
            if (event.getAction() == android.view.DragEvent.ACTION_DRAG_STARTED) {
                return event.getClipDescription() != null
                        && (event.getClipDescription().hasMimeType("application/pdf")
                        || event.getClipDescription().hasMimeType("text/uri-list"));
            }
            if (event.getAction() == android.view.DragEvent.ACTION_DROP) {
                ClipData data = event.getClipData();
                if (data == null || data.getItemCount() != 1 || data.getItemAt(0).getUri() == null) {
                    viewModel.reportError(getString(R.string.single_pdf));
                    return true;
                }
                viewModel.open(data.getItemAt(0).getUri(), requestDragAndDropPermissions(event));
            }
            return true;
        });
        viewModel = new ViewModelProvider(this).get(ViewerViewModel.class);
        search = findViewById(R.id.search);
        sort = findViewById(R.id.sort);
        selectAll = findViewById(R.id.select_all);
        copyButton = findViewById(R.id.copy_button);
        exportButton = findViewById(R.id.export_button);
        themeButton = findViewById(R.id.theme_button);
        status = findViewById(R.id.status);
        progress = findViewById(R.id.progress);
        tabs = findViewById(R.id.result_tabs);
        neutralStatusColor = status.getCurrentTextColor();
        boolean dark = preferences.getBoolean("darkMode", false);
        themeButton.setText(dark ? R.string.light_mode : R.string.dark_mode);
        themeButton.setContentDescription(getString(dark ? R.string.switch_light : R.string.switch_dark));
        themeButton.setOnClickListener(view -> {
            boolean next = !preferences.getBoolean("darkMode", false);
            preferences.edit().putBoolean("darkMode", next).apply();
            AppCompatDelegate.setDefaultNightMode(next ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        });
        findViewById(R.id.open_button).setOnClickListener(view -> choosePdf());
        exportButton.setOnClickListener(view -> exportJson());
        copyButton.setOnClickListener(view -> copy(viewModel.presentation.copySelected()));
        RecyclerView list = findViewById(R.id.metadata_list);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setItemAnimator(null);
        adapter = new MetadataAdapter(viewModel.presentation, this::refreshMetadata, this::copy);
        list.setAdapter(adapter);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence value, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence value, int start, int before, int count) {
                viewModel.presentation.setQuery(value.toString());
                refreshMetadata();
            }
            @Override public void afterTextChanged(Editable value) {}
        });
        sort.setSelection(viewModel.presentation.getSort().ordinal());
        sort.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                viewModel.presentation.setSort(MetadataPresentation.Sort.values()[position]);
                refreshMetadata();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) { showTab(tab.getPosition()); }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
        viewModel.state().observe(this, this::render);
        if (viewModel.sourceUri() == null) {
            if (savedInstanceState != null && savedInstanceState.getString("sourceUri") != null)
                viewModel.open(Uri.parse(savedInstanceState.getString("sourceUri")));
            else handleIncoming(getIntent());
        }
    }

    private void render(ViewerViewModel.State state) {
        DocumentRecord record = state.record;
        boolean ready = record != null;
        findViewById(R.id.results).setVisibility(ready ? View.VISIBLE : View.GONE);
        findViewById(R.id.welcome).setVisibility(ready ? View.GONE : View.VISIBLE);
        findViewById(R.id.file_summary).setVisibility(ready ? View.VISIBLE : View.GONE);
        exportButton.setEnabled(ready && !state.loading && !state.exporting);
        progress.setVisibility(state.loading || state.exporting ? View.VISIBLE : View.GONE);
        status.setText(state.message);
        status.setTextColor(state.error ? MaterialColors.getColor(status, com.google.android.material.R.attr.colorError)
                : neutralStatusColor);
        MaterialButton openExport = findViewById(R.id.open_export);
        openExport.setVisibility(state.exportedUri == null ? View.GONE : View.VISIBLE);
        openExport.setOnClickListener(view -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(state.exportedUri, "application/json")
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));
            } catch (ActivityNotFoundException | SecurityException exception) {
                Toast.makeText(this, R.string.export_no_handler, Toast.LENGTH_LONG).show();
            }
        });
        if (!ready) {
            renderedDocumentId = null;
            refreshMetadata();
            return;
        }
        ((TextView) findViewById(R.id.file_name)).setText(record.getSourceName());
        ((TextView) findViewById(R.id.file_facts)).setText(getString(R.string.file_facts,
                Formatter.formatShortFileSize(this, record.getFileSize()),
                record.getMetadata().getOrDefault("xmpTPg:NPages", "—"), record.getMetadata().size()));
        tabs.getTabAt(0).setText(getString(R.string.metadata_count, record.getMetadata().size()));
        if (!record.getId().equals(renderedDocumentId)) {
            renderedDocumentId = record.getId();
            TextView text = findViewById(R.id.text_preview);
            text.setText(record.getContent().trim().isEmpty() ? getString(R.string.no_text) : record.getContent());
            TextView note = findViewById(R.id.preview_note);
            note.setText(record.isContentTruncated() ? getString(R.string.preview_truncated)
                    : getString(R.string.preview_characters, record.getContent().length()));
            findViewById(R.id.text_scroll).scrollTo(0, 0);
        }
        if (!search.getText().toString().equals(viewModel.presentation.getQuery()))
            search.setText(viewModel.presentation.getQuery());
        tabs.selectTab(tabs.getTabAt(viewModel.selectedTab));
        showTab(viewModel.selectedTab);
        refreshMetadata();
    }

    private void refreshMetadata() {
        if (adapter == null) return;
        adapter.refresh();
        int visible = adapter.getItemCount();
        int count = viewModel.presentation.selectedCount();
        copyButton.setEnabled(count > 0);
        copyButton.setText(count > 0 ? getString(R.string.copy_count, count) : getString(R.string.copy_selected));
        selectAll.setOnCheckedChangeListener(null);
        selectAll.setEnabled(visible > 0);
        selectAll.setCheckedState(count == 0 ? MaterialCheckBox.STATE_UNCHECKED
                : count == visible ? MaterialCheckBox.STATE_CHECKED : MaterialCheckBox.STATE_INDETERMINATE);
        selectAll.setOnCheckedChangeListener((button, checked) -> {
            viewModel.presentation.selectVisible(checked);
            refreshMetadata();
        });
        findViewById(R.id.no_matches).setVisibility(visible == 0 ? View.VISIBLE : View.GONE);
    }
    private void showTab(int position) {
        viewModel.selectedTab = position;
        findViewById(R.id.metadata_panel).setVisibility(position == 0 ? View.VISIBLE : View.GONE);
        findViewById(R.id.text_panel).setVisibility(position == 1 ? View.VISIBLE : View.GONE);
    }
    private void choosePdf() { openPdf.launch(new String[]{"application/pdf"}); }
    private void exportJson() {
        String name = viewModel.prepareExport();
        if (name != null) saveJson.launch(name);
    }
    private void copy(String value) {
        if (value.isEmpty()) return;
        try {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard == null) throw new IllegalStateException("Clipboard unavailable");
            clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.metadata), value));
            Toast.makeText(this, R.string.copied, Toast.LENGTH_SHORT).show();
        } catch (RuntimeException exception) {
            Toast.makeText(this, R.string.clipboard_failed, Toast.LENGTH_LONG).show();
        }
    }
    private void handleIncoming(Intent intent) {
        if (intent == null) return;
        Uri uri = null;
        if (Intent.ACTION_VIEW.equals(intent.getAction())) uri = intent.getData();
        else if (Intent.ACTION_SEND.equals(intent.getAction())) uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (uri != null) viewModel.open(uri);
    }
    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncoming(intent);
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        if (viewModel.sourceUri() != null) state.putString("sourceUri", viewModel.sourceUri().toString());
        super.onSaveInstanceState(state);
    }
    @Override public boolean onKeyShortcut(int keyCode, KeyEvent event) {
        if (event.isCtrlPressed() || event.isMetaPressed()) {
            if (keyCode == KeyEvent.KEYCODE_O) { choosePdf(); return true; }
            if (keyCode == KeyEvent.KEYCODE_E) { exportJson(); return true; }
            if (keyCode == KeyEvent.KEYCODE_F) {
                tabs.selectTab(tabs.getTabAt(0));
                search.requestFocus();
                ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).showSoftInput(search, InputMethodManager.SHOW_IMPLICIT);
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_C && viewModel.selectedTab == 0
                    && viewModel.presentation.selectedCount() > 0) {
                copy(viewModel.presentation.copySelected());
                return true;
            }
        }
        return super.onKeyShortcut(keyCode, event);
    }
    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_ESCAPE && search.length() > 0) { search.setText(""); return true; }
        return super.onKeyDown(keyCode, event);
    }
}
