package com.example.tvlauncher;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "tv_launcher_prefs";
    private static final String KEY_WALLPAPER_URI = "wallpaper_uri";

    private final List<AppItem> apps = new ArrayList<>();
    private AppAdapter adapter;
    private ImageView wallpaperImage;

    private final ActivityResultLauncher<String[]> wallpaperPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::onWallpaperSelected);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        wallpaperImage = findViewById(R.id.wallpaperImage);
        RecyclerView appsRecyclerView = findViewById(R.id.appsRecyclerView);

        adapter = new AppAdapter(apps, appItem -> {
            Intent launchIntent = appItem.getLaunchIntent();
            if (launchIntent != null) {
                startActivity(launchIntent);
            }
        });

        appsRecyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
        appsRecyclerView.setAdapter(adapter);

        wallpaperImage.setOnLongClickListener(v -> {
            wallpaperPickerLauncher.launch(new String[]{"image/*"});
            Toast.makeText(this, getString(R.string.wallpaper_picker_hint), Toast.LENGTH_SHORT).show();
            return true;
        });

        loadSavedWallpaper();
        loadInstalledApps();
    }

    private void onWallpaperSelected(Uri uri) {
        if (uri == null) {
            return;
        }

        final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
        getContentResolver().takePersistableUriPermission(uri, takeFlags);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putString(KEY_WALLPAPER_URI, uri.toString()).apply();
        setWallpaperFromUri(uri);
    }

    private void loadSavedWallpaper() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String wallpaperUri = prefs.getString(KEY_WALLPAPER_URI, null);
        if (wallpaperUri != null) {
            setWallpaperFromUri(Uri.parse(wallpaperUri));
        } else {
            wallpaperImage.setImageResource(R.drawable.default_wallpaper);
        }
    }

    private void setWallpaperFromUri(@NonNull Uri uri) {
        try {
            Bitmap bitmap;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(), uri);
                bitmap = ImageDecoder.decodeBitmap(source);
            } else {
                ContentResolver resolver = getContentResolver();
                try (InputStream inputStream = resolver.openInputStream(uri)) {
                    bitmap = BitmapFactory.decodeStream(inputStream);
                }
            }

            if (bitmap != null) {
                wallpaperImage.setImageBitmap(bitmap);
            } else {
                wallpaperImage.setImageResource(R.drawable.default_wallpaper);
            }
        } catch (IOException | SecurityException e) {
            wallpaperImage.setImageResource(R.drawable.default_wallpaper);
        }
    }

    private void loadInstalledApps() {
        PackageManager packageManager = getPackageManager();
        Map<String, AppItem> appItems = new HashMap<>();

        Intent leanbackIntent = new Intent(Intent.ACTION_MAIN);
        leanbackIntent.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        mergeApps(packageManager, leanbackIntent, appItems);

        Intent launcherIntent = new Intent(Intent.ACTION_MAIN);
        launcherIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        mergeApps(packageManager, launcherIntent, appItems);

        apps.clear();
        apps.addAll(appItems.values());
        apps.sort(Comparator.comparing(AppItem::getLabel, String.CASE_INSENSITIVE_ORDER));

        adapter.notifyDataSetChanged();
    }

    private void mergeApps(PackageManager packageManager, Intent intent, Map<String, AppItem> appItems) {
        List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL);
        for (ResolveInfo resolveInfo : resolveInfos) {
            String packageName = resolveInfo.activityInfo.packageName;
            if (getPackageName().equals(packageName)) {
                continue;
            }

            Intent launchIntent = packageManager.getLaunchIntentForPackage(packageName);
            if (launchIntent == null) {
                launchIntent = new Intent(intent);
                launchIntent.setClassName(packageName, resolveInfo.activityInfo.name);
            }
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            CharSequence label = resolveInfo.loadLabel(packageManager);
            if (label == null || label.toString().trim().isEmpty()) {
                try {
                    ApplicationInfo appInfo = packageManager.getApplicationInfo(packageName, 0);
                    label = packageManager.getApplicationLabel(appInfo);
                } catch (PackageManager.NameNotFoundException ignored) {
                    label = packageName;
                }
            }

            appItems.put(packageName, new AppItem(label.toString(), resolveInfo.loadIcon(packageManager), launchIntent));
        }
    }
}
