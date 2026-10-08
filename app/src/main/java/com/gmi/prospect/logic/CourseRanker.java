package com.gmi.prospect.logic;

import androidx.annotation.NonNull;

import com.gmi.prospect.data.Course;
import com.gmi.prospect.data.Interest;
import com.gmi.prospect.data.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * Orders eligible courses by the interest quiz.
 * <p>
 * Score of a course = average quiz score of the course's interests.
 * Sorted high → low; ties → Diploma before GAPP/GUFP, then by name.
 * The quiz never changes eligibility - only the order of the list.
 * <p>
 * Pure Java (no Context), so it is covered by local JUnit tests.
 */
public final class CourseRanker {

    private CourseRanker() {
        // Static helper only - no instances.
    }

    /**
     * Turns quiz answers into one score per Interest: +1 for the Interest of
     * each chosen answer (answer index 0-3 = ELECTRICAL, MECHANICAL, DESIGN, IT).
     *
     * @param answers chosen answer index per question (-1 = not answered, ignored)
     * @return scores indexed by {@link Interest#ordinal()}
     */
    @NonNull
    public static int[] scoreAnswers(@NonNull int[] answers) {
        int[] scores = new int[Interest.values().length];
        for (int answer : answers) {
            if (answer >= 0) {
                scores[Interest.fromAnswerIndex(answer).ordinal()]++;
            }
        }
        return scores;
    }

    /** Average quiz score of the course's interests (0 if it has none). */
    public static double matchScore(@NonNull Course course, @NonNull int[] scores) {
        List<Interest> interests = course.getInterests();
        if (interests.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (Interest interest : interests) {
            total += scores[interest.ordinal()];
        }
        return (double) total / interests.size();
    }

    /**
     * Returns a NEW list with the courses in ranked order (best match first).
     *
     * @param nameOf gives a course's display name for the last tie-break
     *               (the Activity passes the translated name; tests can pass the ID)
     */
    @NonNull
    public static List<Course> rank(@NonNull List<Course> eligible, @NonNull int[] scores,
                                    @NonNull Function<Course, String> nameOf) {
        List<Course> ranked = new ArrayList<>(eligible);
        Collections.sort(ranked, (a, b) -> {
            // 1. Higher score first
            int byScore = Double.compare(matchScore(b, scores), matchScore(a, scores));
            if (byScore != 0) {
                return byScore;
            }
            // 2. Diploma before GAPP / GUFP (false sorts before true)
            int byLevel = Boolean.compare(a.getLevel() != Level.DIPLOMA, b.getLevel() != Level.DIPLOMA);
            if (byLevel != 0) {
                return byLevel;
            }
            // 3. Alphabetical by name
            return nameOf.apply(a).compareToIgnoreCase(nameOf.apply(b));
        });
        return ranked;
    }
}
