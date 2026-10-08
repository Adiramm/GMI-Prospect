package com.gmi.prospect.data;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import java.io.Serializable;

/** One SPM subject. Serializable so it can be passed in an Intent if needed. */
public class Subject implements Serializable {

    private final String id;
    @StringRes
    private final int nameRes;
    private final boolean scienceTechnical;

    /**
     * @param id               stable key used in grade maps, e.g. "math"
     * @param nameRes          translated subject name
     * @param scienceTechnical true for Science/Technical/Vocational subjects (diploma rule)
     */
    public Subject(@NonNull String id, @StringRes int nameRes, boolean scienceTechnical) {
        this.id = id;
        this.nameRes = nameRes;
        this.scienceTechnical = scienceTechnical;
    }

    @NonNull
    public String getId() {
        return id;
    }

    @StringRes
    public int getNameRes() {
        return nameRes;
    }

    public boolean isScienceTechnical() {
        return scienceTechnical;
    }
}
