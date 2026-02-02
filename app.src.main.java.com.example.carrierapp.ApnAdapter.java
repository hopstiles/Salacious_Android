package com.example.carrierapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ApnAdapter extends RecyclerView.Adapter<ApnAdapter.ViewHolder> {

    private final List<ApnEditorActivity.ApnData> list;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(ApnEditorActivity.ApnData item);
    }

    public ApnAdapter(List<ApnEditorActivity.ApnData> list, OnItemClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_2, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ApnEditorActivity.ApnData item = list.get(position);
        holder.text1.setText(item.name);
        holder.text2.setText(item.apn + " (" + item.type + ")");
        holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView text1, text2;
        ViewHolder(View v) {
            super(v);
            text1 = v.findViewById(android.R.id.text1);
            text2 = v.findViewById(android.R.id.text2);
        }
    }
}