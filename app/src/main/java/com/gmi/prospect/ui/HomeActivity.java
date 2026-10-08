package com.gmi.prospect.ui;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Layout;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.os.LocaleListCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.gmi.prospect.GmiApp;
import com.gmi.prospect.R;
import com.gmi.prospect.base.BaseActivity;
import com.gmi.prospect.base.LifecycleLog;
import com.gmi.prospect.ui.view.SwitchOverlayView;
import com.google.android.material.button.MaterialButton;

/**
 * Main menu: gradient header with the lifecycle log, theme and language
 * toggles, plus a 2x2 grid of cards (Courses, Check Eligibility, About Us,
 * Contact Us).
 * <p>
 * Both toggles RECREATE the Activity (onPause → onStop → onSaveInstanceState →
 * onDestroy → onCreate → onStart → onResume) - watch it in Logcat. The switch
 * is animated with {@link SwitchOverlayView}: a picture of the old screen is
 * taken before the switch and animated away in the new Activity's onCreate.
 */
public class HomeActivity extends BaseActivity {

    /** Delay between each view's entrance animation (staggered effect). */
    private static final long STAGGER_MS = 70;

    // Builds the UI. savedInstanceState == null means a fresh start; non-null
    // means Android re-created us (rotation, theme or language change), so we
    // skip the entrance animation and, after a theme/language switch, play the
    // switch animation instead. It must start HERE, right after setContentView,
    // so the new Activity's very first frame already shows the old screen
    // picture on top - otherwise the new colours would flash for a moment.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        SwitchOverlayView.playIfPending(this);
        useWhiteSystemBarIcons(false);
        applyWindowInsets();

        setupLogToggle();
        setupThemeToggle();
        setupLanguageToggle();
        setupCards();
        placeRedDot();

        if (savedInstanceState == null) {
            playEntranceAnimation(STAGGER_MS,
                    findViewById(R.id.textGreeting),
                    findViewById(R.id.redDot),
                    findViewById(R.id.textSubtitle),
                    findViewById(R.id.textSectionTitle),
                    findViewById(R.id.row1),
                    findViewById(R.id.row2));
        }
    }

    // ---------------------------------------------------------------------
    // Lifecycle log (">_" button)
    // ---------------------------------------------------------------------

    /**
     * Switches the on-screen lifecycle log on or off. The choice is saved, and
     * every screen shows or hides the panel in its onStart. No recreation is
     * needed: this screen updates its own panel straight away.
     */
    private void setupLogToggle() {
        MaterialButton buttonLog = findViewById(R.id.buttonLog);
        showLogToggleState(buttonLog);
        buttonLog.setOnClickListener(v -> {
            LifecycleLog.setPanelOn(this, !LifecycleLog.isPanelOn(this));
            showLogToggleState(buttonLog);
            refreshLogPanel();
        });
    }

    /** On = solid white circle with a blue icon; off = the see-through header style. */
    private void showLogToggleState(MaterialButton buttonLog) {
        boolean on = LifecycleLog.isPanelOn(this);
        buttonLog.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this,
                on ? R.color.white : R.color.header_overlay)));
        buttonLog.setIconTint(ColorStateList.valueOf(ContextCompat.getColor(this,
                on ? R.color.blue_500 : R.color.on_header)));
        buttonLog.setContentDescription(getString(on ? R.string.cd_log_hide : R.string.cd_log_show));
    }

    // ---------------------------------------------------------------------
    // Theme (light / dark)
    // ---------------------------------------------------------------------

    /** True if the screen is currently drawn in dark mode. */
    private boolean isNightMode() {
        int uiMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return uiMode == Configuration.UI_MODE_NIGHT_YES;
    }

    /** The button shows the mode you will switch TO: moon in light mode, sun in dark mode. */
    private void setupThemeToggle() {
        MaterialButton buttonTheme = findViewById(R.id.buttonTheme);
        boolean night = isNightMode();
        buttonTheme.setIconResource(night ? R.drawable.ic_light_mode : R.drawable.ic_dark_mode);
        buttonTheme.setContentDescription(getString(
                night ? R.string.cd_switch_to_light : R.string.cd_switch_to_dark));

        buttonTheme.setOnClickListener(v -> {
            int newMode = night
                    ? AppCompatDelegate.MODE_NIGHT_NO
                    : AppCompatDelegate.MODE_NIGHT_YES;
            // Picture of the old screen, revealed away in a circle from this button
            SwitchOverlayView.capture(this, v, SwitchOverlayView.STYLE_REVEAL);
            // Saves the choice (GmiApp re-applies it on next launch) and
            // applies it now, which recreates this Activity.
            GmiApp.setNightMode(this, newMode);
        });
    }

    // ---------------------------------------------------------------------
    // Language (English / Bahasa Melayu)
    // ---------------------------------------------------------------------

    /**
     * R.string.lang_code is "en" in values/ and "ms" in values-ms/, so it tells
     * us which language is on screen right now. AppCompat saves the new choice
     * itself (autoStoreLocales in the manifest) and recreates the Activity.
     */
    private void setupLanguageToggle() {
        findViewById(R.id.buttonLanguage).setOnClickListener(v -> {
            boolean isMalay = "ms".equals(getString(R.string.lang_code));
            String newLanguage = isMalay ? "en" : "ms";
            // Picture of the old screen, cross-faded into the new language
            SwitchOverlayView.capture(this, v, SwitchOverlayView.STYLE_FADE);
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(newLanguage));
        });
    }

    // ---------------------------------------------------------------------
    // Cards
    // ---------------------------------------------------------------------

    /** Each card opens its screen with an explicit Intent. */
    private void setupCards() {
        findViewById(R.id.cardCourses).setOnClickListener(v -> open(CourseListActivity.class));
        findViewById(R.id.cardEligibility).setOnClickListener(v -> open(GradeInputActivity.class));
        findViewById(R.id.cardAbout).setOnClickListener(v -> open(AboutActivity.class));
        findViewById(R.id.cardContact).setOnClickListener(v -> open(ContactActivity.class));
    }

    private void open(Class<?> activityClass) {
        startActivity(new Intent(this, activityClass));
    }

    // ---------------------------------------------------------------------
    // Layout helpers
    // ---------------------------------------------------------------------

    /**
     * Moves the red dot so it sits just above the LAST letter of the greeting,
     * like the dot on the "i" in the GMI logo. Both languages end with "GMI",
     * so that letter is always the "I". It is recalculated after every layout,
     * because where the text ends depends on the language and screen width.
     */
    private void placeRedDot() {
        TextView greeting = findViewById(R.id.textGreeting);
        View dot = findViewById(R.id.redDot);
        int dotSize = dot.getLayoutParams().width;
        float gap = 3 * getResources().getDisplayMetrics().density;

        greeting.addOnLayoutChangeListener((v, left, top, right, bottom,
                                            oldLeft, oldTop, oldRight, oldBottom) -> {
            Layout layout = greeting.getLayout();
            CharSequence text = greeting.getText();
            if (layout == null || text.length() == 0) {
                return;
            }
            int last = text.length() - 1;
            int line = layout.getLineForOffset(last);

            // Centre of the last letter, measured from the greeting's left edge
            float letterLeft = layout.getPrimaryHorizontal(last);
            float letterWidth = greeting.getPaint().measureText(text, last, last + 1);
            float centerX = greeting.getPaddingLeft() + letterLeft + letterWidth / 2f;

            // Top of the capital letters on that line (bounds.top is above the baseline, so negative)
            Rect bounds = new Rect();
            greeting.getPaint().getTextBounds("I", 0, 1, bounds);
            float capTop = greeting.getPaddingTop() + layout.getLineBaseline(line) + bounds.top;

            // The dot's XML position is the greeting's top-left corner; shift it from there
            dot.setTranslationX(centerX - dotSize / 2f);
            dot.setTranslationY(capTop - gap - dotSize);
        });
    }

    /**
     * Edge-to-edge: the gradient goes behind the status bar, so we push the
     * header content down by the status-bar height and give the scroll content
     * extra bottom space for the navigation bar.
     */
    private void applyWindowInsets() {
        View root = findViewById(R.id.homeRoot);
        View header = findViewById(R.id.header);
        View content = findViewById(R.id.content);
        // Remember the padding from the XML so we add to it, not replace it
        int headerTop = header.getPaddingTop();
        int contentBottom = content.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            root.setPadding(bars.left, 0, bars.right, 0);
            header.setPadding(header.getPaddingLeft(), headerTop + bars.top,
                    header.getPaddingRight(), header.getPaddingBottom());
            content.setPadding(content.getPaddingLeft(), content.getPaddingTop(),
                    content.getPaddingRight(), contentBottom + bars.bottom);
            return insets;
        });
    }
}
