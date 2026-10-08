package com.gmi.prospect.logic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.gmi.prospect.R;
import com.gmi.prospect.data.Course;
import com.gmi.prospect.data.CourseRepository;
import com.gmi.prospect.data.Level;
import com.gmi.prospect.data.RuleType;
import com.gmi.prospect.data.SubjectRepository;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Local unit tests for {@link EligibilityChecker} (run with: gradlew test).
 * Each test builds a student's SPM grades and checks the result per course.
 */
public class EligibilityCheckerTest {

    private static final String BM = SubjectRepository.BAHASA_MELAYU;
    private static final String ENGLISH = SubjectRepository.ENGLISH;
    private static final String MATH = SubjectRepository.MATHEMATICS;
    private static final String SEJARAH = SubjectRepository.SEJARAH;
    private static final String ADD_MATH = SubjectRepository.ADD_MATH;
    private static final String PHYSICS = SubjectRepository.PHYSICS;
    private static final String CHEMISTRY = SubjectRepository.CHEMISTRY;

    /** Builds a grade map from pairs: grades("math", "A", "bm", "B"). */
    private static Map<String, String> grades(String... pairs) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put(pairs[i], pairs[i + 1]);
        }
        return map;
    }

    /** A science-stream student with an A in every subject. */
    private static Map<String, String> allA() {
        return grades(BM, "A", ENGLISH, "A", MATH, "A", SEJARAH, "A",
                ADD_MATH, "A", PHYSICS, "A", CHEMISTRY, "A");
    }

    /** True if the result lists this exact missing item. */
    private static boolean hasMissing(EligibilityResult result, int messageRes, String subjectId) {
        for (EligibilityResult.Missing item : result.getMissing()) {
            boolean sameSubject = subjectId == null
                    ? item.getSubjectId() == null
                    : subjectId.equals(item.getSubjectId());
            if (item.getMessageRes() == messageRes && sameSubject) {
                return true;
            }
        }
        return false;
    }

    @Test
    public void allA_eligibleForEveryCourse() {
        Map<Course, EligibilityResult> results = EligibilityChecker.checkAll(allA());

        assertEquals(CourseRepository.getAllCourses().size(), results.size());
        for (Map.Entry<Course, EligibilityResult> entry : results.entrySet()) {
            assertTrue(entry.getKey().getId() + " should be eligible", entry.getValue().isEligible());
        }
    }

    @Test
    public void dInMaths_notEligibleForAnyDiploma() {
        Map<String, String> grades = allA();
        grades.put(MATH, "D");

        for (Course course : CourseRepository.getCoursesByLevel(Level.DIPLOMA)) {
            EligibilityResult result = EligibilityChecker.check(course, grades);
            assertFalse(course.getId() + " should not be eligible", result.isEligible());
            assertTrue(hasMissing(result, R.string.missing_credit_in, MATH));
        }
        // GAPP and GUFP also need a credit in Maths
        assertFalse(EligibilityChecker.checkRule(RuleType.GAPP, grades).isEligible());
        assertFalse(EligibilityChecker.checkRule(RuleType.GUFP, grades).isEligible());
    }

    @Test
    public void noAddMaths_diplomaYes_gappAndGufpNo() {
        Map<String, String> grades = allA();
        grades.remove(ADD_MATH);

        assertTrue(EligibilityChecker.checkRule(RuleType.DIPLOMA_GENERAL, grades).isEligible());

        EligibilityResult gapp = EligibilityChecker.checkRule(RuleType.GAPP, grades);
        assertFalse(gapp.isEligible());
        assertEquals(1, gapp.getMissing().size());
        assertTrue(hasMissing(gapp, R.string.missing_credit_in, ADD_MATH));

        EligibilityResult gufp = EligibilityChecker.checkRule(RuleType.GUFP, grades);
        assertFalse(gufp.isEligible());
        assertEquals(1, gufp.getMissing().size());
        assertTrue(hasMissing(gufp, R.string.missing_credit_in, ADD_MATH));
    }

    @Test
    public void failSejarah_notEligibleForAnything() {
        Map<String, String> grades = allA();
        grades.put(SEJARAH, "G");

        for (Map.Entry<Course, EligibilityResult> entry : EligibilityChecker.checkAll(grades).entrySet()) {
            EligibilityResult result = entry.getValue();
            assertFalse(entry.getKey().getId() + " should not be eligible", result.isEligible());
            assertTrue(hasMissing(result, R.string.missing_pass_in, SEJARAH));
        }
    }

    // ----- Extra edge cases for the diploma rule -----

    @Test
    public void diploma_scienceCreditCannotAlsoCountAsOtherCredit() {
        // Only one credit besides Maths (Physics) → science OK, "other" missing
        Map<String, String> grades = grades(BM, "D", ENGLISH, "E", MATH, "C", SEJARAH, "E", PHYSICS, "C");
        EligibilityResult result = EligibilityChecker.checkRule(RuleType.DIPLOMA_GENERAL, grades);
        assertFalse(result.isEligible());
        assertEquals(1, result.getMissing().size());
        assertTrue(hasMissing(result, R.string.missing_credit_other, null));

        // A credit in BM counts as the "other" subject
        grades.put(BM, "C");
        assertTrue(EligibilityChecker.checkRule(RuleType.DIPLOMA_GENERAL, grades).isEligible());
    }

    @Test
    public void diploma_needsAScienceTechnicalCredit() {
        // Core subjects only: credits in BM/English cover "other", but no science subject
        Map<String, String> grades = grades(BM, "A", ENGLISH, "A", MATH, "A", SEJARAH, "A");
        EligibilityResult result = EligibilityChecker.checkRule(RuleType.DIPLOMA_GENERAL, grades);
        assertEquals(1, result.getMissing().size());
        assertTrue(hasMissing(result, R.string.missing_credit_science_technical, null));
    }

    @Test
    public void diploma_needsAPassInEnglish() {
        Map<String, String> grades = allA();
        grades.put(ENGLISH, "G");
        EligibilityResult result = EligibilityChecker.checkRule(RuleType.DIPLOMA_GENERAL, grades);
        assertEquals(1, result.getMissing().size());
        assertTrue(hasMissing(result, R.string.missing_pass_in, ENGLISH));
    }

    @Test
    public void gufp_failBm_onlyOneMessageAboutBm() {
        // GUFP needs a credit in BM, so the general "pass in BM" message is not repeated
        Map<String, String> grades = allA();
        grades.put(BM, "G");
        EligibilityResult result = EligibilityChecker.checkRule(RuleType.GUFP, grades);
        assertEquals(1, result.getMissing().size());
        assertTrue(hasMissing(result, R.string.missing_credit_in, BM));
    }
}
