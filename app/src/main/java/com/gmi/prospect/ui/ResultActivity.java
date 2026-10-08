package com.gmi.prospect.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.IntentCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.gmi.prospect.R;
import com.gmi.prospect.base.BaseActivity;
import com.gmi.prospect.data.Course;
import com.gmi.prospect.data.Interest;
import com.gmi.prospect.data.Level;
import com.gmi.prospect.logic.CourseRanker;
import com.gmi.prospect.logic.EligibilityChecker;
import com.gmi.prospect.logic.EligibilityResult;
import com.gmi.prospect.ui.adapter.ResultAdapter;
import com.gmi.prospect.ui.view.ConfettiView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Result of the eligibility check.
 * <ul>
 *     <li>Eligible for 1+ courses → confetti, "You're eligible for N courses 🎉"
 *         and the courses ranked by the quiz, each with an Apply button.</li>
 *     <li>Eligible for none → friendly message, what is missing per level,
 *         Retake and Contact Us buttons. No confetti.</li>
 * </ul>
 * The Home button is always shown.
 * <p>
 * LIFECYCLE: confetti animates only between onResume and onPause (rule #5),
 * and plays only once - after rotation it is not replayed (saved flag).
 */
public class ResultActivity extends BaseActivity {

    /** Bundle key: true once the confetti has been shown on this screen. */
    private static final String KEY_CONFETTI_SHOWN = "confetti_shown";

    private ConfettiView confettiView;
    private boolean confettiShown;

    // Works out the results from the Intent extras (grades + quiz scores).
    // This is quick, so it is simply redone after every re-creation. The only
    // thing we must remember across re-creation is whether the confetti was
    // already shown - otherwise every rotation would celebrate again.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
        setupToolbar(R.string.title_result);
        confettiView = findViewById(R.id.confettiView);

        Map<Course, EligibilityResult> results = EligibilityChecker.checkAll(readGrades());
        List<Course> eligible = new ArrayList<>();
        for (Map.Entry<Course, EligibilityResult> entry : results.entrySet()) {
            if (entry.getValue().isEligible()) {
                eligible.add(entry.getKey());
            }
        }

        boolean alreadyShown = savedInstanceState != null
                && savedInstanceState.getBoolean(KEY_CONFETTI_SHOWN, false);
        if (eligible.isEmpty()) {
            showNotEligible(results);
        } else {
            showEligible(eligible);
            if (!alreadyShown) {
                confettiView.burst(); // queued only - it starts moving in onResume
            }
        }
        confettiShown = alreadyShown || !eligible.isEmpty();

        findViewById(R.id.buttonHome).setOnClickListener(v -> goHome());
    }

    // The screen is in front and the user can see it: let the confetti move.
    // (If the burst already finished, or there is none, start() does nothing.)
    @Override
    protected void onResume() {
        super.onResume();
        confettiView.start();
    }

    // The user is leaving (Home button, another app, a dialog): freeze the
    // confetti so it uses no CPU while nobody is watching. It continues from
    // the same point in onResume.
    @Override
    protected void onPause() {
        super.onPause();
        confettiView.stop();
    }

    // Before a possible re-creation: remember that the confetti was shown, so
    // onCreate does not queue a new burst after rotation / theme / language change.
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_CONFETTI_SHOWN, confettiShown);
    }

    // ---------------------------------------------------------------------
    // Reading the Intent
    // ---------------------------------------------------------------------

    /** Grades from GradeInputActivity (passed on by QuizActivity). Empty map if missing. */
    @NonNull
    @SuppressWarnings("unchecked")
    private Map<String, String> readGrades() {
        HashMap<String, String> grades = IntentCompat.getSerializableExtra(
                getIntent(), GradeInputActivity.EXTRA_GRADES, HashMap.class);
        return grades != null ? grades : new HashMap<>();
    }

    /** Quiz scores per Interest; all zero if missing (then only the tie-break order applies). */
    @NonNull
    private int[] readScores() {
        int[] scores = getIntent().getIntArrayExtra(QuizActivity.EXTRA_INTEREST_SCORES);
        return scores != null ? scores : new int[Interest.values().length];
    }

    // ---------------------------------------------------------------------
    // Eligible
    // ---------------------------------------------------------------------

    /** Ranks the eligible courses by the quiz and shows them in the RecyclerView. */
    private void showEligible(List<Course> eligible) {
        List<Course> ranked = CourseRanker.rank(eligible, readScores(),
                course -> getString(course.getNameRes()));
        String headline = getResources().getQuantityString(
                R.plurals.result_eligible_headline, ranked.size(), ranked.size());

        RecyclerView recycler = findViewById(R.id.recyclerResults);
        recycler.setAdapter(new ResultAdapter(headline, ranked, this::openAdmissionForm));
        recycler.setVisibility(View.VISIBLE);
    }

    /** Apply → admission form with the chosen course and the grades. */
    private void openAdmissionForm(@NonNull Course course) {
        Intent intent = new Intent(this, AdmissionFormActivity.class);
        intent.putExtra(CourseDetailActivity.EXTRA_COURSE_ID, course.getId());
        intent.putExtra(GradeInputActivity.EXTRA_GRADES, new HashMap<>(readGrades()));
        startActivity(intent);
    }

    // ---------------------------------------------------------------------
    // Not eligible
    // ---------------------------------------------------------------------

    /**
     * One card per level with what is still missing. All courses of a level
     * share the same rule, so a LinkedHashSet removes the repeated lines while
     * keeping their order.
     */
    private void showNotEligible(Map<Course, EligibilityResult> results) {
        ViewGroup container = findViewById(R.id.containerMissingLevels);
        LayoutInflater inflater = LayoutInflater.from(this);

        for (Level level : Level.values()) {
            Set<String> missing = new LinkedHashSet<>();
            for (Map.Entry<Course, EligibilityResult> entry : results.entrySet()) {
                if (entry.getKey().getLevel() == level) {
                    missing.addAll(entry.getValue().getMissingMessages(this));
                }
            }
            if (missing.isEmpty()) {
                continue;
            }
            View card = inflater.inflate(R.layout.item_missing_level, container, false);
            ((TextView) card.findViewById(R.id.textLevelTitle)).setText(level.getLabelRes());
            ViewGroup bullets = card.findViewById(R.id.containerMissing);
            for (String line : missing) {
                View row = inflater.inflate(R.layout.item_requirement, bullets, false);
                ((TextView) row.findViewById(R.id.textRequirement)).setText(line);
                bullets.addView(row);
            }
            container.addView(card);
        }

        findViewById(R.id.buttonRetake).setOnClickListener(v -> retake());
        findViewById(R.id.buttonContact).setOnClickListener(v ->
                startActivity(new Intent(this, ContactActivity.class)));
        findViewById(R.id.scrollNotEligible).setVisibility(View.VISIBLE);
    }

    /**
     * Back to the grade screen that is already in the back stack.
     * CLEAR_TOP closes Quiz and Result; SINGLE_TOP re-uses the existing
     * GradeInputActivity instead of making a new one, so the student's
     * grades are still there to fix.
     */
    private void retake() {
        Intent intent = new Intent(this, GradeInputActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }

    /** Back to Home, closing every screen above it (same flags as retake()). */
    private void goHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }
}
