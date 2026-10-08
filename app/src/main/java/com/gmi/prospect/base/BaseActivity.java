package com.gmi.prospect.base;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.gmi.prospect.R;
import com.google.android.material.appbar.MaterialToolbar;

/**
 * Parent of every screen in the app.
 * <p>
 * It overrides each Activity lifecycle callback and logs it, so we can watch
 * the lifecycle in Logcat (filter by tag {@code LIFECYCLE}). Child Activities
 * override the same methods to do their own work, and must always call
 * {@code super.onXxx()} so the log line still appears.
 */
public abstract class BaseActivity extends AppCompatActivity {

    /** Logcat tag used for all lifecycle messages. */
    protected static final String TAG = "LIFECYCLE";

    /** Writes one lifecycle line, e.g. "HomeActivity → onResume". */
    private void logLifecycle(String callback) {
        Log.d(TAG, getClass().getSimpleName() + " → " + callback);
    }

    // Called once when the Activity is created (or re-created after rotation /
    // theme / language change). Children inflate their layout and restore
    // saved state from savedInstanceState here.
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Draw behind the status/navigation bars on every Android version
        // (Android 15+ forces this anyway). Layouts add padding for the bars.
        // Called AFTER super.onCreate: calling it before left the launch
        // screen's window hidden (black) on the Android 17 emulator.
        EdgeToEdge.enable(this);
        logLifecycle("onCreate");
    }

    // The Activity is about to become visible. Good place to start work that
    // only matters while the user can see the screen (e.g. the Splash timer).
    @Override
    protected void onStart() {
        super.onStart();
        logLifecycle("onStart");
    }

    // Called only when coming back from the stopped state (e.g. returning from
    // the Home screen or another app), just before onStart.
    @Override
    protected void onRestart() {
        super.onRestart();
        logLifecycle("onRestart");
    }

    // The Activity is in the foreground and the user can interact with it.
    // Start animations / restore drafts here (e.g. ConfettiView, form draft).
    @Override
    protected void onResume() {
        super.onResume();
        logLifecycle("onResume");
    }

    // The user is leaving (another screen, a dialog, or the app goes to the
    // background). Pause animations and save drafts here - it is the last
    // callback guaranteed to run before the process may be killed.
    @Override
    protected void onPause() {
        super.onPause();
        logLifecycle("onPause");
    }

    // The Activity is no longer visible. Stop timers and other background work
    // so the app never does anything the user cannot see.
    @Override
    protected void onStop() {
        super.onStop();
        logLifecycle("onStop");
    }

    // Called before the Activity may be destroyed and re-created (rotation,
    // dark mode, language). Children put their UI state into outState here and
    // read it back in onCreate.
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        logLifecycle("onSaveInstanceState");
    }

    // Final callback: the Activity is finishing (back pressed / finish()) or
    // being re-created. Release anything that could leak (handlers, listeners).
    @Override
    protected void onDestroy() {
        super.onDestroy();
        logLifecycle("onDestroy");
    }

    // ---------------------------------------------------------------------
    // Shared helpers (not lifecycle)
    // ---------------------------------------------------------------------

    /**
     * Sets up the top app bar of an inner screen: title + back arrow.
     * The layout must contain a MaterialToolbar with id "toolbar".
     */
    protected void setupToolbar(@StringRes int titleRes) {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(titleRes);
        // Back arrow behaves exactly like the system Back button
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
    }

    /**
     * Makes the status bar icons (clock, battery) white. Used by screens whose
     * top is the blue gradient, in both light and dark mode.
     *
     * @param alsoNavigationBar true to make the bottom navigation bar icons white too
     */
    protected void useWhiteSystemBarIcons(boolean alsoNavigationBar) {
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(false);
        if (alsoNavigationBar) {
            controller.setAppearanceLightNavigationBars(false);
        }
    }

    /**
     * Entrance animation: each view fades in and slides up (res/anim/fade_slide_up.xml),
     * starting {@code staggerMs} after the previous one.
     */
    protected void playEntranceAnimation(long staggerMs, View... views) {
        for (int i = 0; i < views.length; i++) {
            Animation animation = AnimationUtils.loadAnimation(this, R.anim.fade_slide_up);
            animation.setStartOffset(i * staggerMs);
            views[i].startAnimation(animation);
        }
    }
}
