package com.gmi.prospect.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.StringRes;

import com.gmi.prospect.R;
import com.gmi.prospect.base.BaseActivity;
import com.gmi.prospect.data.CourseRepository;

/**
 * About GMI: gradient hero card, a short introduction and four key-fact cards.
 * <p>
 * Everything shown comes from resources (or CourseRepository), so this screen
 * has no state of its own to save: after rotation or a theme/language change,
 * onCreate simply builds it again - already in the new language.
 */
public class AboutActivity extends BaseActivity {

    /** Delay between each section's entrance animation (staggered effect). */
    private static final long STAGGER_MS = 70;

    // Builds the screen. The entrance animation only plays on a fresh start
    // (savedInstanceState == null), not when Android re-creates the Activity
    // after rotation - the user has already seen it, so it would just be noise.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        setupToolbar(R.string.title_about);
        showFacts();

        if (savedInstanceState == null) {
            playEntranceAnimation(STAGGER_MS,
                    findViewById(R.id.heroCard),
                    findViewById(R.id.sectionIntro),
                    findViewById(R.id.textFactsTitle),
                    findViewById(R.id.rowFacts1),
                    findViewById(R.id.rowFacts2));
        }
    }

    /** Fills the four included item_about_fact cards. */
    private void showFacts() {
        setFact(R.id.factFounded,
                getString(R.string.about_fact_founded_value), R.string.about_fact_founded_label);
        setFact(R.id.factPartnership,
                getString(R.string.about_fact_partnership_value), R.string.about_fact_partnership_label);
        // Counted from the course data, so it stays correct if courses are added or removed
        setFact(R.id.factProgrammes,
                String.valueOf(CourseRepository.getAllCourses().size()), R.string.about_fact_programmes_label);
        setFact(R.id.factHandsOn,
                getString(R.string.about_fact_handson_value), R.string.about_fact_handson_label);
    }

    /** Sets the big value and the small label of one fact card. */
    private void setFact(@IdRes int cardId, String value, @StringRes int labelRes) {
        View card = findViewById(cardId);
        ((TextView) card.findViewById(R.id.textFactValue)).setText(value);
        ((TextView) card.findViewById(R.id.textFactLabel)).setText(labelRes);
    }
}
