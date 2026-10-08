package com.gmi.prospect.data;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * One GMI programme. All text is stored as string resource IDs so the course
 * shows in English or Bahasa Melayu automatically. (The duration is a string
 * resource too, not a plain String, so it can be translated.)
 */
public class Course implements Serializable {

    private final String id;
    @StringRes
    private final int nameRes;
    @StringRes
    private final int descRes;
    @StringRes
    private final int durationRes;
    private final Level level;
    private final Faculty faculty;
    private final RuleType rule;
    private final List<Interest> interests;
    private final boolean limitedSeats;
    @DrawableRes
    private final int icon;

    public Course(@NonNull String id, @StringRes int nameRes, @StringRes int descRes,
                  @StringRes int durationRes, @NonNull Level level, @NonNull Faculty faculty,
                  @NonNull RuleType rule, boolean limitedSeats, @DrawableRes int icon,
                  @NonNull Interest... interests) {
        this.id = id;
        this.nameRes = nameRes;
        this.descRes = descRes;
        this.durationRes = durationRes;
        this.level = level;
        this.faculty = faculty;
        this.rule = rule;
        this.limitedSeats = limitedSeats;
        this.icon = icon;
        // ArrayList is Serializable; wrap it so nobody can change the list later.
        this.interests = Collections.unmodifiableList(new ArrayList<>(Arrays.asList(interests)));
    }

    @NonNull
    public String getId() {
        return id;
    }

    @StringRes
    public int getNameRes() {
        return nameRes;
    }

    @StringRes
    public int getDescRes() {
        return descRes;
    }

    /** Translated duration, e.g. "3 years". */
    @StringRes
    public int getDurationRes() {
        return durationRes;
    }

    @NonNull
    public Level getLevel() {
        return level;
    }

    @NonNull
    public Faculty getFaculty() {
        return faculty;
    }

    @NonNull
    public RuleType getRule() {
        return rule;
    }

    @NonNull
    public List<Interest> getInterests() {
        return interests;
    }

    public boolean isLimitedSeats() {
        return limitedSeats;
    }

    @DrawableRes
    public int getIcon() {
        return icon;
    }
}
