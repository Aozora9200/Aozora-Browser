package com.aozora.aozora;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.util.List;

public class SavedPageAdapter extends RecyclerView.Adapter<SavedPageAdapter.ViewHolder> {
    private Context context;
    private List<SavedPage> pages;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(SavedPage page);
        void onItemLongClick(SavedPage page);
    }

    public SavedPageAdapter(Context context, List<SavedPage> pages, OnItemClickListener listener) {
        this.context = context;
        this.pages = pages;
        this.listener = listener;
    }

    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.savedpageitem, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SavedPage page = pages.get(position);
        holder.textViewTitle.setText(page.title);
        holder.textViewUrl.setText(page.url);
        holder.textViewDate.setText(page.dateSaved);

        File imgFile = new File(page.screenshotPath);
        if (imgFile.exists()) {
            holder.imageViewScreenshot.setImageURI(Uri.fromFile(imgFile));
        } else {
            holder.imageViewScreenshot.setImageResource(R.drawable.placeholder);
        }

        holder.itemView.setOnClickListener(v -> listener.onItemClick(page));
        holder.itemView.setOnLongClickListener(v -> {
            listener.onItemLongClick(page);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return pages.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewScreenshot;
        TextView textViewTitle, textViewUrl, textViewDate;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewScreenshot = itemView.findViewById(R.id.imageViewScreenshot);
            textViewTitle = itemView.findViewById(R.id.textViewTitle);
            textViewUrl = itemView.findViewById(R.id.textViewUrl);
            textViewDate = itemView.findViewById(R.id.textViewDate);
        }
    }
}
