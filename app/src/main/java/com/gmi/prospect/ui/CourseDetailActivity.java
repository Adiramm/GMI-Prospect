package com.gmi.prospect.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.gmi.prospect.R;
import com.gmi.prospect.base.BaseActivity;
import com.gmi.prospect.data.Course;
import com.gmi.prospect.data.CourseRepository;
import com.gmi.prospect.data.Level;
import com.gmi.prospect.data.RuleType;
import com.gmi.prospect.data.Subject;
import com.gmi.prospect.data.SubjectRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Details of one course: name, level, faculty, duration, description and entry
 * requirements, plus an "Am I eligible?" button that opens GradeInputActivity.
 * <p>
 * The requirement bullets are GENERATED from the course's {@link RuleType}, so
 * the text on this screen can never disagree with the eligibility check.
 * <p>
 * Expects {@link #EXTRA_COURSE_ID} in the starting Intent.
 */
public class CourseDetailActivity extends BaseActivity {

    /** Intent extra: ID of the course to show (String). Also passed on to GradeInputActivity. */
    public static final String EXTRA_COURSE_ID = "com.gmi.prospect.extra.COURSE_ID";

    private Course course;

    // Reads the course from the Intent and fills the screen. Nothing needs to
    // be saved in onSaveInstanceState: after rotation the SAME Intent is
    // delivered again, so onCreate simply rebuilds the screen from it.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        course = CourseRepository.findById(getIntent().getStringExtra(EXTRA_COURSE_ID));
        if (course == null) {
            // Opened without a valid course - nothing to show, go back.
            finish();
            return;
        }

        setContentView(R.layout.activity_course_detail);
        setupToolbar(R.string.title_course_detail);
        showCourseInfo();
        showRequirements(buildRequirementLines(course.getRule()));

        findViewById(R.id.buttonCheckEligibility).setOnClickListener(v -> openGradeInput());
    }

    // ---------------------------------------------------------------------
    // Filling the screen
    // ---------------------------------------------------------------------

    /** Header, facts card, description, badge and medical note. */
    private void showCourseInfo() {
        ImageView imageIcon = findViewById(R.id.imageCourseIcon);
        imageIcon.setImageResource(course.getIcon());

        ((TextView) findViewById(R.id.textCourseName)).setText(course.getNameRes());
        ((TextView) findViewById(R.id.textLevel)).setText(course.getLevel().getLabelRes());
        ((TextView) findViewById(R.id.textFaculty)).setText(course.getFaculty().getLabelRes());
        ((TextView) findViewById(R.id.textDuration)).setText(course.getDurationRes());
        ((TextView) findViewById(R.id.textDescription)).setText(course.getDescRes());

        findViewById(R.id.chipLimitedSeats)
                .setVisibility(course.isLimitedSeats() ? View.VISIBLE : View.GONE);
        // Colour blindness / medical check applies to diplomas only (shown, not checked)
        findViewById(R.id.cardMedicalNote)
                .setVisibility(course.getLevel() == Level.DIPLOMA ? View.VISIBLE : View.GONE);
    }

    /**
     * Turns a RuleType into readable lines, in this order:
     * general passes (BM, Sejarah) → credits → "1 science/technical" →
     * "1 other subject" → extra passes (e.g. English for diplomas).
     */
    private List<String> buildRequirementLines(RuleType rule) {
        List<String> lines = new ArrayList<>();
        for (String subjectId : RuleType.GENERAL_PASS_SUBJECTS) {
            // GUFP already needs a credit in BM, so don't also list "pass in BM"
            if (!rule.getCreditSubjects().contains(subjectId)) {
                lines.add(getString(R.string.req_pass_in, subjectName(subjectId)));
            }
        }
        for (String subjectId : rule.getCreditSubjects()) {
            lines.add(getString(R.string.req_credit_in, subjectName(subjectId)));
        }
        if (rule.needsScienceTechnicalCredit()) {
            lines.add(getString(R.string.req_credit_science_technical));
        }
        if (rule.needsOtherCredit()) {
            lines.add(getString(R.string.req_credit_other));
        }
        for (String subjectId : rule.getPassSubjects()) {
            lines.add(getString(R.string.req_pass_in, subjectName(subjectId)));
        }
        return lines;
    }

    /** Translated subject name for an ID such as "math". */
    private String subjectName(String subjectId) {
        Subject subject = SubjectRepository.findById(subjectId);
        return subject != null ? getString(subject.getNameRes()) : subjectId;
    }

    /** Inflates one item_requirement.xml bullet per line into the requirements card. */
    private void showRequirements(@NonNull List<String> lines) {
        ViewGroup container = findViewById(R.id.containerRequirements);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (String line : lines) {
            View row = inflater.inflate(R.layout.item_requirement, container, false);
            ((TextView) row.findViewById(R.id.textRequirement)).setText(line);
            container.addView(row);
        }
    }

    // ---------------------------------------------------------------------
    // Navigation
    // ---------------------------------------------------------------------

    /** Starts the eligibility check, remembering which course the student came from. */
    private void openGradeInput() {
        Intent intent = new Intent(this, GradeInputActivity.class);
        intent.putExtra(EXTRA_COURSE_ID, course.getId());
        startActivity(intent);
    }
}
