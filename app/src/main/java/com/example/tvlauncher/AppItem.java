package com.example.tvlauncher;

import android.content.Intent;
import android.graphics.drawable.Drawable;

public class AppItem {
    private final String label;
    private final Drawable icon;
    private final Intent launchIntent;

    public AppItem(String label, Drawable icon, Intent launchIntent) {
        this.label = label;
        this.icon = icon;
        this.launchIntent = launchIntent;
    }

    public String getLabel() {
        return label;
    }

    public Drawable getIcon() {
        return icon;
    }

    public Intent getLaunchIntent() {
        return launchIntent;
    }
}
