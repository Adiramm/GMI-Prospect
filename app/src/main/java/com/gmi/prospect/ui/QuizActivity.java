package com.gmi.prospect.ui;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.gmi.prospect.R;
import com.gmi.prospect.base.BaseActivity;
import com.gmi.prospect.data.QuizQuestion;
import com.gmi.prospect.data.QuizRepository;
import com.gmi.prospect.logic.CourseRanker;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.Arrays;
import java.util.List;

/**
 * Interest quiz: one question per screen with 4 answer cards.
 * <p>
 * "Next" is disabled until an answer is picked. After the last question the
 * answers become interest scores and go to ResultActivity, together with the
 * grades (and course ID) this screen received.
 * <p>
 * LIFECYCLE: the current question and all answers survive rotation, dark-mode
 * and language switches via onSaveInstanceState → onCreate (rule #2).
 */
public class QuizActivity extends BaseActivity {

    /** Intent extra for ResultActivity: int[] score per Interest (index = Interest.ordinal()). */
    public static final String EXTRA_INTEREST_SCORES = "com.gmi.prospect.extra.INTEREST_SCORES";

    private static final String KEY_INDEX = "question_index";
    private static final String KEY_ANSWERS = "answers";
    private static final int NO_ANSWER = -1;

    /** Slide animation: how far (dp) the question moves, and how long each half takes. */
    private static final float SLIDE_DP = 48f;
    private static final long SLIDE_OUT_MS = 140;
    private static final long SLIDE_IN_MS = 220;

    private List<QuizQuestion> questions;
    private int currentIndex;
    /** Chosen answer index (0-3) per question, or NO_ANSWER. */
    private int[] answers;
    private boolean animating;

    private final MaterialCardView[] answerCards = new MaterialCardView[QuizQuestion.ANSWER_COUNT];
    private View quizContent;
    private TextView textProgress;
    private TextView textQuestion;
    private LinearProgressIndicator progressQuiz;
    private MaterialButton buttonBack;
    private MaterialButton buttonNext;

    // Sets up the screen. On a fresh start we begin at question 1 with no
    // answers. If Android re-created us (rotation, dark mode, language), we
    // read back the question the student was on and every answer so far -
    // otherwise turning the phone would restart the quiz.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);
        setupToolbar(R.string.title_quiz);

        questions = QuizRepository.getQuestions();
        answers = new int[questions.size()];
        Arrays.fill(answers, NO_ANSWER);
        currentIndex = 0;
        if (savedInstanceState != null) {
            currentIndex = savedInstanceState.getInt(KEY_INDEX, 0);
            int[] saved = savedInstanceState.getIntArray(KEY_ANSWERS);
            if (saved != null && saved.length == answers.length) {
                answers = saved;
            }
            Log.d(TAG, "QuizActivity restored question " + (currentIndex + 1)
                    + ", answers " + Arrays.toString(answers));
        }

        bindViews();
        progressQuiz.setMax(questions.size());
        showQuestion(false);
    }

    // Called before the Activity may be destroyed and re-created: store which
    // question is showing and every answer picked so far.
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_INDEX, currentIndex);
        outState.putIntArray(KEY_ANSWERS, answers);
    }

    private void bindViews() {
        quizContent = findViewById(R.id.quizContent);
        textProgress = findViewById(R.id.textProgress);
        textQuestion = findViewById(R.id.textQuestion);
        progressQuiz = findViewById(R.id.progressQuiz);
        buttonBack = findViewById(R.id.buttonBack);
        buttonNext = findViewById(R.id.buttonNext);

        int[] cardIds = {R.id.answer0, R.id.answer1, R.id.answer2, R.id.answer3};
        for (int i = 0; i < cardIds.length; i++) {
            final int answerIndex = i;
            answerCards[i] = findViewById(cardIds[i]);
            ((TextView) answerCards[i].findViewById(R.id.textLetter))
                    .setText(String.valueOf((char) ('A' + i)));
            answerCards[i].setOnClickListener(v -> selectAnswer(answerIndex));
        }
        buttonBack.setOnClickListener(v -> moveTo(currentIndex - 1));
        buttonNext.setOnClickListener(v -> onNextClicked());
    }

    // ---------------------------------------------------------------------
    // Showing a question
    // ---------------------------------------------------------------------

    /** Fills in the current question, its answers, progress and buttons. */
    private void showQuestion(boolean animateProgress) {
        QuizQuestion question = questions.get(currentIndex);
        textProgress.setText(getString(R.string.quiz_progress, currentIndex + 1, questions.size()));
        progressQuiz.setProgressCompat(currentIndex + 1, animateProgress);
        textQuestion.setText(question.getQuestionRes());
        for (int i = 0; i < answerCards.length; i++) {
            ((TextView) answerCards[i].findViewById(R.id.textAnswer)).setText(question.getAnswerRes(i));
        }
        showSelection();
    }

    /** Highlights the chosen card and enables Back / Next. */
    private void showSelection() {
        int chosen = answers[currentIndex];
        for (int i = 0; i < answerCards.length; i++) {
            styleAnswerCard(answerCards[i], i == chosen);
        }
        boolean last = currentIndex == questions.size() - 1;
        buttonBack.setEnabled(currentIndex > 0);
        buttonNext.setEnabled(chosen != NO_ANSWER);
        buttonNext.setText(last ? R.string.quiz_see_results : R.string.action_next);
    }

    /** Selected = blue outline, light-blue fill and filled letter badge. */
    private void styleAnswerCard(MaterialCardView card, boolean selected) {
        TextView letter = card.findViewById(R.id.textLetter);
        card.setChecked(selected); // also tells screen readers "selected"
        if (selected) {
            card.setStrokeColor(MaterialColors.getColor(card, androidx.appcompat.R.attr.colorPrimary));
            card.setStrokeWidth(getResources().getDimensionPixelSize(R.dimen.card_selected_stroke_width));
            card.setCardBackgroundColor(MaterialColors.getColor(card,
                    com.google.android.material.R.attr.colorPrimaryContainer));
            letter.setBackgroundTintList(ColorStateList.valueOf(
                    MaterialColors.getColor(card, androidx.appcompat.R.attr.colorPrimary)));
            letter.setTextColor(MaterialColors.getColor(card,
                    com.google.android.material.R.attr.colorOnPrimary));
        } else {
            card.setStrokeColor(MaterialColors.getColor(card,
                    com.google.android.material.R.attr.colorOutlineVariant));
            card.setStrokeWidth(getResources().getDimensionPixelSize(R.dimen.card_stroke_width));
            card.setCardBackgroundColor(MaterialColors.getColor(card,
                    com.google.android.material.R.attr.colorSurface));
            letter.setBackgroundTintList(ColorStateList.valueOf(MaterialColors.getColor(card,
                    com.google.android.material.R.attr.colorSurfaceVariant)));
            letter.setTextColor(MaterialColors.getColor(card,
                    com.google.android.material.R.attr.colorOnSurface));
        }
    }

    private void selectAnswer(int answerIndex) {
        if (animating) {
            return;
        }
        answers[currentIndex] = answerIndex;
        showSelection();
    }

    // ---------------------------------------------------------------------
    // Navigation
    // ---------------------------------------------------------------------

    private void onNextClicked() {
        if (currentIndex == questions.size() - 1) {
            openResults();
        } else {
            moveTo(currentIndex + 1);
        }
    }

    /**
     * Slides the current question out, swaps in the new one, and slides it in
     * from the other side. Next = moves left, Back = moves right.
     */
    private void moveTo(int newIndex) {
        if (animating || newIndex < 0 || newIndex >= questions.size()) {
            return;
        }
        boolean forward = newIndex > currentIndex;
        float distance = SLIDE_DP * getResources().getDisplayMetrics().density;
        float outX = forward ? -distance : distance;

        animating = true;
        currentIndex = newIndex;
        quizContent.animate()
                .translationX(outX)
                .alpha(0f)
                .setDuration(SLIDE_OUT_MS)
                .withEndAction(() -> {
                    showQuestion(true);
                    quizContent.setTranslationX(-outX); // start on the opposite side
                    quizContent.animate()
                            .translationX(0f)
                            .alpha(1f)
                            .setDuration(SLIDE_IN_MS)
                            .withEndAction(() -> animating = false)
                            .start();
                })
                .start();
    }

    /**
     * Turns the answers into interest scores and opens the results. All extras
     * we received (grades + optional course ID) are passed on unchanged.
     */
    private void openResults() {
        int[] scores = CourseRanker.scoreAnswers(answers);
        Log.d(TAG, "QuizActivity scores (ELEC, MECH, DESIGN, IT): " + Arrays.toString(scores));

        Intent intent = new Intent(this, ResultActivity.class);
        intent.putExtras(getIntent()); // grades + course ID from GradeInputActivity
        intent.putExtra(EXTRA_INTEREST_SCORES, scores);
        startActivity(intent);
    }
}
