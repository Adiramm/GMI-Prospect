package com.gmi.prospect.logic;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gmi.prospect.R;
import com.gmi.prospect.data.Course;
import com.gmi.prospect.data.CourseRepository;
import com.gmi.prospect.data.Grade;
import com.gmi.prospect.data.RuleType;
import com.gmi.prospect.data.Subject;
import com.gmi.prospect.data.SubjectRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Checks SPM grades against each course's entry rule.
 * <p>
 * The rules themselves live in {@link RuleType} (the same data the course
 * detail screen shows), so this class only does the checking. Grades come in
 * as a map of subjectId → grade label (e.g. "math" → "B+"), exactly as they
 * travel between Activities. A subject that is not in the map counts as
 * "not taken".
 * <p>
 * Pure Java (no Context), so it is covered by local JUnit tests.
 */
public final class EligibilityChecker {

    private EligibilityChecker() {
        // Static helper only - no instances.
    }

    /** Checks every course, in CourseRepository order. */
    @NonNull
    public static Map<Course, EligibilityResult> checkAll(@NonNull Map<String, String> grades) {
        Map<Course, EligibilityResult> results = new LinkedHashMap<>();
        for (Course course : CourseRepository.getAllCourses()) {
            results.put(course, check(course, grades));
        }
        return results;
    }

    /** Checks one course. */
    @NonNull
    public static EligibilityResult check(@NonNull Course course, @NonNull Map<String, String> grades) {
        return checkRule(course.getRule(), grades);
    }

    /**
     * Checks one rule. Missing items are listed in the same order as the
     * requirements on the course detail screen:
     * general passes → credits → science/technical → other subject → extra passes.
     */
    @NonNull
    public static EligibilityResult checkRule(@NonNull RuleType rule, @NonNull Map<String, String> grades) {
        List<EligibilityResult.Missing> missing = new ArrayList<>();

        // General (all levels): pass in BM and Sejarah. Skipped when the rule
        // already needs a CREDIT in that subject (GUFP + BM), so the student
        // never sees two messages about the same subject.
        for (String subjectId : RuleType.GENERAL_PASS_SUBJECTS) {
            if (!rule.getCreditSubjects().contains(subjectId) && !isPass(grades, subjectId)) {
                missing.add(new EligibilityResult.Missing(R.string.missing_pass_in, subjectId));
            }
        }

        for (String subjectId : rule.getCreditSubjects()) {
            if (!isCredit(grades, subjectId)) {
                missing.add(new EligibilityResult.Missing(R.string.missing_credit_in, subjectId));
            }
        }

        checkExtraCredits(rule, grades, missing);

        for (String subjectId : rule.getPassSubjects()) {
            if (!isPass(grades, subjectId)) {
                missing.add(new EligibilityResult.Missing(R.string.missing_pass_in, subjectId));
            }
        }
        return new EligibilityResult(missing);
    }

    /**
     * Diploma rule: credit in at least 1 Science/Technical subject AND credit
     * in at least 1 other subject. Subjects already counted by the rule
     * (Mathematics) do not count again.
     * <p>
     * The "other" credit must be a DIFFERENT subject from the science one, so
     * once a science credit is used we need 2 extra credits in total.
     * Example: Physics C + BM C → OK. Physics C only → "other" is missing.
     */
    private static void checkExtraCredits(RuleType rule, Map<String, String> grades,
                                          List<EligibilityResult.Missing> missing) {
        if (!rule.needsScienceTechnicalCredit() && !rule.needsOtherCredit()) {
            return;
        }
        int extraCredits = 0;
        int scienceCredits = 0;
        for (String subjectId : grades.keySet()) {
            if (rule.getCreditSubjects().contains(subjectId) || !isCredit(grades, subjectId)) {
                continue;
            }
            extraCredits++;
            Subject subject = SubjectRepository.findById(subjectId);
            if (subject != null && subject.isScienceTechnical()) {
                scienceCredits++;
            }
        }

        boolean scienceOk = scienceCredits >= 1;
        if (rule.needsScienceTechnicalCredit() && !scienceOk) {
            missing.add(new EligibilityResult.Missing(R.string.missing_credit_science_technical, null));
        }
        int neededForOther = (rule.needsScienceTechnicalCredit() && scienceOk) ? 2 : 1;
        if (rule.needsOtherCredit() && extraCredits < neededForOther) {
            missing.add(new EligibilityResult.Missing(R.string.missing_credit_other, null));
        }
    }

    // ---------------------------------------------------------------------
    // Grade helpers
    // ---------------------------------------------------------------------

    /** The student's grade for a subject, or null if not taken / unknown label. */
    @Nullable
    private static Grade gradeOf(Map<String, String> grades, String subjectId) {
        String label = grades.get(subjectId);
        return label == null ? null : Grade.fromLabel(label);
    }

    /** Credit = C or better. Not taken = no credit. */
    private static boolean isCredit(Map<String, String> grades, String subjectId) {
        Grade grade = gradeOf(grades, subjectId);
        return grade != null && grade.isCredit();
    }

    /** Pass = E or better. Not taken = no pass. */
    private static boolean isPass(Map<String, String> grades, String subjectId) {
        Grade grade = gradeOf(grades, subjectId);
        return grade != null && grade.isPass();
    }
}
