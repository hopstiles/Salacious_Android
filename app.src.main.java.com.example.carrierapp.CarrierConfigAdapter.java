package com.example.carrierapp;

import android.os.PersistableBundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CarrierConfigAdapter extends RecyclerView.Adapter<CarrierConfigAdapter.ViewHolder> {

    private final PersistableBundle configBundle;
    private final List<String> originalKeys;
    private final List<String> filteredKeys;
    private final OnConfigChangeListener listener;

    public interface OnConfigChangeListener {
        void onConfigChanged(String key, Object newValue);
    }

    public CarrierConfigAdapter(PersistableBundle configBundle, OnConfigChangeListener listener) {
        this.configBundle = configBundle;
        this.listener = listener;
        this.originalKeys = new ArrayList<>(configBundle.keySet());
        Collections.sort(this.originalKeys);
        this.filteredKeys = new ArrayList<>(this.originalKeys);
    }

    public void filter(String text) {
        filteredKeys.clear();
        if (text == null || text.isEmpty()) {
            filteredKeys.addAll(originalKeys);
        } else {
            String query = text.toLowerCase();
            for (String key : originalKeys) {
                if (key.toLowerCase().contains(query)) {
                    filteredKeys.add(key);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_carrier_config, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String key = filteredKeys.get(position);
        Object value = configBundle.get(key);

        holder.keyText.setText(key);
        holder.valueSwitch.setOnCheckedChangeListener(null);
        holder.itemView.setOnClickListener(null);

        if (value instanceof Boolean) {
            holder.valueSwitch.setVisibility(View.VISIBLE);
            holder.valueText.setVisibility(View.GONE);
            holder.valueSwitch.setChecked((Boolean) value);
            holder.valueSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                listener.onConfigChanged(key, isChecked);
            });
        } else {
            holder.valueSwitch.setVisibility(View.GONE);
            holder.valueText.setVisibility(View.VISIBLE);
            holder.valueText.setText(String.valueOf(value));
            holder.itemView.setOnClickListener(v -> {
                listener.onConfigChanged(key, "EDIT_REQUEST");
            });
        }
    }

    @Override
    public int getItemCount() {
        return filteredKeys.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialTextView keyText;
        MaterialTextView valueText;
        MaterialSwitch valueSwitch;

        ViewHolder(View view) {
            super(view);
            keyText = view.findViewById(R.id.config_key);
            valueText = view.findViewById(R.id.value_text);
            valueSwitch = view.findViewById(R.id.value_switch);
        }
    }
}