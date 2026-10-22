package com.example.pdfmetaviewerandroid.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.pdfmetaviewerandroid.R;
import com.google.android.material.checkbox.MaterialCheckBox;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class MetadataAdapter extends RecyclerView.Adapter<MetadataAdapter.Row> {
    private List<Map.Entry<String, String>> rows = new ArrayList<>();
    private final MetadataPresentation presentation;
    private final Runnable changed;
    private final Consumer<String> copy;
    public MetadataAdapter(MetadataPresentation presentation, Runnable changed, Consumer<String> copy) {
        this.presentation = presentation;
        this.changed = changed;
        this.copy = copy;
    }
    public void refresh() { rows = presentation.visibleRows(); notifyDataSetChanged(); }
    @NonNull @Override public Row onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        return new Row(LayoutInflater.from(parent.getContext()).inflate(R.layout.metadata_row, parent, false));
    }
    @Override public void onBindViewHolder(@NonNull Row holder, int position) {
        Map.Entry<String, String> entry = rows.get(position);
        holder.property.setText(entry.getKey());
        holder.value.setText(entry.getValue());
        holder.selected.setOnCheckedChangeListener(null);
        holder.selected.setChecked(presentation.isSelected(entry.getKey()));
        holder.selected.setContentDescription(holder.itemView.getContext().getString(R.string.select_property, entry.getKey()));
        holder.selected.setOnCheckedChangeListener((button, checked) -> { presentation.toggle(entry.getKey()); changed.run(); });
        holder.itemView.setOnClickListener(view -> { presentation.toggle(entry.getKey()); changed.run(); });
        holder.itemView.setOnLongClickListener(view -> { copy.accept(entry.getKey() + "\t" + entry.getValue()); return true; });
    }
    @Override public int getItemCount() { return rows.size(); }
    static final class Row extends RecyclerView.ViewHolder {
        final MaterialCheckBox selected;
        final TextView property;
        final TextView value;
        Row(View item) {
            super(item);
            selected = item.findViewById(R.id.row_selected);
            property = item.findViewById(R.id.row_property);
            value = item.findViewById(R.id.row_value);
        }
    }
}
