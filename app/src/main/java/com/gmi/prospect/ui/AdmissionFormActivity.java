package com.gmi.prospect.ui;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.IntentCompat;

import com.gmi.prospect.R;
import com.gmi.prospect.base.BaseActivity;
import com.gmi.prospect.data.Course;
import com.gmi.prospect.data.CourseRepository;
import com.gmi.prospect.data.Subject;
import com.gmi.prospect.data.SubjectRepository;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import java.util.HashMap;
import java.util.Map;

/**
 * Admission form for the course chosen on the Result screen.
 * <p>
 * Shows a read-only summary (course + SPM grades from the Intent), 5 validated
 * fields, and sends the application with the user's email app (ACTION_SENDTO).
 * <p>
 * LIFECYCLE (rule #3): the typed fields are a DRAFT in SharedPreferences -
 * saved in onPause, restored in onResume ("Draft restored" Snackbar), and
 * cleared after a successful submit.
 */
public class AdmissionFormActivity extends BaseActivity {

    /** Where the application email goes. */
    private static final String APPLICATION_EMAIL = "ghaziadiram@gmail.com";

    /** SharedPreferences file for the draft (excluded from cloud backup - it holds an IC number). */
    private static final String DRAFT_PREFS = "admission_draft";
    /** Draft keys, in the same order as {@link #fields}. */
    private static final String[] DRAFT_KEYS = {"name", "ic", "phone", "email", "address"};

    /** Malaysian phone after removing spaces/dashes: 01x mobile (10-11 digits) or 0x landline, optional +60. */
    private static final String PHONE_PATTERN = "^(\\+?60|0)(1\\d{8,9}|[3-9]\\d{7,8})$";
    /** Letters (any language), spaces and . ' @ / - (e.g. "bin", "a/l"). */
    private static final String NAME_PATTERN = "^[\\p{L} .'@/-]{3,}$";
    private static final int MIN_ADDRESS_LENGTH = 10;

    private Course course;
    private Map<String, String> grades;

    private TextInputLayout layoutName, layoutIc, layoutPhone, layoutEmail, layoutAddress;
    private EditText editName, editIc, editPhone, editEmail, editAddress;
    /** The 5 draft fields, same order as DRAFT_KEYS. */
    private EditText[] fields;

    /** True right after Submit (draft already cleared), until the user types again. */
    private boolean skipDraftSave;
    /** True between Submit and coming back from the email app. */
    private boolean returningFromEmail;

    // Reads the course + grades from the Intent and builds the screen. Typed
    // text survives rotation on its own (every EditText has an id), so there is
    // nothing to restore here - the draft is handled in onResume / onPause.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        course = CourseRepository.findById(getIntent().getStringExtra(CourseDetailActivity.EXTRA_COURSE_ID));
        if (course == null) {
            finish(); // opened without a course - nothing to apply for
            return;
        }
        grades = readGrades();

        setContentView(R.layout.activity_admission_form);
        setupToolbar(R.string.title_admission);
        bindViews();
        showSummary();
        setupFieldWatchers();
        findViewById(R.id.buttonSubmit).setOnClickListener(v -> submit());
    }

    // Back in front: put the saved draft into the fields. This covers every
    // way of coming back - from the Home button, from another app, and also a
    // brand-new screen after the app was closed or killed (fields start empty,
    // the draft fills them).
    @Override
    protected void onResume() {
        super.onResume();
        restoreDraft();
        if (returningFromEmail) {
            returningFromEmail = false;
            showSnackbar(getString(R.string.form_sent_hint));
        }
    }

    // Leaving the screen in ANY way (Home, Back, another app, a phone call,
    // rotation) always passes through onPause first. Saving here - a few short
    // strings, fast enough not to slow the switch - means the draft is safe
    // even if Android later kills the app in the background.
    @Override
    protected void onPause() {
        super.onPause();
        if (!skipDraftSave) {
            saveDraft();
        }
    }

    // ---------------------------------------------------------------------
    // Setup
    // ---------------------------------------------------------------------

    @NonNull
    @SuppressWarnings("unchecked")
    private Map<String, String> readGrades() {
        HashMap<String, String> map = IntentCompat.getSerializableExtra(
                getIntent(), GradeInputActivity.EXTRA_GRADES, HashMap.class);
        return map != null ? map : new HashMap<>();
    }

    private void bindViews() {
        layoutName = findViewById(R.id.layoutName);
        layoutIc = findViewById(R.id.layoutIc);
        layoutPhone = findViewById(R.id.layoutPhone);
        layoutEmail = findViewById(R.id.layoutEmail);
        layoutAddress = findViewById(R.id.layoutAddress);
        editName = findViewById(R.id.editName);
        editIc = findViewById(R.id.editIc);
        editPhone = findViewById(R.id.editPhone);
        editEmail = findViewById(R.id.editEmail);
        editAddress = findViewById(R.id.editAddress);
        fields = new EditText[]{editName, editIc, editPhone, editEmail, editAddress};
    }

    /** Fills the read-only card: course, "Level · duration", and one line per grade. */
    private void showSummary() {
        ((ImageView) findViewById(R.id.imageCourseIcon)).setImageResource(course.getIcon());
        ((TextView) findViewById(R.id.textCourseName)).setText(course.getNameRes());
        ((TextView) findViewById(R.id.textCourseMeta)).setText(courseMeta());

        ViewGroup container = findViewById(R.id.containerGrades);
        LayoutInflater inflater = LayoutInflater.from(this);
        // Loop over all subjects (not the map) so the order is always core first
        for (Subject subject : SubjectRepository.getAllSubjects()) {
            String grade = grades.get(subject.getId());
            if (grade == null) {
                continue;
            }
            View row = inflater.inflate(R.layout.item_grade_summary, container, false);
            ((TextView) row.findViewById(R.id.textSubject)).setText(subject.getNameRes());
            ((TextView) row.findViewById(R.id.textGrade)).setText(grade);
            container.addView(row);
        }
    }

    /** IC auto-format, and: typing in a field clears its error and re-enables the draft. */
    private void setupFieldWatchers() {
        editIc.addTextChangedListener(new IcFormatter(editIc));
        TextInputLayout[] layouts = {layoutName, layoutIc, layoutPhone, layoutEmail, layoutAddress};
        for (int i = 0; i < fields.length; i++) {
            TextInputLayout layout = layouts[i];
            fields[i].addTextChangedListener(new AfterTextChanged() {
                @Override
                public void afterTextChanged(Editable s) {
                    layout.setError(null);
                    skipDraftSave = false;
                }
            });
        }
    }

    // ---------------------------------------------------------------------
    // Draft (SharedPreferences)
    // ---------------------------------------------------------------------

    private SharedPreferences draftPrefs() {
        return getSharedPreferences(DRAFT_PREFS, MODE_PRIVATE);
    }

    /** Writes all 5 fields. An all-empty form removes the draft instead. */
    private void saveDraft() {
        boolean allEmpty = true;
        SharedPreferences.Editor editor = draftPrefs().edit();
        for (int i = 0; i < fields.length; i++) {
            String value = text(fields[i]);
            editor.putString(DRAFT_KEYS[i], value);
            allEmpty &= value.isEmpty();
        }
        if (allEmpty) {
            clearDraft();
            return;
        }
        editor.apply(); // apply() writes in the background - no delay when leaving
        Log.d(TAG, "AdmissionFormActivity draft saved");
    }

    /**
     * Copies the draft into the fields. The Snackbar only appears if something
     * actually changed - e.g. not after Home → back, when the fields still
     * hold the same text.
     */
    private void restoreDraft() {
        SharedPreferences prefs = draftPrefs();
        if (!prefs.contains(DRAFT_KEYS[0])) {
            return; // no draft saved
        }
        boolean changed = false;
        for (int i = 0; i < fields.length; i++) {
            String saved = prefs.getString(DRAFT_KEYS[i], "");
            if (!saved.equals(text(fields[i]))) {
                fields[i].setText(saved);
                changed = true;
            }
        }
        Log.d(TAG, "AdmissionFormActivity draft " + (changed ? "restored" : "unchanged"));
        if (changed) {
            Snackbar.make(findViewById(R.id.buttonSubmit), R.string.draft_restored, Snackbar.LENGTH_LONG)
                    .setAnchorView(R.id.buttonSubmit)
                    .setAction(R.string.draft_clear, v -> clearForm())
                    .show();
        }
    }

    private void clearDraft() {
        draftPrefs().edit().clear().apply();
        Log.d(TAG, "AdmissionFormActivity draft cleared");
    }

    /** "Clear" on the Snackbar: empty fields + no draft. */
    private void clearForm() {
        for (EditText field : fields) {
            field.setText("");
        }
        clearDraft();
    }

    // ---------------------------------------------------------------------
    // Validation
    // ---------------------------------------------------------------------

    /** Checks every field, shows inline errors, and focuses the first wrong one. */
    private boolean validate() {
        String ic = icDigits();
        String phone = text(editPhone).replaceAll("[\\s-]", "");

        View firstError = null;
        firstError = check(layoutName, text(editName).matches(NAME_PATTERN), R.string.error_name, firstError);
        if (ic.length() != 12) {
            firstError = check(layoutIc, false, R.string.error_ic_digits, firstError);
        } else {
            firstError = check(layoutIc, isValidBirthDate(ic), R.string.error_ic_date, firstError);
        }
        firstError = check(layoutPhone, phone.matches(PHONE_PATTERN), R.string.error_phone, firstError);
        firstError = check(layoutEmail, Patterns.EMAIL_ADDRESS.matcher(text(editEmail)).matches(),
                R.string.error_email, firstError);
        firstError = check(layoutAddress, text(editAddress).length() >= MIN_ADDRESS_LENGTH,
                R.string.error_address, firstError);

        if (firstError != null) {
            firstError.requestFocus(); // also scrolls the field into view
            return false;
        }
        return true;
    }

    /**
     * Shows or clears one field's error.
     *
     * @return the first field with an error so far (unchanged if one was already found)
     */
    @Nullable
    private View check(TextInputLayout layout, boolean valid, @StringRes int errorRes,
                       @Nullable View firstError) {
        layout.setError(valid ? null : getString(errorRes));
        if (!valid && firstError == null) {
            return layout.getEditText();
        }
        return firstError;
    }

    /** IC starts with the birth date YYMMDD: month 01-12, day 01-31. */
    private static boolean isValidBirthDate(String icDigits) {
        int month = Integer.parseInt(icDigits.substring(2, 4));
        int day = Integer.parseInt(icDigits.substring(4, 6));
        return month >= 1 && month <= 12 && day >= 1 && day <= 31;
    }

    // ---------------------------------------------------------------------
    // Submit
    // ---------------------------------------------------------------------

    /** Validates, then opens the email app with everything filled in, and clears the draft. */
    private void submit() {
        if (!validate()) {
            return;
        }
        String name = text(editName);
        String subject = getString(R.string.email_subject, getString(course.getNameRes()), name);
        String body = buildEmailBody(name);

        // mailto: with subject/body in the address works with almost every
        // email app; the extras cover apps that read those instead.
        Uri uri = Uri.parse("mailto:" + APPLICATION_EMAIL
                + "?subject=" + Uri.encode(subject) + "&body=" + Uri.encode(body));
        Intent intent = new Intent(Intent.ACTION_SENDTO, uri);
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{APPLICATION_EMAIL});
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, body);

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            showNoEmailApp(subject, body); // keep the draft - nothing was sent
            return;
        }
        // Handed over to the email app: the draft is no longer needed. Skip the
        // save in the onPause that follows, or it would write the draft back.
        clearDraft();
        skipDraftSave = true;
        returningFromEmail = true;
    }

    private String buildEmailBody(String name) {
        StringBuilder gradeLines = new StringBuilder();
        for (Subject subject : SubjectRepository.getAllSubjects()) {
            String grade = grades.get(subject.getId());
            if (grade != null) {
                gradeLines.append(getString(R.string.email_grade_line,
                        getString(subject.getNameRes()), grade)).append('\n');
            }
        }
        return getString(R.string.email_body,
                getString(course.getNameRes()), courseMeta(),
                name, text(editIc), text(editPhone), text(editEmail), text(editAddress),
                gradeLines.toString().trim());
    }

    /** No email app installed: explain, and offer to copy the application text. */
    private void showNoEmailApp(String subject, String body) {
        Snackbar.make(findViewById(R.id.buttonSubmit),
                        getString(R.string.form_no_email_app, APPLICATION_EMAIL), Snackbar.LENGTH_INDEFINITE)
                .setAnchorView(R.id.buttonSubmit)
                .setAction(R.string.action_copy, v -> {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    clipboard.setPrimaryClip(ClipData.newPlainText(subject, subject + "\n\n" + body));
                    showSnackbar(getString(R.string.form_copied));
                })
                .show();
    }

    // ---------------------------------------------------------------------
    // Small helpers
    // ---------------------------------------------------------------------

    private String courseMeta() {
        return getString(R.string.result_course_meta,
                getString(course.getLevel().getLabelRes()), getString(course.getDurationRes()));
    }

    private static String text(EditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }

    private String icDigits() {
        return text(editIc).replaceAll("\\D", "");
    }

    private void showSnackbar(String message) {
        Snackbar.make(findViewById(R.id.buttonSubmit), message, Snackbar.LENGTH_LONG)
                .setAnchorView(R.id.buttonSubmit)
                .show();
    }

    /** TextWatcher where only afterTextChanged matters. */
    private abstract static class AfterTextChanged implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    }

    /**
     * Formats the IC as 000000-00-0000 while typing. Dashes are only added
     * once the next digit exists, so Backspace never gets stuck on a dash.
     * The cursor stays after the same digit it was after.
     */
    private static class IcFormatter extends AfterTextChanged {
        private final EditText field;
        private boolean selfChange; // ignore the change we make ourselves

        IcFormatter(EditText field) {
            this.field = field;
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (selfChange) {
                return;
            }
            String current = s.toString();
            int cursor = field.getSelectionEnd();
            int digitsBeforeCursor = current.substring(0, Math.max(0, cursor)).replaceAll("\\D", "").length();

            String digits = current.replaceAll("\\D", "");
            if (digits.length() > 12) {
                digits = digits.substring(0, 12);
            }
            StringBuilder formatted = new StringBuilder();
            int newCursor = 0;
            for (int i = 0; i < digits.length(); i++) {
                if (i == 6 || i == 8) {
                    formatted.append('-');
                }
                formatted.append(digits.charAt(i));
                if (i < digitsBeforeCursor) {
                    newCursor = formatted.length();
                }
            }
            if (formatted.toString().equals(current)) {
                return;
            }
            selfChange = true;
            s.replace(0, s.length(), formatted);
            field.setSelection(Math.min(newCursor, s.length()));
            selfChange = false;
        }
    }
}
