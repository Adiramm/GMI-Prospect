package com.gmi.prospect;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * Application class - created once, before any Activity.
 * <p>
 * We apply the saved light/dark choice here so the very first screen already
 * opens in the right theme (no flash of the wrong colours). The language choice
 * does not need code here: AppCompat stores and restores it automatically thanks
 * to {@code AppLocalesMetadataHolderService} (autoStoreLocales) in the manifest.
 */
public class GmiApp extends Application {

    /** SharedPreferences file for app-wide settings. */
    public static final String PREFS_NAME = "gmi_settings";
    /** Key for the saved night mode (one of AppCompatDelegate.MODE_NIGHT_*). */
    public static final String KEY_NIGHT_MODE = "night_mode";

    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setDefaultNightMode(getSavedNightMode(this));
    }

    /** Returns the saved night mode, or "follow system" if the user never chose one. */
    public static int getSavedNightMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getInt(KEY_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    /**
     * Saves and applies a new night mode. Applying it recreates every open
     * Activity (onPause → onStop → onDestroy → onCreate ...), which is why
     * screens must save their state in onSaveInstanceState.
     */
    public static void setNightMode(Context context, int mode) {
        context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putInt(KEY_NIGHT_MODE, mode)
                .apply();
        AppCompatDelegate.setDefaultNightMode(mode);
    }
}
