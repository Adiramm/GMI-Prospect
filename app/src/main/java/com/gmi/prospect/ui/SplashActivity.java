package com.gmi.prospect.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.Lifecycle;

import com.gmi.prospect.R;
import com.gmi.prospect.base.BaseActivity;
import com.gmi.prospect.base.LifecycleLog;

/**
 * Launcher screen: GMI logo + greeting on the blue gradient.
 * <p>
 * It opens Home either when the user taps "Let's go" or automatically after
 * ~2 seconds. The timer is a {@link Handler} + {@link Runnable} tied to the
 * lifecycle: started in onStart, cancelled in onStop, and it only navigates
 * while the screen is resumed - so the app never jumps to Home while the user
 * is leaving or in the background.
 * <p>
 * Lint suggests using only the system splash screen (Android 12+). We keep this
 * Activity on purpose: the assignment's lifecycle rule #4 is demonstrated here.
 * The system splash is still set up in themes.xml, so it shows first on a cold start.
 */
@SuppressLint("CustomSplashScreen")
public class SplashActivity extends BaseActivity {

    /** How long the splash stays before opening Home automatically. */
    private static final long SPLASH_DELAY_MS = 2000;

    /** Delay between each view's entrance animation (staggered effect). */
    private static final long STAGGER_MS = 120;

    /** If the timer fires while we are paused, check again after this long. */
    private static final long RETRY_MS = 300;

    // Handler on the main (UI) thread - it runs timerRunnable after the delay.
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable timerRunnable = this::onTimerFinished;

    // Builds the UI and plays the entrance animation. The timer is NOT started
    // here: onCreate runs only once, but the screen can become visible again
    // (onStart) after being in the background.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        useWhiteSystemBarIcons(true);

        findViewById(R.id.buttonGo).setOnClickListener(v -> openHome());

        playEntranceAnimation(STAGGER_MS,
                findViewById(R.id.imageLogo),
                findViewById(R.id.textGreeting),
                findViewById(R.id.textSubtitle),
                findViewById(R.id.buttonGo));
    }

    // Screen is becoming visible: (re)start the 2-second timer. Because this
    // also runs after onRestart, coming back from the background gives the
    // user a fresh 2 seconds instead of jumping straight to Home.
    @Override
    protected void onStart() {
        super.onStart();
        handler.postDelayed(timerRunnable, SPLASH_DELAY_MS);
        LifecycleLog.d("SplashActivity: timer started (" + SPLASH_DELAY_MS + " ms)");
    }

    // Screen is no longer visible: cancel the pending timer so Home is never
    // opened while the user is in another app.
    @Override
    protected void onStop() {
        super.onStop();
        handler.removeCallbacks(timerRunnable);
        LifecycleLog.d("SplashActivity: timer cancelled");
    }

    /**
     * Runs when the 2 seconds are up.
     * <p>
     * Why the check? Leaving the app calls onPause at once, but on Android 10+
     * onStop can come up to ~1 second later (after the "go home" animation).
     * If the timer fires inside that gap we must NOT open Home. So we only
     * navigate while RESUMED; otherwise we check again shortly - onStop will
     * normally cancel that retry, and onStart starts a fresh timer when the
     * user comes back.
     */
    private void onTimerFinished() {
        if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
            openHome();
        } else {
            LifecycleLog.d("SplashActivity: timer fired while paused - not opening Home");
            handler.postDelayed(timerRunnable, RETRY_MS);
        }
    }

    /**
     * Opens Home and closes the splash with finish(), so pressing Back on Home
     * leaves the app instead of returning to the splash.
     */
    private void openHome() {
        if (isFinishing()) {
            return; // Already leaving (e.g. button tapped twice quickly)
        }
        handler.removeCallbacks(timerRunnable); // Button tapped before the timer fired
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }
}
