package com.gmi.prospect.data;

import androidx.annotation.StringRes;

import com.gmi.prospect.R;

/** Faculty a course belongs to. Used for the section headers in the Diploma tab. */
public enum Faculty {
    ELECTRICAL(R.string.faculty_electrical),
    MECHANICAL(R.string.faculty_mechanical),
    COMPUTER(R.string.faculty_computer),
    PRE_UNIVERSITY(R.string.faculty_pre_university);

    @StringRes
    private final int labelRes;

    Faculty(@StringRes int labelRes) {
        this.labelRes = labelRes;
    }

    /** Translated faculty name, e.g. "Electrical Engineering". */
    @StringRes
    public int getLabelRes() {
        return labelRes;
    }
}
