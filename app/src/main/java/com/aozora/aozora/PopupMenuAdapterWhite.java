package com.aozora.aozora;

import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PopupMenuAdapterWhite extends RecyclerView.Adapter<PopupMenuAdapterWhite.ViewHolder> {

    private ColorFilter invertFilter;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    private final List<PopupMenuItem> items;
    private final OnItemClickListener listener;

    public PopupMenuAdapterWhite(List<PopupMenuItem> items, OnItemClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView label;

        ViewHolder(View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.item_icon);
            label = itemView.findViewById(R.id.item_label);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.popup_menu_icon, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PopupMenuItem item = items.get(position);
        holder.icon.setImageResource(item.iconResId);
        holder.label.setText(item.label);

        ColorMatrix colorMatrix_Invert = new ColorMatrix(new float[] {
                -1,  0,  0,  0, 255, // R
                0, -1,  0,  0, 255, // G
                0,  0, -1,  0, 255, // B
                0,  0,  0,  1,   0  // A
        });
        invertFilter = new ColorMatrixColorFilter(colorMatrix_Invert);

        if ("シークレット".equals(item.label)) {
            holder.icon.setColorFilter(invertFilter);
        } else {
            holder.icon.clearColorFilter();
        }

        holder.itemView.setOnClickListener(v -> listener.onItemClick(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}
