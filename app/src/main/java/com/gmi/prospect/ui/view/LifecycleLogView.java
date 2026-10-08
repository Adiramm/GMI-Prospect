package com.gmi.prospect.ui.view;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.gmi.prospect.R;
import com.gmi.prospect.base.LifecycleLog;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * The on-screen lifecycle log panel (layout: view_lifecycle_log.xml).
 * <p>
 * It shows the lines collected by {@link LifecycleLog}: time, then the screen
 * name in blue, then the callback. The newest line flashes red for a moment.
 * Expanded = the last few lines (6 in portrait, 3 in landscape - see
 * res/values-land/integers.xml), scrollable; collapsed = only the newest line.
 * <p>
 * BaseActivity calls {@link #startListening()} in onStart and
 * {@link #stopListening()} in onStop, so the panel only updates while its
 * screen is visible. When the screen comes back it simply redraws everything.
 */
public class LifecycleLogView extends LinearLayout implements LifecycleLog.Listener {

    private static final long FLASH_MS = 900;

    /** Minutes:seconds.milliseconds - enough to see onPause → onStop timing. */
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("mm:ss.SSS", Locale.US);

    private TextView textLog;
    private ScrollView scrollLog;
    private ImageButton buttonToggle;
    private int basePaddingBottom;
    private boolean flashNewest;
    private final Runnable endFlash = () -> {
        flashNewest = false;
        render();
    };

    public LifecycleLogView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        textLog = findViewById(R.id.textLog);
        scrollLog = findViewById(R.id.scrollLog);
        buttonToggle = findViewById(R.id.buttonLogToggle);
        basePaddingBottom = getPaddingBottom();

        findViewById(R.id.buttonLogClear).setOnClickListener(v -> LifecycleLog.clear());
        buttonToggle.setOnClickListener(v -> {
            LifecycleLog.setExpanded(!LifecycleLog.isExpanded());
            render();
        });
    }

    /** Start following new lines (from Activity.onStart) and draw what is there now. */
    public void startListening() {
        LifecycleLog.addListener(this);
        render();
    }

    /** Stop following (from Activity.onStop): no work for a screen nobody can see. */
    public void stopListening() {
        LifecycleLog.removeListener(this);
        removeCallbacks(endFlash);
        flashNewest = false;
    }

    /** Extra space at the bottom for the navigation bar (edge-to-edge). */
    public void setBottomInset(int insetPx) {
        setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(), basePaddingBottom + insetPx);
    }

    @Override
    public void onLogChanged() {
        flashNewest = true;
        render();
        removeCallbacks(endFlash);
        postDelayed(endFlash, FLASH_MS);
    }

    // ---------------------------------------------------------------------
    // Drawing
    // ---------------------------------------------------------------------

    private void render() {
        boolean expanded = LifecycleLog.isExpanded();
        List<LifecycleLog.Entry> entries = LifecycleLog.getEntries();

        SpannableStringBuilder text = new SpannableStringBuilder();
        if (entries.isEmpty()) {
            append(text, getContext().getString(R.string.log_empty), R.color.log_muted);
        } else {
            int first = expanded ? 0 : entries.size() - 1; // collapsed: newest line only
            for (int i = first; i < entries.size(); i++) {
                if (text.length() > 0) {
                    text.append('\n');
                }
                appendEntry(text, entries.get(i), flashNewest && i == entries.size() - 1);
            }
        }
        textLog.setText(text);

        // Expanded: a fixed window of a few lines that scrolls; collapsed: one line, cut with "…"
        textLog.setMaxLines(expanded ? Integer.MAX_VALUE : 1);
        textLog.setEllipsize(expanded ? null : TextUtils.TruncateAt.END);
        ViewGroup.LayoutParams params = scrollLog.getLayoutParams();
        params.height = expanded
                ? textLog.getLineHeight() * getResources().getInteger(R.integer.log_visible_lines)
                : ViewGroup.LayoutParams.WRAP_CONTENT;
        scrollLog.setLayoutParams(params);

        buttonToggle.setRotation(expanded ? 0f : 180f);
        buttonToggle.setContentDescription(getContext().getString(
                expanded ? R.string.cd_log_collapse : R.string.cd_log_expand));

        // Keep the newest line in view (after the new text has been laid out)
        scrollLog.post(() -> scrollLog.scrollTo(0, textLog.getHeight()));
    }

    /** "05:31.204  HomeActivity → onResume" with the screen name in blue. */
    private void appendEntry(SpannableStringBuilder text, LifecycleLog.Entry entry, boolean newest) {
        append(text, timeFormat.format(entry.timeMillis) + "  ", R.color.log_muted);
        String message = entry.message;
        if (newest) {
            append(text, message, R.color.log_newest);
            return;
        }
        int space = message.indexOf(' ');
        String name = space > 0 ? message.substring(0, space) : message;
        append(text, name, R.color.log_class);
        append(text, message.substring(name.length()), R.color.log_text);
    }

    private void append(SpannableStringBuilder text, String part, @ColorRes int colorRes) {
        int start = text.length();
        text.append(part);
        text.setSpan(new ForegroundColorSpan(ContextCompat.getColor(getContext(), colorRes)),
                start, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
}
