package com.gmi.prospect.base;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.gmi.prospect.GmiApp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The app's lifecycle log: every message goes to Logcat (tag {@code LIFECYCLE})
 * AND into a small in-memory list that the on-screen log panel shows.
 * <p>
 * The list lives in static fields, so it survives Activity re-creation
 * (rotation, theme, language) but NOT the app process being killed - an empty
 * log after coming back is itself a sign that Android killed the process.
 * <p>
 * Whether the panel is shown is a user setting, saved in SharedPreferences.
 * Everything here runs on the main (UI) thread.
 */
public final class LifecycleLog {

    /** Logcat tag used for all lifecycle messages (filter: tag:LIFECYCLE). */
    public static final String TAG = "LIFECYCLE";

    /** Oldest lines are dropped after this many, so memory use stays tiny. */
    private static final int MAX_LINES = 100;

    private static final String KEY_PANEL_ON = "lifecycle_log_on";

    /** One logged line and when it happened. */
    public static final class Entry {
        public final long timeMillis;
        public final String message;

        Entry(long timeMillis, String message) {
            this.timeMillis = timeMillis;
            this.message = message;
        }
    }

    /** Something that wants to hear about new lines (the log panel). */
    public interface Listener {
        void onLogChanged();
    }

    private static final List<Entry> entries = new ArrayList<>();
    private static final List<Listener> listeners = new ArrayList<>();
    /** Panel expanded (several lines) or collapsed (newest line only). Kept while the app runs. */
    private static boolean expanded = true;

    private LifecycleLog() {
        // Static helper only - no instances.
    }

    /** Logs one line to Logcat and to the on-screen log, e.g. "HomeActivity → onResume". */
    public static void d(@NonNull String message) {
        Log.d(TAG, message);
        entries.add(new Entry(System.currentTimeMillis(), message));
        if (entries.size() > MAX_LINES) {
            entries.remove(0);
        }
        notifyListeners();
    }

    /** All lines, oldest first (read-only). */
    @NonNull
    public static List<Entry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    /** Empties the on-screen log (Logcat keeps its own copy). */
    public static void clear() {
        entries.clear();
        notifyListeners();
    }

    public static void addListener(@NonNull Listener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public static void removeListener(@NonNull Listener listener) {
        listeners.remove(listener);
    }

    private static void notifyListeners() {
        // Copy first: a listener may remove itself while we loop
        for (Listener listener : new ArrayList<>(listeners)) {
            listener.onLogChanged();
        }
    }

    // ---------------------------------------------------------------------
    // Settings
    // ---------------------------------------------------------------------

    /** True if the user switched the log panel on (Home screen button). Off by default. */
    public static boolean isPanelOn(@NonNull Context context) {
        return context.getSharedPreferences(GmiApp.PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_PANEL_ON, false);
    }

    /** Saves the on/off choice, so it is remembered after the app is closed. */
    public static void setPanelOn(@NonNull Context context, boolean on) {
        context.getSharedPreferences(GmiApp.PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_PANEL_ON, on)
                .apply();
    }

    public static boolean isExpanded() {
        return expanded;
    }

    public static void setExpanded(boolean value) {
        expanded = value;
    }
}
