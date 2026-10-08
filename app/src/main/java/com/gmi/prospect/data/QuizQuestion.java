package com.gmi.prospect.data;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

/**
 * One interest-quiz question with exactly 4 answers.
 * Answer index i adds +1 to {@link Interest#fromAnswerIndex(int)}.
 */
public class QuizQuestion {

    /** Every question has one answer per Interest. */
    public static final int ANSWER_COUNT = 4;

    @StringRes
    private final int questionRes;
    @StringRes
    private final int[] answerRes;

    public QuizQuestion(@StringRes int questionRes, @StringRes int electricalAnswer,
                        @StringRes int mechanicalAnswer, @StringRes int designAnswer,
                        @StringRes int itAnswer) {
        this.questionRes = questionRes;
        this.answerRes = new int[]{electricalAnswer, mechanicalAnswer, designAnswer, itAnswer};
    }

    @StringRes
    public int getQuestionRes() {
        return questionRes;
    }

    /** Text of answer {@code index} (0-3). */
    @StringRes
    public int getAnswerRes(int index) {
        return answerRes[index];
    }

    /** The Interest that answer {@code index} scores for. */
    @NonNull
    public Interest getInterestForAnswer(int index) {
        return Interest.fromAnswerIndex(index);
    }
}
