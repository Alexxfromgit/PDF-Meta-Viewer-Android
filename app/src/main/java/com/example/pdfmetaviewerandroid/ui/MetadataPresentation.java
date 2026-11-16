package com.example.pdfmetaviewerandroid.ui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Search, ordering and selection use metadata keys, so sorting never changes what gets copied. */
public final class MetadataPresentation {
    public enum Sort { PROPERTY_ASC, PROPERTY_DESC, VALUE_ASC, VALUE_DESC }
    private final List<Map.Entry<String, String>> rows = new ArrayList<>();
    private final Set<String> selected = new HashSet<>();
    private String query = "";
    private Sort sort = Sort.PROPERTY_ASC;

    public void setMetadata(Map<String, String> values) {
        rows.clear();
        for (Map.Entry<String, String> entry : values.entrySet())
            rows.add(new java.util.AbstractMap.SimpleImmutableEntry<>(entry));
        selected.clear();
        query = "";
    }
    public String getQuery() { return query; }
    public Sort getSort() { return sort; }
    public void setQuery(String value) {
        query = value;
        Set<String> visible = new HashSet<>();
        for (Map.Entry<String, String> row : visibleRows()) visible.add(row.getKey());
        selected.retainAll(visible);
    }
    public void setSort(Sort value) { sort = value; }
    public boolean isSelected(String key) { return selected.contains(key); }
    public int selectedCount() { return selected.size(); }
    public void toggle(String key) { if (!selected.remove(key)) selected.add(key); }
    public void selectVisible(boolean select) {
        for (Map.Entry<String, String> row : visibleRows()) {
            if (select) selected.add(row.getKey()); else selected.remove(row.getKey());
        }
    }
    public List<Map.Entry<String, String>> visibleRows() {
        String needle = query.trim().toLowerCase(Locale.ROOT);
        List<Map.Entry<String, String>> visible = new ArrayList<>();
        for (Map.Entry<String, String> row : rows) {
            if (needle.isEmpty() || row.getKey().toLowerCase(Locale.ROOT).contains(needle)
                    || row.getValue().toLowerCase(Locale.ROOT).contains(needle)) visible.add(row);
        }
        Comparator<Map.Entry<String, String>> byKey =
                Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER);
        Comparator<Map.Entry<String, String>> order =
                sort == Sort.VALUE_ASC || sort == Sort.VALUE_DESC
                        ? Map.Entry.<String, String>comparingByValue(String.CASE_INSENSITIVE_ORDER).thenComparing(byKey)
                        : byKey;
        if (sort == Sort.PROPERTY_DESC || sort == Sort.VALUE_DESC) order = order.reversed();
        visible.sort(order);
        return visible;
    }
    public String copySelected() {
        StringBuilder text = new StringBuilder();
        for (Map.Entry<String, String> row : visibleRows()) {
            if (!selected.contains(row.getKey())) continue;
            if (text.length() > 0) text.append('\n');
            text.append(row.getKey()).append('\t').append(row.getValue());
        }
        return text.toString();
    }
}
