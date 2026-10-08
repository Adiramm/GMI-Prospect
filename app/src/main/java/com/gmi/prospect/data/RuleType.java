package com.gmi.prospect.data;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Which entry-requirement rule applies to a course, and WHAT that rule needs.
 * <p>
 * This enum is the single source of truth for entry requirements: the course
 * detail screen turns it into a bullet list, and EligibilityChecker (later
 * phase) checks grades against the same data. Change a rule here and both
 * update together.
 */
public enum RuleType {

    /** All diplomas: credit in Maths + 1 science/technical + 1 other subject, pass in English. */
    DIPLOMA_GENERAL(
            list(SubjectRepository.MATHEMATICS),
            list(SubjectRepository.ENGLISH),
            true, true),

    /** GAPP: credit in English, Maths, Add Maths, Physics, Chemistry. */
    GAPP(
            list(SubjectRepository.ENGLISH, SubjectRepository.MATHEMATICS,
                    SubjectRepository.ADD_MATH, SubjectRepository.PHYSICS,
                    SubjectRepository.CHEMISTRY),
            list(),
            false, false),

    /** GUFP: minimum C in BM, Maths, English, Add Maths, Physics, Chemistry. */
    GUFP(
            list(SubjectRepository.BAHASA_MELAYU, SubjectRepository.MATHEMATICS,
                    SubjectRepository.ENGLISH, SubjectRepository.ADD_MATH,
                    SubjectRepository.PHYSICS, SubjectRepository.CHEMISTRY),
            list(),
            false, false);

    /**
     * Every level needs a pass in these (needed for an SPM certificate).
     * Kept separate from each rule's own lists so it is written only once.
     */
    public static final List<String> GENERAL_PASS_SUBJECTS =
            list(SubjectRepository.BAHASA_MELAYU, SubjectRepository.SEJARAH);

    private final List<String> creditSubjects;
    private final List<String> passSubjects;
    private final boolean needsScienceTechnicalCredit;
    private final boolean needsOtherCredit;

    /**
     * @param creditSubjects              subject IDs that need a credit (C or better)
     * @param passSubjects                subject IDs that need a pass (E or better)
     * @param needsScienceTechnicalCredit true = credit in at least 1 Science/Technical subject
     * @param needsOtherCredit            true = credit in at least 1 other subject
     *                                    (not one already counted above)
     */
    RuleType(List<String> creditSubjects, List<String> passSubjects,
             boolean needsScienceTechnicalCredit, boolean needsOtherCredit) {
        this.creditSubjects = creditSubjects;
        this.passSubjects = passSubjects;
        this.needsScienceTechnicalCredit = needsScienceTechnicalCredit;
        this.needsOtherCredit = needsOtherCredit;
    }

    /** Small helper: a read-only list of subject IDs. */
    private static List<String> list(String... subjectIds) {
        return Collections.unmodifiableList(Arrays.asList(subjectIds));
    }

    /** Subject IDs that need a credit (C or better). */
    @NonNull
    public List<String> getCreditSubjects() {
        return creditSubjects;
    }

    /** Subject IDs (besides the general ones) that need a pass (E or better). */
    @NonNull
    public List<String> getPassSubjects() {
        return passSubjects;
    }

    public boolean needsScienceTechnicalCredit() {
        return needsScienceTechnicalCredit;
    }

    public boolean needsOtherCredit() {
        return needsOtherCredit;
    }
}
