package com.gmi.prospect.logic;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.gmi.prospect.data.Subject;
import com.gmi.prospect.data.SubjectRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Outcome of checking one course: eligible or not, plus what is missing.
 * <p>
 * Missing items are stored as a string resource + subject ID instead of
 * finished text. That keeps EligibilityChecker free of Android code (so it can
 * be unit-tested on the PC), and the text is only built when shown, in the
 * language currently on screen (English or Bahasa Melayu).
 */
public class EligibilityResult {

    /** One unmet requirement, e.g. "Needs a credit (C or better) in Mathematics". */
    public static class Missing {
        @StringRes
        private final int messageRes;
        @Nullable
        private final String subjectId;

        /**
         * @param messageRes string resource; if it contains %1$s the subject name goes there
         * @param subjectId  subject the message is about, or null for general messages
         */
        public Missing(@StringRes int messageRes, @Nullable String subjectId) {
            this.messageRes = messageRes;
            this.subjectId = subjectId;
        }

        @StringRes
        public int getMessageRes() {
            return messageRes;
        }

        @Nullable
        public String getSubjectId() {
            return subjectId;
        }

        /** Readable text in the current app language. */
        @NonNull
        public String toText(@NonNull Context context) {
            if (subjectId == null) {
                return context.getString(messageRes);
            }
            Subject subject = SubjectRepository.findById(subjectId);
            String subjectName = subject != null ? context.getString(subject.getNameRes()) : subjectId;
            return context.getString(messageRes, subjectName);
        }
    }

    private final List<Missing> missing;

    public EligibilityResult(@NonNull List<Missing> missing) {
        this.missing = Collections.unmodifiableList(new ArrayList<>(missing));
    }

    /** Eligible = nothing is missing. */
    public boolean isEligible() {
        return missing.isEmpty();
    }

    /** Unmet requirements, in the same order as the course's entry requirements. */
    @NonNull
    public List<Missing> getMissing() {
        return missing;
    }

    /** All missing items as readable sentences (for the Result screen). */
    @NonNull
    public List<String> getMissingMessages(@NonNull Context context) {
        List<String> messages = new ArrayList<>();
        for (Missing item : missing) {
            messages.add(item.toText(context));
        }
        return messages;
    }
}
