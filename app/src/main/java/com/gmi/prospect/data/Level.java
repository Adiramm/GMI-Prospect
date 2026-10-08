package com.gmi.prospect.data;

import androidx.annotation.StringRes;

import com.gmi.prospect.R;

/** Programme level offered by GMI. Order matters: ranking ties put DIPLOMA first. */
public enum Level {
    DIPLOMA(R.string.level_diploma),
    GAPP(R.string.level_gapp),
    GUFP(R.string.level_gufp);

    @StringRes
    private final int labelRes;

    Level(@StringRes int labelRes) {
        this.labelRes = labelRes;
    }

    /** Translated display name, e.g. "Diploma". */
    @StringRes
    public int getLabelRes() {
        return labelRes;
    }
}
