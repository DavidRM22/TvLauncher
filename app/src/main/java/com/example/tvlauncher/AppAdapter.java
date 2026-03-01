package com.example.tvlauncher;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class AppAdapter extends RecyclerView.Adapter<AppAdapter.AppViewHolder> {

    public interface OnAppClickListener {
        void onAppClick(AppItem appItem);
    }

    private final List<AppItem> appItems;
    private final OnAppClickListener onAppClickListener;

    public AppAdapter(List<AppItem> appItems, OnAppClickListener onAppClickListener) {
        this.appItems = appItems;
        this.onAppClickListener = onAppClickListener;
    }

    @NonNull
    @Override
    public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app_card, parent, false);
        return new AppViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
        AppItem appItem = appItems.get(position);
        holder.appNameText.setText(appItem.getLabel());
        holder.appIconImage.setImageDrawable(appItem.getIcon());

        holder.itemView.setOnClickListener(v -> onAppClickListener.onAppClick(appItem));
        holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
            float scale = hasFocus ? 1.08f : 1.0f;
            v.animate().scaleX(scale).scaleY(scale).setDuration(140).start();
            holder.cardView.setStrokeWidth(hasFocus ? 3 : 0);
        });
    }

    @Override
    public int getItemCount() {
        return appItems.size();
    }

    static class AppViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView cardView;
        final ImageView appIconImage;
        final TextView appNameText;

        AppViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            appIconImage = itemView.findViewById(R.id.appIconImage);
            appNameText = itemView.findViewById(R.id.appNameText);
        }
    }
}
