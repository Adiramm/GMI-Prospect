package com.gmi.prospect.data;

import com.gmi.prospect.R;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The 6 interest-quiz questions. For every question the answers are listed in
 * Interest order: ELECTRICAL, MECHANICAL, DESIGN, IT.
 * Scoring: +1 to the Interest of each chosen answer.
 */
public final class QuizRepository {

    private static final List<QuizQuestion> QUESTIONS = Collections.unmodifiableList(Arrays.asList(
            new QuizQuestion(R.string.quiz_q1,
                    R.string.quiz_q1_a0, R.string.quiz_q1_a1, R.string.quiz_q1_a2, R.string.quiz_q1_a3),
            new QuizQuestion(R.string.quiz_q2,
                    R.string.quiz_q2_a0, R.string.quiz_q2_a1, R.string.quiz_q2_a2, R.string.quiz_q2_a3),
            new QuizQuestion(R.string.quiz_q3,
                    R.string.quiz_q3_a0, R.string.quiz_q3_a1, R.string.quiz_q3_a2, R.string.quiz_q3_a3),
            new QuizQuestion(R.string.quiz_q4,
                    R.string.quiz_q4_a0, R.string.quiz_q4_a1, R.string.quiz_q4_a2, R.string.quiz_q4_a3),
            new QuizQuestion(R.string.quiz_q5,
                    R.string.quiz_q5_a0, R.string.quiz_q5_a1, R.string.quiz_q5_a2, R.string.quiz_q5_a3),
            new QuizQuestion(R.string.quiz_q6,
                    R.string.quiz_q6_a0, R.string.quiz_q6_a1, R.string.quiz_q6_a2, R.string.quiz_q6_a3)
    ));

    private QuizRepository() {
        // Static helper only - no instances.
    }

    /** All questions in display order. */
    public static List<QuizQuestion> getQuestions() {
        return QUESTIONS;
    }
}
