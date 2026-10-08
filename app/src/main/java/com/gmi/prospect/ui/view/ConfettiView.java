package com.gmi.prospect.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.gmi.prospect.R;
import com.gmi.prospect.base.LifecycleLog;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Full-screen confetti overlay drawn on a Canvas (no library).
 * <p>
 * Rectangles and circles in GMI blue, GMI red and yellow fall from the top for
 * about 3 seconds, spinning and "flipping" as they go, then fade out.
 * <p>
 * How it is driven (lifecycle rule #5):
 * <ul>
 *     <li>{@link #burst()} - queue one round of confetti (does not move yet)</li>
 *     <li>{@link #start()} - from Activity.onResume: animate</li>
 *     <li>{@link #stop()} - from Activity.onPause: freeze, no more frames</li>
 * </ul>
 * Each frame asks for the next one with postInvalidateOnAnimation(). After
 * stop() no new frame is requested, so the view uses no CPU in the background.
 * Play time only counts while running, so a paused burst continues where it left off.
 */
public class ConfettiView extends View {


    private static final long DURATION_MS = 3000;
    private static final long FADE_MS = 600;
    private static final int PARTICLE_COUNT = 140;
    /** Downward acceleration, in dp per second². */
    private static final float GRAVITY_DP = 250f;

    /** One piece of confetti. Positions in px, speeds in px per second. */
    private static class Particle {
        float x, y;
        float speedX, speedY;
        float size;
        float rotation, rotationSpeed;   // degrees, degrees per second
        float flip, flipSpeed;           // radians - squashes rectangles to look 3D
        int color;
        boolean circle;
    }

    private final List<Particle> particles = new ArrayList<>();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random random = new Random();
    private final float density;
    private final int[] colors;

    private boolean burstPending;   // burst() called, particles made on the first frame (size known then)
    private boolean running;
    private long elapsedMs;         // play time so far (does not grow while stopped)
    private long lastFrameMs;       // time of the previous frame; 0 = first frame after start()

    public ConfettiView(@NonNull Context context) {
        this(context, null);
    }

    public ConfettiView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        colors = new int[]{
                ContextCompat.getColor(context, R.color.blue_300),
                ContextCompat.getColor(context, R.color.blue_500),
                ContextCompat.getColor(context, R.color.red_400),
                ContextCompat.getColor(context, R.color.red_500),
                ContextCompat.getColor(context, R.color.confetti_yellow)
        };
    }

    // ---------------------------------------------------------------------
    // Public controls
    // ---------------------------------------------------------------------

    /** Queues one 3-second burst. It starts moving on the next {@link #start()}. */
    public void burst() {
        particles.clear();
        elapsedMs = 0;
        burstPending = true;
    }

    /** Animates (call from onResume). Does nothing if there is no confetti left to play. */
    public void start() {
        if (running || (!burstPending && particles.isEmpty())) {
            return;
        }
        running = true;
        lastFrameMs = 0;
        LifecycleLog.d("ConfettiView → start (animating)");
        postInvalidateOnAnimation();
    }

    /** Freezes the animation (call from onPause). No more frames are requested. */
    public void stop() {
        if (!running) {
            return;
        }
        running = false;
        LifecycleLog.d("ConfettiView → stop (paused at " + elapsedMs + " ms)");
    }

    // Safety net: never keep animating a view that is no longer on screen.
    @Override
    protected void onDetachedFromWindow() {
        stop();
        super.onDetachedFromWindow();
    }

    // ---------------------------------------------------------------------
    // Drawing
    // ---------------------------------------------------------------------

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (burstPending && getWidth() > 0) {
            createParticles();
            burstPending = false;
        }
        if (running) {
            long now = SystemClock.uptimeMillis();
            // Cap the step so a slow frame does not make the confetti jump
            long deltaMs = lastFrameMs == 0 ? 16 : Math.min(now - lastFrameMs, 50);
            lastFrameMs = now;
            update(deltaMs);
        }

        int alpha = currentAlpha();
        for (Particle p : particles) {
            drawParticle(canvas, p, alpha);
        }

        if (running) {
            if (elapsedMs >= DURATION_MS) {
                particles.clear();
                running = false;
                LifecycleLog.d("ConfettiView → finished");
            } else {
                postInvalidateOnAnimation(); // ask for the next frame
            }
        }
    }

    /** Fully visible, then fades out during the last FADE_MS. */
    private int currentAlpha() {
        long fadeStart = DURATION_MS - FADE_MS;
        if (elapsedMs <= fadeStart) {
            return 255;
        }
        float progress = Math.min(1f, (elapsedMs - fadeStart) / (float) FADE_MS);
        return (int) (255 * (1f - progress));
    }

    private void drawParticle(Canvas canvas, Particle p, int alpha) {
        paint.setColor(p.color);
        paint.setAlpha(alpha);
        canvas.save();
        canvas.translate(p.x, p.y);
        canvas.rotate(p.rotation);
        if (p.circle) {
            canvas.drawCircle(0, 0, p.size * 0.35f, paint);
        } else {
            // cos(flip) shrinks the width back and forth: looks like a paper strip flipping
            float halfWidth = p.size / 2f * Math.abs((float) Math.cos(p.flip));
            float halfHeight = p.size * 0.3f;
            canvas.drawRect(-halfWidth, -halfHeight, halfWidth, halfHeight, paint);
        }
        canvas.restore();
    }

    // ---------------------------------------------------------------------
    // Physics
    // ---------------------------------------------------------------------

    /** Moves every particle forward by {@code deltaMs}. */
    private void update(long deltaMs) {
        elapsedMs += deltaMs;
        float seconds = deltaMs / 1000f;
        float gravity = GRAVITY_DP * density;
        for (Particle p : particles) {
            p.speedY += gravity * seconds;
            p.x += p.speedX * seconds;
            p.y += p.speedY * seconds;
            p.rotation += p.rotationSpeed * seconds;
            p.flip += p.flipSpeed * seconds;
        }
    }

    /** Spreads particles across the width, just above the top edge. */
    private void createParticles() {
        particles.clear();
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            Particle p = new Particle();
            p.x = random.nextFloat() * getWidth();
            p.y = -randomRange(0, 400) * density;          // staggered, so they arrive in waves
            p.speedX = randomRange(-60, 60) * density;
            p.speedY = randomRange(100, 250) * density;
            p.size = randomRange(8, 14) * density;
            p.rotation = random.nextFloat() * 360f;
            p.rotationSpeed = randomRange(-360, 360);
            p.flip = random.nextFloat() * (float) Math.PI;
            p.flipSpeed = randomRange(3, 9);
            p.color = colors[random.nextInt(colors.length)];
            p.circle = random.nextInt(3) == 0;               // about 1 in 3 are circles
            particles.add(p);
        }
    }

    private float randomRange(float min, float max) {
        return min + random.nextFloat() * (max - min);
    }
}
