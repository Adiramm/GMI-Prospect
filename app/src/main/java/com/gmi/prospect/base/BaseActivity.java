package com.gmi.prospect.base;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.LayoutRes;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.gmi.prospect.R;
import com.gmi.prospect.ui.view.LifecycleLogView;
import com.google.android.material.appbar.MaterialToolbar;

/**
 * Parent of every screen in the app.
 * <p>
 * It overrides each Activity lifecycle callback and logs it, so we can watch
 * the lifecycle in Logcat (filter by tag {@code LIFECYCLE}) and in the
 * on-screen log panel. Child Activities override the same methods to do their
 * own work, and must always call {@code super.onXxx()} so the log line still appears.
 * <p>
 * It also adds the lifecycle log panel under every screen (see
 * {@link #setContentView(int)}); the panel is only shown when switched on in Home.
 */
public abstract class BaseActivity extends AppCompatActivity {

    /** Panel under the screen (null until setContentView has been called). */
    private LifecycleLogView logPanel;

    /** Writes one lifecycle line, e.g. "HomeActivity → onResume". */
    private void logLifecycle(String callback) {
        LifecycleLog.d(getClass().getSimpleName() + " → " + callback);
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
    // Also the moment the log panel starts following new lines (and is shown
    // or hidden to match the setting), because the screen is now visible.
    @Override
    protected void onStart() {
        super.onStart();
        logLifecycle("onStart");
        refreshLogPanel();
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
    // The log panel stops following new lines too: nobody can see it.
    @Override
    protected void onStop() {
        super.onStop();
        logLifecycle("onStop");
        if (logPanel != null) {
            logPanel.stopListening();
        }
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
    // Lifecycle log panel
    // ---------------------------------------------------------------------

    /**
     * Every screen calls this with its layout. We put that layout in a column
     * with the log panel underneath: when the panel is shown, the screen just
     * gets shorter, so the panel never covers a button.
     */
    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        View screen = getLayoutInflater().inflate(layoutResID, column, false);
        column.addView(screen, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        logPanel = (LifecycleLogView) getLayoutInflater()
                .inflate(R.layout.view_lifecycle_log, column, false);
        column.addView(logPanel);
        super.setContentView(column);
        applyLogPanelInsets(column, screen);
    }

    /**
     * Shows or hides the panel to match the setting and (re)starts it. Called
     * in onStart, and by Home right after its ">_" button is tapped.
     */
    protected void refreshLogPanel() {
        if (logPanel == null) {
            return;
        }
        boolean on = LifecycleLog.isPanelOn(this);
        logPanel.setVisibility(on ? View.VISIBLE : View.GONE);
        if (on) {
            logPanel.startListening();
        } else {
            logPanel.stopListening();
        }
        ViewCompat.requestApplyInsets(logPanel.getRootView());
    }

    /**
     * Edge-to-edge: the navigation bar is at the very bottom. While the panel
     * is shown, the PANEL makes room for it and the screen above is told there
     * is no navigation bar - otherwise there would be an empty gap between them.
     */
    private void applyLogPanelInsets(View column, View screen) {
        ViewCompat.setOnApplyWindowInsetsListener(column, (v, insets) -> {
            WindowInsetsCompat forScreen = insets;
            if (logPanel.getVisibility() == View.VISIBLE) {
                Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                logPanel.setBottomInset(bars.bottom);
                forScreen = new WindowInsetsCompat.Builder(insets)
                        .setInsets(WindowInsetsCompat.Type.systemBars(),
                                Insets.of(bars.left, bars.top, bars.right, 0))
                        .build();
            }
            ViewCompat.dispatchApplyWindowInsets(screen, forScreen);
            return WindowInsetsCompat.CONSUMED;
        });
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
