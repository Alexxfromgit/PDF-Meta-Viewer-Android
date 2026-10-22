package com.example.pdfmetaviewerandroid;

import com.example.pdfmetaviewerandroid.ui.MetadataPresentation;
import org.junit.Test;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.junit.Assert.*;

public class MetadataPresentationTest {
    private MetadataPresentation sample() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("Author", "Zelda");
        fields.put("Title", "Alpha [draft]");
        fields.put("Producer", "Alpha");
        MetadataPresentation model = new MetadataPresentation();
        model.setMetadata(fields);
        return model;
    }
    @Test public void searchIsLiteralCaseInsensitiveAndSearchesKeysAndValues() {
        MetadataPresentation model = sample();
        model.setQuery("aUtHoR");
        assertEquals("Author", model.visibleRows().get(0).getKey());
        model.setQuery("[draft]");
        assertEquals("Title", model.visibleRows().get(0).getKey());
        model.setQuery(".*");
        assertTrue(model.visibleRows().isEmpty());
    }
    @Test public void copyingAfterValueSortKeepsTheRightPropertyValuePairs() {
        MetadataPresentation model = sample();
        model.toggle("Author");
        model.toggle("Title");
        model.setSort(MetadataPresentation.Sort.VALUE_DESC);
        assertEquals("Author\tZelda\nTitle\tAlpha [draft]", model.copySelected());
        model.setSort(MetadataPresentation.Sort.VALUE_ASC);
        assertEquals("Title\tAlpha [draft]\nAuthor\tZelda", model.copySelected());
    }
    @Test public void filteringCannotCopyInvisibleSelections() {
        MetadataPresentation model = sample();
        model.selectVisible(true);
        model.setQuery("Producer");
        assertEquals(1, model.selectedCount());
        assertEquals("Producer\tAlpha", model.copySelected());
        model.setQuery("");
        assertEquals(1, model.selectedCount());
    }
    @Test public void replacingTheDocumentClearsSelectionAndSearch() {
        MetadataPresentation model = sample();
        model.toggle("Author");
        model.setQuery("Author");
        model.setMetadata(new LinkedHashMap<>());
        assertEquals("", model.getQuery());
        assertEquals(0, model.selectedCount());
        assertEquals("", model.copySelected());
    }
}
