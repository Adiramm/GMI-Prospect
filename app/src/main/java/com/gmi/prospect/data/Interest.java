package com.gmi.prospect.data;

/**
 * Interest areas measured by the quiz. The order is important: answer index
 * 0, 1, 2, 3 of every quiz question maps to ELECTRICAL, MECHANICAL, DESIGN, IT.
 */
public enum Interest {
    ELECTRICAL,
    MECHANICAL,
    DESIGN,
    IT;

    /** Converts a quiz answer index (0-3) to its Interest. */
    public static Interest fromAnswerIndex(int index) {
        return values()[index];
    }
}
