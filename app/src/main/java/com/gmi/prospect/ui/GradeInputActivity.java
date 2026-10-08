package com.gmi.prospect.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.gmi.prospect.R;
import com.gmi.prospect.base.BaseActivity;
import com.gmi.prospect.data.Grade;
import com.gmi.prospect.data.Subject;
import com.gmi.prospect.data.SubjectRepository;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SPM grade input - first step of the eligibility check.
 * <p>
 * Shows the 4 core subjects (BM, English, Mathematics, Sejarah), each with a
 * single-select row of grade chips. "+ Add subject" opens a bottom sheet of
 * optional subjects; added rows can be removed again. "Next" validates and
 * sends the grades to QuizActivity as a HashMap (subjectId → grade label).
 * <p>
 * LIFECYCLE: picked grades and added subjects survive rotation, dark-mode and
 * language switches via onSaveInstanceState → onCreate (rule #2).
 */
public class GradeInputActivity extends BaseActivity {

    /** Intent extra for QuizActivity: HashMap&lt;String, String&gt; subjectId → grade label. */
    public static final String EXTRA_GRADES = "com.gmi.prospect.extra.GRADES";

    // Bundle keys for onSaveInstanceState
    private static final String KEY_GRADES = "grades";
    private static final String KEY_ADDED_SUBJECTS = "added_subjects";

    /** The student's picks: subjectId → grade label ("A+", "B" ...). Only subjects on screen. */
    private final HashMap<String, String> selectedGrades = new HashMap<>();
    /** Optional subjects the student added, in the order they were added. */
    private final ArrayList<String> addedSubjectIds = new ArrayList<>();
    /** subjectId → its row card, in screen order. Keyed by ID, so a subject can appear only once. */
    private final Map<String, MaterialCardView> rows = new LinkedHashMap<>();

    private LinearLayout containerSubjects;
    private ScrollView scrollGrades;
    private View buttonNext;
    @Nullable
    private BottomSheetDialog addSubjectSheet;

    // Builds the 4 core rows, then - if Android re-created us after rotation,
    // dark mode or a language switch - rebuilds the added rows and re-picks
    // every grade from savedInstanceState. Without this the student would
    // lose everything they typed just by turning the phone.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grade_input);
        setupToolbar(R.string.title_eligibility);

        containerSubjects = findViewById(R.id.containerSubjects);
        scrollGrades = findViewById(R.id.scrollGrades);
        buttonNext = findViewById(R.id.buttonNext);

        for (Subject subject : SubjectRepository.getCoreSubjects()) {
            addSubjectRow(subject, false);
        }
        restoreState(savedInstanceState);

        findViewById(R.id.buttonAddSubject).setOnClickListener(v -> showAddSubjectSheet());
        buttonNext.setOnClickListener(v -> goToQuiz());
    }

    // Called before the Activity may be destroyed and re-created. The rows
    // and chips are created in code with generated IDs, so Android cannot
    // restore them by itself - we save the plain data instead: which optional
    // subjects were added, and which grade was picked for each subject.
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Bundle grades = new Bundle();
        for (Map.Entry<String, String> entry : selectedGrades.entrySet()) {
            grades.putString(entry.getKey(), entry.getValue());
        }
        outState.putBundle(KEY_GRADES, grades);
        outState.putStringArrayList(KEY_ADDED_SUBJECTS, addedSubjectIds);
    }

    // The Activity is going away (Back, or re-creation on rotation). A dialog
    // that is still showing would "leak" its window, so close it here.
    @Override
    protected void onDestroy() {
        if (addSubjectSheet != null) {
            addSubjectSheet.dismiss();
            addSubjectSheet = null;
        }
        super.onDestroy();
    }

    /** Rebuilds added rows first (so their chips exist), then re-picks every saved grade. */
    private void restoreState(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return; // fresh start - nothing to restore
        }
        ArrayList<String> added = savedInstanceState.getStringArrayList(KEY_ADDED_SUBJECTS);
        if (added != null) {
            for (String subjectId : added) {
                addOptionalSubject(subjectId);
            }
        }
        Bundle grades = savedInstanceState.getBundle(KEY_GRADES);
        if (grades != null) {
            for (String subjectId : grades.keySet()) {
                pickGrade(subjectId, grades.getString(subjectId));
            }
        }
        Log.d(TAG, "GradeInputActivity restored " + addedSubjectIds.size()
                + " added subjects and " + selectedGrades.size() + " grades");
    }

    // ---------------------------------------------------------------------
    // Subject rows
    // ---------------------------------------------------------------------

    /**
     * Inflates one subject card with its 10 grade chips and adds it below the
     * existing rows.
     *
     * @param removable true for optional subjects (shows the × button)
     */
    private void addSubjectRow(@NonNull Subject subject, boolean removable) {
        LayoutInflater inflater = LayoutInflater.from(this);
        MaterialCardView card = (MaterialCardView) inflater.inflate(
                R.layout.item_subject_grade, containerSubjects, false);
        // We save this row ourselves (see onSaveInstanceState). Stop Android's
        // automatic view saving for it: every row reuses the same view IDs.
        card.setSaveFromParentEnabled(false);

        String subjectName = getString(subject.getNameRes());
        ((TextView) card.findViewById(R.id.textSubjectName)).setText(subjectName);

        View buttonRemove = card.findViewById(R.id.buttonRemove);
        if (removable) {
            buttonRemove.setVisibility(View.VISIBLE);
            buttonRemove.setContentDescription(getString(R.string.cd_remove_subject, subjectName));
            buttonRemove.setOnClickListener(v -> removeSubjectRow(subject.getId()));
        }

        ChipGroup chipGroup = card.findViewById(R.id.chipGroupGrades);
        for (Grade grade : Grade.values()) {
            Chip chip = (Chip) inflater.inflate(R.layout.item_grade_chip, chipGroup, false);
            chip.setId(View.generateViewId()); // ChipGroup needs an ID to track the selection
            chip.setText(grade.getLabel());
            chip.setTag(grade.getLabel());
            chipGroup.addView(chip);
        }
        chipGroup.setOnCheckedStateChangeListener(
                (group, checkedIds) -> onGradeChanged(subject.getId(), group, checkedIds));

        containerSubjects.addView(card);
        rows.put(subject.getId(), card);
    }

    /** Stores the picked grade and clears the row's error highlight. */
    private void onGradeChanged(String subjectId, ChipGroup group, List<Integer> checkedIds) {
        if (checkedIds.isEmpty()) {
            selectedGrades.remove(subjectId);
            return;
        }
        Chip chip = group.findViewById(checkedIds.get(0));
        selectedGrades.put(subjectId, (String) chip.getTag());
        setRowError(rows.get(subjectId), false);
    }

    /** Checks the chip with this grade label in the subject's row (used when restoring). */
    private void pickGrade(String subjectId, @Nullable String gradeLabel) {
        MaterialCardView card = rows.get(subjectId);
        if (card == null || gradeLabel == null) {
            return;
        }
        ChipGroup chipGroup = card.findViewById(R.id.chipGroupGrades);
        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            View chip = chipGroup.getChildAt(i);
            if (gradeLabel.equals(chip.getTag())) {
                chipGroup.check(chip.getId()); // triggers onGradeChanged
                return;
            }
        }
    }

    /**
     * Adds an optional subject row. Duplicates are blocked here: the bottom
     * sheet already hides added subjects, and this check is the safety net.
     */
    private void addOptionalSubject(String subjectId) {
        Subject subject = SubjectRepository.findById(subjectId);
        if (subject == null) {
            return;
        }
        if (rows.containsKey(subjectId)) {
            showError(getString(R.string.grades_error_duplicate, getString(subject.getNameRes())));
            return;
        }
        addSubjectRow(subject, true);
        addedSubjectIds.add(subjectId);
    }

    /** Removes an added subject's row and forgets its grade. */
    private void removeSubjectRow(String subjectId) {
        MaterialCardView card = rows.remove(subjectId);
        if (card != null) {
            containerSubjects.removeView(card);
        }
        addedSubjectIds.remove(subjectId);
        selectedGrades.remove(subjectId);
    }

    // ---------------------------------------------------------------------
    // "+ Add subject" bottom sheet
    // ---------------------------------------------------------------------

    /** Lists optional subjects not yet on screen, grouped Science/Technical then Other. */
    @SuppressLint("InflateParams") // a dialog's content has no parent view yet
    private void showAddSubjectSheet() {
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        View content = LayoutInflater.from(this).inflate(R.layout.sheet_add_subject, null);
        LinearLayout options = content.findViewById(R.id.containerOptions);

        addOptionGroup(options, R.string.grades_sheet_science, true, sheet);
        addOptionGroup(options, R.string.grades_sheet_other, false, sheet);
        if (options.getChildCount() == 0) {
            content.findViewById(R.id.textAllAdded).setVisibility(View.VISIBLE);
        }

        sheet.setContentView(content);
        sheet.setOnDismissListener(d -> addSubjectSheet = null);
        addSubjectSheet = sheet;
        sheet.show();
    }

    /** Adds a section header + one tappable line per available subject of this group. */
    private void addOptionGroup(LinearLayout options, @StringRes int titleRes,
                                boolean scienceTechnical, BottomSheetDialog sheet) {
        LayoutInflater inflater = LayoutInflater.from(this);
        boolean headerAdded = false;
        for (Subject subject : SubjectRepository.getOptionalSubjects()) {
            if (subject.isScienceTechnical() != scienceTechnical || rows.containsKey(subject.getId())) {
                continue;
            }
            if (!headerAdded) {
                TextView header = (TextView) inflater.inflate(R.layout.item_section_header, options, false);
                header.setText(titleRes);
                options.addView(header);
                headerAdded = true;
            }
            TextView option = (TextView) inflater.inflate(R.layout.item_subject_option, options, false);
            option.setText(subject.getNameRes());
            option.setOnClickListener(v -> {
                addOptionalSubject(subject.getId());
                sheet.dismiss();
                // Show the new row (it is added at the bottom)
                scrollGrades.post(() -> scrollGrades.fullScroll(View.FOCUS_DOWN));
            });
            options.addView(option);
        }
    }

    // ---------------------------------------------------------------------
    // Validation & navigation
    // ---------------------------------------------------------------------

    /** Validates, then opens the quiz with the grades (and the course, if we came from one). */
    private void goToQuiz() {
        if (!validate()) {
            return;
        }
        Intent intent = new Intent(this, QuizActivity.class);
        intent.putExtra(EXTRA_GRADES, new HashMap<>(selectedGrades));
        String courseId = getIntent().getStringExtra(CourseDetailActivity.EXTRA_COURSE_ID);
        if (courseId != null) {
            intent.putExtra(CourseDetailActivity.EXTRA_COURSE_ID, courseId);
        }
        startActivity(intent);
    }

    /**
     * Every row must have a grade. Rows without one are highlighted, a
     * Snackbar says how many are missing, and the screen scrolls to the first.
     * (Duplicate subjects cannot get this far - see addOptionalSubject.)
     */
    private boolean validate() {
        MaterialCardView firstError = null;
        int missingCount = 0;
        for (Map.Entry<String, MaterialCardView> row : rows.entrySet()) {
            boolean missing = !selectedGrades.containsKey(row.getKey());
            setRowError(row.getValue(), missing);
            if (missing) {
                missingCount++;
                if (firstError == null) {
                    firstError = row.getValue();
                }
            }
        }
        if (firstError == null) {
            return true;
        }
        showError(getResources().getQuantityString(
                R.plurals.grades_error_missing, missingCount, missingCount));
        int top = containerSubjects.getTop() + firstError.getTop();
        scrollGrades.smoothScrollTo(0, Math.max(0, top - firstError.getHeight() / 2));
        return false;
    }

    /** Red outline + "Pick a grade" under the row, or back to normal. */
    private void setRowError(@Nullable MaterialCardView card, boolean error) {
        if (card == null) {
            return;
        }
        if (error) {
            card.setStrokeColor(MaterialColors.getColor(card, androidx.appcompat.R.attr.colorError));
            card.setStrokeWidth(getResources().getDimensionPixelSize(R.dimen.card_error_stroke_width));
        } else {
            card.setStrokeColor(MaterialColors.getColor(card,
                    com.google.android.material.R.attr.colorOutlineVariant));
            card.setStrokeWidth(getResources().getDimensionPixelSize(R.dimen.card_stroke_width));
        }
        card.findViewById(R.id.textRowError).setVisibility(error ? View.VISIBLE : View.GONE);
    }

    /** Snackbar shown just above the Next button. */
    private void showError(String message) {
        Snackbar.make(buttonNext, message, Snackbar.LENGTH_LONG)
                .setAnchorView(buttonNext)
                .show();
    }
}
