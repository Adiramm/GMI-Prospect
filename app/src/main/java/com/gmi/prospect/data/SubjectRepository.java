package com.gmi.prospect.data;

import androidx.annotation.Nullable;

import com.gmi.prospect.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Hard-coded list of SPM subjects.
 * <p>
 * Core subjects are always shown on the grade screen; optional subjects are
 * picked with "+ Add subject". Subject IDs are the keys of the grade map
 * (subjectId → grade label) passed between Activities.
 */
public final class SubjectRepository {

    // IDs used by EligibilityChecker - keep them as constants to avoid typos.
    public static final String BAHASA_MELAYU = "bm";
    public static final String ENGLISH = "english";
    public static final String MATHEMATICS = "math";
    public static final String SEJARAH = "sejarah";
    public static final String ADD_MATH = "add_math";
    public static final String PHYSICS = "physics";
    public static final String CHEMISTRY = "chemistry";

    /** Compulsory subjects - every student has a grade for these. */
    private static final List<Subject> CORE = Collections.unmodifiableList(Arrays.asList(
            new Subject(BAHASA_MELAYU, R.string.subject_bm, false),
            new Subject(ENGLISH, R.string.subject_english, false),
            new Subject(MATHEMATICS, R.string.subject_math, false),
            new Subject(SEJARAH, R.string.subject_sejarah, false)
    ));

    /** Subjects the student may add. true = Science/Technical/Vocational. */
    private static final List<Subject> OPTIONAL = Collections.unmodifiableList(Arrays.asList(
            // Science / Technical / Vocational
            new Subject(PHYSICS, R.string.subject_physics, true),
            new Subject(CHEMISTRY, R.string.subject_chemistry, true),
            new Subject("biology", R.string.subject_biology, true),
            new Subject("science", R.string.subject_science, true),
            new Subject(ADD_MATH, R.string.subject_add_math, true),
            new Subject("computer_science", R.string.subject_computer_science, true),
            new Subject("engineering_drawing", R.string.subject_engineering_drawing, true),
            new Subject("mech_eng_studies", R.string.subject_mech_eng_studies, true),
            new Subject("elec_eng_studies", R.string.subject_elec_eng_studies, true),
            new Subject("civil_eng_studies", R.string.subject_civil_eng_studies, true),
            new Subject("invention", R.string.subject_invention, true),
            new Subject("graphic_comm_tech", R.string.subject_graphic_comm_tech, true),
            new Subject("agriculture", R.string.subject_agriculture, true),
            // Other subjects
            new Subject("pendidikan_islam", R.string.subject_pendidikan_islam, false),
            new Subject("pendidikan_moral", R.string.subject_pendidikan_moral, false),
            new Subject("accounting", R.string.subject_accounting, false),
            new Subject("economics", R.string.subject_economics, false),
            new Subject("business", R.string.subject_business, false),
            new Subject("geography", R.string.subject_geography, false),
            new Subject("visual_arts", R.string.subject_visual_arts, false),
            new Subject("english_literature", R.string.subject_english_literature, false),
            new Subject("chinese", R.string.subject_chinese, false),
            new Subject("tamil", R.string.subject_tamil, false),
            new Subject("arabic", R.string.subject_arabic, false)
    ));

    private SubjectRepository() {
        // Static helper only - no instances.
    }

    /** Bahasa Melayu, English, Mathematics, Sejarah. */
    public static List<Subject> getCoreSubjects() {
        return CORE;
    }

    /** Subjects shown in the "+ Add subject" list. */
    public static List<Subject> getOptionalSubjects() {
        return OPTIONAL;
    }

    /** Core + optional, in display order. */
    public static List<Subject> getAllSubjects() {
        List<Subject> all = new ArrayList<>(CORE);
        all.addAll(OPTIONAL);
        return all;
    }

    /** Looks up a subject by ID, or returns null if it does not exist. */
    @Nullable
    public static Subject findById(String id) {
        for (Subject subject : getAllSubjects()) {
            if (subject.getId().equals(id)) {
                return subject;
            }
        }
        return null;
    }
}
