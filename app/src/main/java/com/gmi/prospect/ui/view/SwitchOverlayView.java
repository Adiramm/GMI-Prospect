package com.gmi.prospect.ui.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Region;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Animates a theme or language switch.
 * <p>
 * Switching theme or language DESTROYS and RE-CREATES the Activity, so there
 * is nothing on screen to animate "from". The trick:
 * <ol>
 *     <li>Before the switch, {@link #capture} takes a picture of the old screen
 *         and keeps it in a static field (static fields survive re-creation,
 *         because the app process stays alive).</li>
 *     <li>In the NEW Activity's onCreate, {@link #playIfPending} lays that
 *         picture over the new screen, so the very first frame still looks
 *         like the old one - no flicker.</li>
 *     <li>The picture is then animated away, revealing the new screen:
 *         {@link #STYLE_REVEAL} = growing circle from the tapped button (theme),
 *         {@link #STYLE_FADE} = cross-fade (language).</li>
 * </ol>
 */
public class SwitchOverlayView extends View {

    /** New screen grows out of the tapped button as a circle. */
    public static final int STYLE_REVEAL = 0;
    /** Old screen fades out over the new one. */
    public static final int STYLE_FADE = 1;

    private static final long REVEAL_MS = 550;
    private static final long FADE_MS = 350;

    // Waiting between capture() (old Activity) and playIfPending() (new Activity)
    @Nullable
    private static Bitmap pendingSnapshot;
    private static int pendingStyle;
    private static float pendingCenterX;
    private static float pendingCenterY;

    private final Bitmap snapshot;
    private final float centerX;
    private final float centerY;
    private final Path holePath = new Path();
    private float holeRadius;

    private SwitchOverlayView(@NonNull Context context, @NonNull Bitmap snapshot,
                              float centerX, float centerY) {
        super(context);
        this.snapshot = snapshot;
        this.centerX = centerX;
        this.centerY = centerY;
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    // ---------------------------------------------------------------------
    // Step 1: in the OLD Activity, just before switching
    // ---------------------------------------------------------------------

    /**
     * Takes a picture of the whole window and remembers where the tapped
     * button is. Call this right BEFORE changing the theme or language.
     *
     * @param origin the button that was tapped (centre of the reveal circle)
     * @param style  {@link #STYLE_REVEAL} or {@link #STYLE_FADE}
     */
    public static void capture(@NonNull Activity activity, @NonNull View origin, int style) {
        View decor = activity.getWindow().getDecorView();
        if (decor.getWidth() == 0 || decor.getHeight() == 0) {
            return; // not laid out yet - just switch without animation
        }
        clearPending();
        Bitmap bitmap = Bitmap.createBitmap(decor.getWidth(), decor.getHeight(), Bitmap.Config.ARGB_8888);
        decor.draw(new Canvas(bitmap));

        int[] location = new int[2];
        origin.getLocationInWindow(location);
        pendingSnapshot = bitmap;
        pendingStyle = style;
        pendingCenterX = location[0] + origin.getWidth() / 2f;
        pendingCenterY = location[1] + origin.getHeight() / 2f;
    }

    // ---------------------------------------------------------------------
    // Step 2: in the NEW Activity's onCreate, after setContentView
    // ---------------------------------------------------------------------

    /** If a snapshot is waiting, covers the new screen with it and animates it away. */
    public static void playIfPending(@NonNull Activity activity) {
        Bitmap snapshot = pendingSnapshot;
        if (snapshot == null) {
            return; // normal start - nothing to animate
        }
        pendingSnapshot = null;

        ViewGroup decor = (ViewGroup) activity.getWindow().getDecorView();
        SwitchOverlayView overlay = new SwitchOverlayView(activity, snapshot, pendingCenterX, pendingCenterY);
        decor.addView(overlay, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        if (pendingStyle == STYLE_REVEAL) {
            overlay.playReveal();
        } else {
            overlay.playFade();
        }
    }

    /** Drops a snapshot that was never used, so it does not waste memory. */
    private static void clearPending() {
        if (pendingSnapshot != null) {
            pendingSnapshot.recycle();
            pendingSnapshot = null;
        }
    }

    // ---------------------------------------------------------------------
    // Animations
    // ---------------------------------------------------------------------

    /** Cuts a growing circular hole in the old picture until the new screen shows fully. */
    private void playReveal() {
        // Radius that reaches the farthest corner of the screen from the button
        float maxRadius = 0;
        float[][] corners = {{0, 0}, {snapshot.getWidth(), 0},
                {0, snapshot.getHeight()}, {snapshot.getWidth(), snapshot.getHeight()}};
        for (float[] corner : corners) {
            maxRadius = Math.max(maxRadius, (float) Math.hypot(corner[0] - centerX, corner[1] - centerY));
        }

        ValueAnimator animator = ValueAnimator.ofFloat(0f, maxRadius);
        animator.setDuration(REVEAL_MS);
        animator.setInterpolator(new DecelerateInterpolator(1.5f));
        animator.addUpdateListener(a -> {
            holeRadius = (float) a.getAnimatedValue();
            invalidate();
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                removeSelf();
            }
        });
        animator.start();
    }

    /** Fades the old picture out over the new screen. */
    private void playFade() {
        animate().alpha(0f)
                .setDuration(FADE_MS)
                .withEndAction(this::removeSelf)
                .start();
    }

    /** Animation done: take the overlay off the screen and free the bitmap. */
    private void removeSelf() {
        ViewGroup parent = (ViewGroup) getParent();
        if (parent != null) {
            parent.removeView(this);
        }
        snapshot.recycle();
    }

    @Override
    @SuppressWarnings("deprecation") // clipPath(.., DIFFERENCE) is only used on Android 7.x (API 24-25)
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (snapshot.isRecycled()) {
            return;
        }
        canvas.save();
        if (holeRadius > 0) {
            holePath.reset();
            holePath.addCircle(centerX, centerY, holeRadius, Path.Direction.CW);
            // Draw everything EXCEPT the circle, so the new screen shows through it
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                canvas.clipOutPath(holePath);
            } else {
                canvas.clipPath(holePath, Region.Op.DIFFERENCE);
            }
        }
        canvas.drawBitmap(snapshot, 0, 0, null);
        canvas.restore();
    }
}
